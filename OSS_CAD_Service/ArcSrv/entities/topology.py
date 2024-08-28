# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from entities import CustomBaseModel
from pydantic import Field
from typing import List


class GNodeB(CustomBaseModel):
    name: str
    mcc: int
    mnc: int
    gnb_id: int = Field(alias="gNBId", alias_priority=1)
    gnb_id_length: int = Field(alias="gNBIdLength", alias_priority=1)
    cm_handle: str = Field(alias="cmHandle", alias_priority=1)

    def __hash__(self):
        return hash((self.mcc, self.mnc, self.gnb_id, self.gnb_id_length))


class NodeRelation(CustomBaseModel):
    node: GNodeB
    neighbors: List[GNodeB]
