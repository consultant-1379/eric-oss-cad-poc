# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum
from typing import Dict, List

from pydantic import BaseModel, Field

from .api_response import BaseResponse, ResponseStatus


class GNodeB(BaseModel):

    gnb_id: int = Field(alias="gnbId", alias_priority=1)
    cm_handle: str = Field(alias="cmHandle", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True


class KpiRetrievePostRequestBody(BaseModel):
    gnb_list: List[GNodeB] = Field(alias="gnbList", alias_priority=1)

    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "gnbList": [
                    {
                        "gnbId": 208727,
                        "cmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449",
                    },
                    {
                        "gnbId": 208731,
                        "cmHandle": "07148148A84D38E0404CFAEB5CA09309",
                    },
                ]
            }
        }


class KpiRetrievePostResponseResult(str, Enum):
    KPI_RETRIEVE_POST_MISSING_DATA_400 = (
        "One of the required entities needed for retrieving KPIs is missing "
        "in the provided data."
    )
    KPI_RETRIEVE_POST_SUCCESS_200 = (
        "KPIs for the provided data has been retrieved successfully."
    )


class KpiRetrievePostResponseCls:
    def __init__(
        self,
        *,
        status: ResponseStatus,
        kpi_data: Dict,
        result: KpiRetrievePostResponseResult
    ):
        self.status = status
        self.kpi_data = kpi_data
        self.result = result


class KpiRetrievePostResponse(BaseResponse):
    result: KpiRetrievePostResponseResult
    kpi_data: Dict = Field(alias="KpiData", alias_priority=1)

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "result": KpiRetrievePostResponseResult.KPI_RETRIEVE_POST_SUCCESS_200,
                "kpiData": {
                    "KPI1": 1,
                    "KPI2": 1,
                    "KPI3": 1,
                },
            }
        }
