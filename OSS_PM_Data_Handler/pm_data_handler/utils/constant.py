# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from abc import ABC, abstractmethod
from typing import Final


class MongoDBConstant(ABC):
    """
    MongoDB Constants definition [ Name <-> Value Mapping ] :
        ORDER = ASCENDING , DESCENDING
        SCRAM AUTH ALGORITHM = SHA256 , SHA1

    """

    ASCENDING: Final = 1
    DESCENDING: Final = -1
    SCRAM_SHA_256: Final = "SCRAM-SHA-256"
    SCRAM_SHA_1: Final = "SCRAM-SHA-1"

    """
        Schema Registry Parameters - abstract_method() -> abstract method

        Desc:
            Prevent creating instances of this configuration class
    """

    @abstractmethod
    def abstract_method(self):
        pass
