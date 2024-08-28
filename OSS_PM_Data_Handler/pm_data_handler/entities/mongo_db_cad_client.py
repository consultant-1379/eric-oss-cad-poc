# coding: utf-8
#
# Copyright Ericsson (c) 2022
from bson.codec_options import CodecOptions
from pymongo import MongoClient
from pymongo.collection import Collection
from pymongo.database import Database
from pymongo.errors import ConnectionFailure, InvalidName, PyMongoError

from pm_data_handler.configs.mongo_db_config import MongoDBClientParams
from pm_data_handler.entities.cad_pm_data import PMData
from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger
from pm_data_handler.utils.meta import SingletonMeta


class MongoDBCadClient(MongoClient, metaclass=SingletonMeta):
    logger = PMDataHandlerLogger().get_logger("prod")
    db: Database
    pm_collection: Collection
    """
        MongoDB CAD Client.

        Desc:
            A customized client-side representation of a MongoDB cluster for PM data storage

            Instance can represent either a standalone MongoDB server, a replica
            set, or a sharded cluster. Instance of this class is responsible for
            maintaining up-to-date state of the cluster, and possibly cache
            resources related to this, including background threads for monitoring,
            and connection pools.

            The client will check if the PM Data timeseries collection is created,
            if not it will create it with all needed configuration and indexes information

        Args:
            host (str): MongoDB Server Address
            port (int): MongoDB Server Port number
            username (str): Auth username
            password (str): Auth password
            auth_source (str): Name of the authentication database that is associated provided username
            auth_mechanism (str): Authentication mechanism that uses username and password to authenticate connection
            arc_db_name (str): Name of the database holding the pm data collection
            pm_collection_name (str): Name of the collection holding PM Records documents
            tz_aware (bool): If True, objects will be timezone-aware

        Notes:
            - PM Records timestamp field will be store in UTC format
            - If the authentication database differs from the database to which we connect to retrieve
              pm counters, specify the authentication database with the authSource parameter in the URL
            - Some operations on time series collections can only take advantage of an index if that
              index is specified in a hint.

    """

    def __init__(
        self,
        host=MongoDBClientParams.HOST,
        port=MongoDBClientParams.PORT,
        username=MongoDBClientParams.USERNAME,
        password=MongoDBClientParams.PASSWORD,
        auth_source=MongoDBClientParams.AUTH_SOURCE_DB_NAME,
        auth_mechanism=MongoDBClientParams.AUTH_MECHANISM,
        cad_db_name=MongoDBClientParams.CAD_DB_NAME,
        pm_collection_name=MongoDBClientParams.PM_COLLECTION_NAME,
        tz_aware=MongoDBClientParams.DB_TIME_ZONE_AWARE,
    ):
        # TODO : Configure Authentication for mongoDB ( in both sides: client and server )
        super().__init__(
            host=host,
            port=port,
            username=username,
            password=password,
            authSource=auth_source,
            authMechanism=auth_mechanism,
            tz_aware=tz_aware,
        )
        self._check_mongo_server()
        self.db = self.retrieve_db(cad_db_name)

        self.pm_collection = self.get_pm_collection(pm_collection_name, tz_aware)
        try:
            # Create a secondary index on the metadata.gnb_id field
            self.pm_collection.create_index(
                [("metadata.gnb_id", MongoDBClientParams.PM_RECORD_GNB_ID_INDEX_ORDER)]
            )
            # Create a secondary index on the timestamp field
            self.pm_collection.create_index(
                [("timestamp", MongoDBClientParams.PM_RECORD_TIMESTAMP_INDEX_ORDER)]
            )
            # Create a compound secondary index on metadata.gnb_id and timestamp :
            self.pm_collection.create_index(
                [
                    (
                        "metadata.gnb_id",
                        MongoDBClientParams.PM_RECORD_GNB_ID_INDEX_ORDER,
                    ),
                    ("timestamp", MongoDBClientParams.PM_RECORD_TIMESTAMP_INDEX_ORDER),
                ]
            )
        except (TypeError, ValueError) as exc:
            self.logger.warning(
                "Error occurred while creating indexes for pm collection",
                exc_info=True,
            )
            raise exc

    def _check_mongo_server(self):
        try:
            self.admin.command("ping")
        except ConnectionFailure as exc:
            self.logger.error("Failed to connect to Mongo db server.")
            raise exc

    def retrieve_db(self, cad_db_name):
        return self[cad_db_name]

    def get_pm_collection(self, pm_collection_name, tz_aware):
        if pm_collection_name not in self.db.list_collection_names():
            self.db.create_collection(
                name=pm_collection_name,
                timeseries={
                    "timeField": MongoDBClientParams.TIMESERIES_TIME_FIELD,
                    "metaField": MongoDBClientParams.TIMESERIES_META_FIELD,
                    "granularity": MongoDBClientParams.TIMESERIES_GRANULARITY,
                },
                expireAfterSeconds=MongoDBClientParams.PM_RECORD_EXPIRY_PERIOD_IN_SECONDS,
            )
        try:
            return self.db.get_collection(
                name=pm_collection_name, codec_options=CodecOptions(tz_aware=tz_aware)
            )
        except (TypeError, InvalidName) as exc:
            self.logger.warning(
                "Failed to get pm collection from db client.", exc_info=True
            )
            raise exc

    """
        MongoDB CAD Client - insert() -> method

        Desc:
            Persist an CAD PM data record under the timeseries collection of MongoDB

        Args:
            pm_record (PMData): [*] The PM Record that will be persisted as a document in MongoDB

        Notes:
            - If the corresponding dictionary does not have an _id field one will be added automatically

    """

    def insert(self, cad_needed_pm_data_record: PMData):
        try:
            self.pm_collection.insert_one(cad_needed_pm_data_record.to_dict())
        except PyMongoError as exc:
            self.logger.warning(
                "Error occurred while persisting pm handler record to the database",
                exc_info=True,
            )
            raise exc


# Uncomment the following lines if you want to test the data persistence in the database.
# pm_record = PMData(1, 1, 1, 1, 1, 1, 1, 1)
# pm_persister = MongoDBArcClient()
# pm_persister.insert(pm_record)
# print(list(pm_persister.pm_collection.find({})))
