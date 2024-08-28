# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum

from .api_response import BaseResponse, ResponseStatus


class OptimizationStopPostResponseResult(str, Enum):
    OPTIMIZATION_STOP_POST_STOPPED_SUCCESS = "Optimization stopped successfully."
    OPTIMIZATION_STOP_POST_WRONG_ID = (
        "No optimization instance is associated with the provided ID."
    )

    # TODO OTHER SCENARIOS


class OptimizationStopPostResponseCls:
    def __init__(
        self, *, status: ResponseStatus, result: OptimizationStopPostResponseResult
    ):
        self.status = status
        self.result = result


class OptimizationStopPostResponse(BaseResponse):
    result: OptimizationStopPostResponseResult

    class Config:
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "result": OptimizationStopPostResponseResult.OPTIMIZATION_STOP_POST_STOPPED_SUCCESS,
            }
        }
