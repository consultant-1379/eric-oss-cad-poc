# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from database.mongo_db_client import MongoDBClient
from entities.api_response import ResponseStatus
from mongoengine import (
    BooleanField,
    DateTimeField,
    Document,
    EmbeddedDocument,
    EmbeddedDocumentField,
    FloatField,
    IntField,
    ListField,
    StringField,
)

db_client = MongoDBClient.get_instance()


class Gnbdus(Document):
    gnbdu_id = IntField(required=True, unique=True)
    cm_handle = StringField(required=True)
    name = StringField(required=True)
    fdn = StringField(required=True)
    meta = {"db_alias": "arc_db", "collection": "gnbdus"}


class GnbduPairs(EmbeddedDocument):
    p_gnbdu_id = IntField(required=True)
    s_gnbdu_id = IntField(required=True)


class TargetGnbdus(EmbeddedDocument):
    gnbdu_id = IntField(required=True)


class ResultLinks(EmbeddedDocument):
    p_gnbdu_id = IntField(required=True)
    s_gnbdu_id = IntField(required=True)
    usability = FloatField()
    selected_to_configure = BooleanField()
    link_result = StringField()
    s_bdo_id = IntField()
    p_bdo_id = IntField()

    def as_dict(self):
        return {
            "pGnbId": self.p_gnbdu_id,
            "sGnbId": self.s_gnbdu_id,
            "usability": self.usability,
        }


class Optimization(Document):
    creation_date = DateTimeField(required=True)
    status = StringField(choices=ResponseStatus, required=True)
    target_gnbdus = ListField(EmbeddedDocumentField(TargetGnbdus), required=True)
    restricted_links = ListField(EmbeddedDocumentField(GnbduPairs))
    optimization_start_date = DateTimeField()
    optimization_end_date = DateTimeField()
    configuration_start_date = DateTimeField()
    configuration_end_date = DateTimeField()
    result_links = ListField(EmbeddedDocumentField(ResultLinks))
    mandatory_links = ListField(EmbeddedDocumentField(GnbduPairs))
    meta = {"db_alias": "arc_db", "collection": "optimization"}
