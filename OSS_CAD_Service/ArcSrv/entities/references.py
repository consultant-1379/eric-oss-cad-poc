# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import hashlib
import re
from dataclasses import dataclass, field
from itertools import combinations_with_replacement
from collections.abc import Sequence
from typing import Tuple

MANAGED_ELEMENT = "ManagedElement"
CM_HANDLE_SALT = "EricssonENMAdapter-"
ERICSSON_ENM_NAMESPACE = "ericsson-enm"
SLASH = "/"


@dataclass
class Dn:
    type: str
    value: str
    namespace: str = ERICSSON_ENM_NAMESPACE

    def __str__(self):
        return f"{self.type}={self.value}"

    def __hash__(self):
        return hash((self.type, self.value, self.namespace))

    @property
    def full_name(self):
        return f"{self.namespace}:{self.type}={self.value}"


@dataclass
class ExternalId:
    DN_PATTERN = re.compile("/(?:(?P<namespace>[^:]+):)?(?P<type>[^=]+)=(?P<id>[^/]+)")

    cm_handle: str
    identifier: Tuple[Dn, ...] = field(default_factory=tuple)

    def __str__(self) -> str:
        return f"{self.cm_handle}{self.resource_identifier}"

    def __hash__(self):
        return hash((self.cm_handle, *self.identifier))

    @staticmethod
    def from_str(external_id: str) -> 'ExternalId':
        i = external_id.index('/')
        identifier = []

        for namespace, d_type, value in re.findall(ExternalId.DN_PATTERN, external_id[i:]):
            identifier.append(Dn(d_type, value, namespace) if namespace else Dn(d_type, value))

        return ExternalId(external_id[:i], tuple(identifier))

    @staticmethod
    def generate_resource_identifier(relative_dn: Sequence[Dn]):
        return ''.join(map(lambda x: f"/{x.full_name}", relative_dn))

    @property
    def resource_identifier(self) -> str:
        return self.generate_resource_identifier(self.identifier)

    @property
    def type(self) -> str:
        if self.identifier:
            return self.identifier[-1].type
        return MANAGED_ELEMENT

    def get_parent(self) -> 'ExternalId':
        return ExternalId(self.cm_handle, self.identifier[:-1])


@dataclass
class Fdn:
    DN_PATTERN = re.compile("(?P<type>[^,]+)=(?P<id>[^=,]+)")

    __dn_prefix: Tuple[Dn, ...] = field(default_factory=tuple)
    __resource: Tuple[Dn, ...] = field(default_factory=tuple)

    @staticmethod
    def from_str(fdn: str) -> 'Fdn':
        dn_prefix, resource = [], []

        dn_s = dn_prefix
        for dn in re.findall(Fdn.DN_PATTERN, fdn):
            dn = Dn(*dn)
            if dn.type == MANAGED_ELEMENT:
                dn_s = resource
            dn_s.append(dn)
        return Fdn(tuple(dn_prefix), tuple(resource))

    @property
    def target_dn(self) -> Dn:
        return self.__resource[-1]

    @property
    def dn_prefix(self) -> str:
        return ",".join(map(str, self.__dn_prefix))

    @property
    def resource(self) -> str:
        return ",".join(map(str, self.__resource))

    def __str__(self) -> str:
        return ",".join(map(str, self.__dn_prefix + self.__resource))

    def get_parent(self) -> 'Fdn':
        return Fdn(self.__dn_prefix, self.__resource[:-1])

    def to_external_id(self) -> ExternalId:
        cm_handle = hashlib.md5(
            f"{CM_HANDLE_SALT}{','.join(map(str, (*self.__dn_prefix, self.__resource[0])))}".encode()
        ).hexdigest().upper()
        return ExternalId(cm_handle, self.__resource)

    def to_topology_name(self) -> str:
        return SLASH.join(map(lambda x: str(x.value), self.__dn_prefix + self.__resource))


def reverse_engineer_fdn(external_id: ExternalId, topology_name) -> Fdn:
    depth = external_id.resource_identifier.count(SLASH)
    dn_values = topology_name.split(SLASH)
    values = dn_values[:-depth]

    for arg in combinations_with_replacement(["SubNetwork", "MeContext"], len(values)):
        dn_prefix = (",".join(map(lambda x: x + "={}", arg))).format(*values)
        if hashlib.md5(f"{CM_HANDLE_SALT}{dn_prefix},{MANAGED_ELEMENT}={dn_values[-depth]}".encode())\
                .hexdigest().upper() == external_id.cm_handle:
            return Fdn.from_str(dn_prefix + re.sub(r"(/(?:[^/:]*:)?)", ',', external_id.resource_identifier))
    raise RuntimeError("Could reverse engineer FDN")
