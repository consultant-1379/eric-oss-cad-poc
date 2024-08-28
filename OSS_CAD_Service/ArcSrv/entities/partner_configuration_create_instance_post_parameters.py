# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from pydantic import BaseModel, Field

from .api_response import BaseResponse, ResponseStatus


class PartnerConfigurationCreateInstancePostRequestBody(BaseModel):
    def to_json(self):
        return self.__dict__

    class Config:
        allow_population_by_field_name = True
        schema_extra = {"example": {}}


class PartnerConfigurationCreateInstancePostResponseResult(str):
    CREATED_SUCCESS_200 = "Partner configuration instance created successfully."


class PartnerConfigurationCreateInstancePostResponseCls:
    def __init__(
        self,
        *,
        status: ResponseStatus,
        configuration_id: int,
        result: PartnerConfigurationCreateInstancePostResponseResult
    ):
        self.status = status
        self.configuration_id = configuration_id
        self.result = result


class PartnerConfigurationCreateInstancePostResponse(BaseResponse):
    result: PartnerConfigurationCreateInstancePostResponseResult
    configuration_id: int = Field(alias="configurationId", alias_priority=1)

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "status": ResponseStatus.SUCCESS,
                "configurationId": 12345,
                "result": (
                    PartnerConfigurationCreateInstancePostResponseResult.CREATED_SUCCESS_200
                ),
            }
        }
