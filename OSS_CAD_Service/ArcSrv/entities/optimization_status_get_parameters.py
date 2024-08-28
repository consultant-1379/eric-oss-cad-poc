# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from typing import List

from .api_response import BaseResponse, ResponseStatus
from .bb_links import BBLink


class OptimizationStatusGetResponseCls:
    def __init__(self, *, status: ResponseStatus, result: List[BBLink]):
        self.status = status
        self.result = result


class OptimizationStatusGetResponse(BaseResponse):

    status: ResponseStatus
    result: List[BBLink]

    class Config:
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "result": [
                    {
                        "pGnbId": 208728,
                        "sGnbId": 208729,
                        "usability": 0.125,
                    },
                    {
                        "pGnbId": 208729,
                        "sGnbId": 208730,
                        "usability": 0.143,
                    },
                ],
            }
        }
