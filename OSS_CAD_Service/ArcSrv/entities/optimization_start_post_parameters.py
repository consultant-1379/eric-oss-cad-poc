# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum

from .api_response import BaseResponse, ResponseStatus


class OptimizationStartPostResponseResult(str, Enum):
    OPTIMIZATION_START_POST_BUSY_409 = (
        "Optimization process already started with the given optimizationID."
    )
    OPTIMIZATION_START_POST_DATA_NOT_PROVIDED_400 = "No data to optimize."
    OPTIMIZATION_START_POST_START_SUCCESS_200 = "Optimization started."
    OPTIMIZATION_START_POST_ID_NOT_EXIST_404 = (
        "Optimization instance ID does not exist."
    )
    OPTIMIZATION_START_POST_INVALID_ID_404 = (
        "The provided Optimization instance ID is not valid."
    )


class OptimizationStartPostResponseCls:
    def __init__(
        self, *, status: ResponseStatus, result: OptimizationStartPostResponseResult
    ):
        self.status = status
        self.result = result


class OptimizationStartPostResponse(BaseResponse):
    result: OptimizationStartPostResponseResult

    class Config:
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "result": OptimizationStartPostResponseResult.OPTIMIZATION_START_POST_START_SUCCESS_200,
            }
        }
