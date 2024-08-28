# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum

from .api_response import BaseResponse, ResponseStatus


class ConfigurationStatusGetResponseMessage(str, Enum):
    CONFIGURATION_STATUS_ID_FOUND = "The configuration id has been found."
    CONFIGURATION_STATUS_GET_WRONG_ID = (
        "No configuration instance is associated with the provided ID."
    )


class ConfigurationStatusGetResponseCls:
    def __init__(
        self,
        *,
        status: ResponseStatus,
        message: ConfigurationStatusGetResponseMessage,
    ):
        self.status = status
        self.message = message


class ConfigurationStatusGetResponse(BaseResponse):
    message: ConfigurationStatusGetResponseMessage

    class Config:
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "message": ConfigurationStatusGetResponseMessage.CONFIGURATION_STATUS_GET_WRONG_ID,
            }
        }
