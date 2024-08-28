# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum

from pydantic import BaseModel


class ResponseStatus(str, Enum):
    SUCCESS = "Success"
    ERROR = "Error"
    CREATED = "Created"
    FAILED = "Failed"
    OPTIMIZATION_IN_QUEUE = "Optimization in queue"
    OPTIMIZATION_IN_PROGRESS = "Optimization in progress"
    OPTIMIZATION_FINISHED = "Optimization finished"
    OPTIMIZATION_COLLECTING_PM_DATA = "Collecting PM data"
    OPTIMIZATION_COLLECTING_CM_DATA = "Collecting CM data"
    OPTIMIZATION_PREDICTING_CELL_CAPABILITIES = "Predicting cell capabilities"
    OPTIMIZATION_BUILDING_CAPABILITY = "Building capability matrix"
    OPTIMIZATION_BUILDING_BB_CONFIGURATIONS = "Building BB configurations"
    OPTIMIZATION_BUILDING_USABILITY = "Building usability matrix"
    OPTIMIZATION_WRONG_ID = (
        "No optimization instance is associated with the provided ID"
    )
    OPTIMIZATION_INVALID_ID = "The provided ID is not valid."
    OPTIMIZATION_UNEXPECTED_ERROR = "Unexpected error during optimization"
    CONFIGURATION_IN_QUEUE = "configuration in queue"
    CONFIGURATION_ONGOING = "configuration ongoing"
    CONFIGURATION_FINISHED = "configuration finished"


class BaseResponse(BaseModel):
    status: ResponseStatus

    class Config:
        orm_mode = True
