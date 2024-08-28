# coding: utf-8
#
# Copyright Ericsson (c) 2022
from abc import ABC, abstractmethod
from typing import Final

from pm_data_handler.utils.constant import MongoDBConstant


class MongoDBClientParams(ABC):
    """
    MongoDB Client Parameters Abstract configuration class  ([*] indicates that corresponding field is required) :

    Final Static Attributes :

        HOST (Final str) [*]: MongoDB Server Address

        PORT (Final int) [*]: MongoDB Server Port number

        USERNAME (Final str): Auth username

        PASSWORD (Final str): Auth password

        AUTH_SOURCE_DB_NAME (Final str): Name of the authentication database that is associated provided username

        AUTH_MECHANISM (Final str): Authentication mechanism that uses username and password to authenticate connection

        CAD_DB_NAME (Final str): Name of the database holding the pm data collection

        PM_COLLECTION_NAME (Final str): Name of the collection holding PM Records documents

        DB_TIME_ZONE_AWARE (Final bool) [*]: If True, objects will be timezone-aware

        TIMESERIES_TIME_FIELD (Final str) [*]: The name of the field which contains the date in each time series doc
                                            Documents in a time series collection must have a valid BSON date as the
                                            value for the timeField

        TIMESERIES_META_FIELD (Final str): The name of the field which contains metadata in each time series document
                                            The metadata in the specified field should be data that is used to label
                                            a unique series of documents. The metadata should rarely, if ever, change

        TIMESERIES_GRANULARITY (Final str):  Possible values are:
                                            "seconds"
                                            "minutes"
                                            "hours"
                                            Manually set the granularity parameter to improve performance by
                                            optimizing how data in the time series collection is stored internally
                                            To select a value for granularity, choose the closest match to the time
                                            span between consecutive incoming measurements

        PM_RECORD_GNB_ID_INDEX_ORDER (Final int): Index order for the gnb_id meta field

        PM_RECORD_TIMESTAMP_INDEX_ORDER (Final int): Index order for the timestamp field

        PM_RECORD_EXPIRY_PERIOD_IN_SECONDS (Final int): Enable the automatic deletion of documents in a time series
                                                        collection by specifying the number of seconds after which
                                                        documents expire
                                                        MongoDB deletes expired documents automatically

    """

    HOST: Final = "mongodb"
    PORT: Final = 27017
    USERNAME: Final = "mongodb"
    PASSWORD: Final = "password123"
    AUTH_SOURCE_DB_NAME: Final = "arc_db"
    CAD_DB_NAME: Final = "arc_db"
    AUTH_MECHANISM: Final = MongoDBConstant.SCRAM_SHA_256
    TIMESERIES_TIME_FIELD: Final = "timestamp"
    TIMESERIES_META_FIELD: Final = "metadata"
    TIMESERIES_GRANULARITY: Final = "minutes"
    PM_COLLECTION_NAME: Final = "pm_data"
    PM_RECORD_GNB_ID_INDEX_ORDER: Final = MongoDBConstant.ASCENDING
    PM_RECORD_TIMESTAMP_INDEX_ORDER: Final = MongoDBConstant.DESCENDING
    PM_RECORD_EXPIRY_PERIOD_IN_SECONDS: Final = 691200  # 8 days
    DB_TIME_ZONE_AWARE = True

    """
        Schema Registry Parameters - abstract_method() -> abstract method

        Desc:
            Prevent creating instances of this configuration class
    """

    @abstractmethod
    def abstract_method(self):
        pass
