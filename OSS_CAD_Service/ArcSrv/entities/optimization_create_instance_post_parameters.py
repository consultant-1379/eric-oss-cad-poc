# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum
from typing import List

from pydantic import BaseModel, Field

from .api_response import BaseResponse, ResponseStatus


class GNodeB(BaseModel):
    gnb_id: int = Field(alias="gnbId", alias_priority=1)
    cm_handle: str = Field(alias="cmHandle", alias_priority=1)
    # TODO: name: str = Field(alias="name", alias_priority=1)
    #       fdn: str = Field(alias="fdn", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True


class SelectedNodes(BaseModel):
    gnb_id_list: List[GNodeB] = Field(alias="gnbIdList", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True


class GNodeBPairs(BaseModel):
    p_gnbdu_id: int = Field(alias="pGnbduId", alias_priority=1)
    s_gnbdu_id: int = Field(alias="sGnbduId", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True


class NodePairsList(BaseModel):
    gnb_pairs_list: List[GNodeBPairs] = Field(alias="gnbPairsList", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True


class OptimizationCreateInstancePostRequestBody(BaseModel):
    selected_nodes: SelectedNodes = Field(alias="selectedNodes", alias_priority=1)
    unwanted_node_pairs: NodePairsList = Field(
        alias="unwantedNodePairs", alias_priority=1
    )
    mandatory_node_pairs: NodePairsList = Field(
        alias="mandatoryNodePairs", alias_priority=1
    )

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "selectedNodes": {
                    "gnbIdList": [
                        {
                            "gnbId": 208727,
                            "cmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449",
                        },
                        {
                            "gnbId": 208731,
                            "cmHandle": "07148148A84D38E0404CFAEB5CA09309",
                        },
                    ]
                },
                "unwantedNodePairs": {
                    "gnbPairsList": [{"pGnbduId": 208727, "sGnbduId": 208731}]
                },
                "mandatoryNodePairs": {
                    "gnbPairsList": [{"pGnbduId": 208731, "sGnbduId": 208727}]
                },
            }
        }


class OptimizationCreateInstancePostResponseResult(str, Enum):
    OPTIMIZATION_CREATE_INSTANCE_POST_DATA_NOT_PROVIDED_400 = "No data to optimize."
    OPTIMIZATION_CREATE_INSTANCE_POST_MISSING_DATA_400 = (
        "One of the required entities needed for optimization is missing "
        "in the provided data."
    )
    OPTIMIZATION_CREATE_INSTANCE_POST_CREATED_SUCCESS_200 = (
        "Optimization instance created."
    )


class OptimizationCreateInstancePostResponseCls:
    def __init__(
        self,
        *,
        status: ResponseStatus,
        optimization_id: str,
        result: OptimizationCreateInstancePostResponseResult
    ):
        self.status = status
        self.optimization_id = optimization_id
        self.result = result


class OptimizationCreateInstancePostResponse(BaseResponse):
    result: OptimizationCreateInstancePostResponseResult
    optimization_id: str = Field(alias="optimizationId", alias_priority=1)

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "optimizationId": "145721ABCD",
                "result": (
                    OptimizationCreateInstancePostResponseResult.OPTIMIZATION_CREATE_INSTANCE_POST_CREATED_SUCCESS_200
                ),
            }
        }
