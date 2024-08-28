# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from pydantic import BaseModel, Field


class BBLink(BaseModel):
    p_gnbdu_id: int = Field(alias="pGnbId", alias_priority=1)
    s_gnbdu_id: int = Field(alias="sGnbId", alias_priority=1)
    usability: float = Field(alias="usability", alias_priority=1)

    class Config:
        allow_population_by_field_name = True
