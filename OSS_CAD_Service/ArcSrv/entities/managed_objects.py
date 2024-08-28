# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from .references import Dn
from collections.abc import Sequence, Mapping
from typing import Optional


class ManagedObject(object):
    def __init__(self, rdn: Sequence[Dn] = tuple(), attributes: Optional[Mapping] = None, mo_type: Optional[str] = None):
        self.__rdn = rdn
        self.__type = mo_type
        self.__attributes = attributes or dict()

    @property
    def rdn(self):
        return ','.join(map(str, self.__rdn))

    @property
    def id(self):
        return self.__rdn[-1].value if self.rdn else None

    @property
    def type(self):
        return self.__rdn[-1].type if self.rdn else self.__type

    @property
    def attributes(self):
        return self.__attributes | {self.type[0].lower() + self.type[1:] + "Id": self.id} if self.__rdn else {}

    def __setitem__(self, key, value):
        self.__attributes[key] = value

    def __getattr__(self, item):
        return self.__attributes[item]
