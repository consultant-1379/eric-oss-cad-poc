# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from configs import mongo_db_config as config
from mongoengine import connect


class MongoDBClient:
    __instance = None

    @staticmethod
    def get_instance():
        if MongoDBClient.__instance is None:
            MongoDBClient()
        return MongoDBClient.__instance

    def __init__(self):
        connect(
            db=config.arc_db_name,
            alias=config.arc_db_name,
            host=config.host,
            port=config.port,
            username=config.username,
            password=config.password,
            authentication_source=config.auth_source,
            authentication_mechanism=config.auth_mechanism,
            tz_aware=config.db_time_zone_aware,
        )
        MongoDBClient.__instance = self
