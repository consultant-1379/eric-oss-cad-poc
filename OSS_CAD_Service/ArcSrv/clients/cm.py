# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import re

from clients.connections import RestConnection, HttpMethod
from entities.references import ExternalId, Dn, MANAGED_ELEMENT
from entities.managed_objects import ManagedObject
from functools import lru_cache
from collections.abc import Sequence
from typing import Tuple

ATTRIBUTES = "attributes"
RESOURCE_IDENTIFIER = "resourceIdentifier"
OPTIONS = "options"


class _MoVisitor:
    def __init__(self):
        self.result = []

    def visit_objects(self, resources, path: Tuple[Dn, ...] = tuple(), r_type: str = MANAGED_ELEMENT):
        r_type = re.sub(r"^([^:]+:)?", "", r_type)

        for resource in resources:
            r_path = path + (Dn(r_type, resource['id']),)
            self.visit_object(resource, r_path)

    def visit_object(self, resource, path: Tuple[Dn, ...] = tuple()):
        for key, value in resource.items():
            if key == 'id':
                pass
            elif key == ATTRIBUTES:
                self.result.append(ManagedObject(rdn=path, attributes=value, mo_type=path[-1].type))
            else:
                self.visit_objects(value, path, key)


class CmClient(object):
    CONTEXT_PATH = "/ncmp/v1/ch/"
    RESOURCE_PATH = CONTEXT_PATH + "{cm_handle}/data/ds/ncmp-datastore:passthrough-running"

    def __init__(self, connection: RestConnection):
        self.__connection = connection

    def search_handle(self, **kwargs):
        return self.__connection.request_json(HttpMethod.POST, f"{CmClient.CONTEXT_PATH}searches", **kwargs)

    def get_handle_details(self, cm_handle: str):
        return self.__connection.request_json(HttpMethod.GET, f"{CmClient.CONTEXT_PATH}{cm_handle}")

    def create_resource(self, external_id: ExternalId, manage_object: ManagedObject):
        parent_ref = external_id.get_parent()
        params = {RESOURCE_IDENTIFIER: parent_ref.resource_identifier}
        body = {
            manage_object.type: [{
                "id": manage_object.id,
                ATTRIBUTES: manage_object.attributes
            }]
        }
        self.__connection.request(
            HttpMethod.POST, CmClient.RESOURCE_PATH.format(cm_handle=external_id.cm_handle), params=params, json=body)

    def patch_resource(self, external_id: ExternalId, manage_object: ManagedObject, fields: Sequence):
        parent_ref = external_id.get_parent()
        params = {RESOURCE_IDENTIFIER: parent_ref.resource_identifier}
        body = {
            manage_object.type: [{
                "id": manage_object.id,
                ATTRIBUTES: dict(filter(lambda x: x[0] in fields, manage_object.attributes.items()))
            }]
        }
        self.__connection.request_json(
            HttpMethod.PATCH, CmClient.RESOURCE_PATH.format(cm_handle=external_id.cm_handle), params=params, json=body)

    def delete_resource(self, external_id: ExternalId):
        params = {RESOURCE_IDENTIFIER: external_id.resource_identifier}
        self.__connection.request(
            HttpMethod.DELETE, CmClient.RESOURCE_PATH.format(cm_handle=external_id.cm_handle), params=params, json={})

    def get_resource(self, external_id: ExternalId) -> ManagedObject:
        params = {RESOURCE_IDENTIFIER: external_id.resource_identifier}
        mo_visitor = _MoVisitor()
        mo_visitor.visit_object(self.__connection.request_json(
            HttpMethod.GET, CmClient.RESOURCE_PATH.format(cm_handle=external_id.cm_handle), params=params))
        return mo_visitor.result[0] if len(mo_visitor.result) == 1 else None

    def list_resources(self, external_id: ExternalId, filters: Tuple[ManagedObject] = tuple()) -> Sequence[ManagedObject]:
        params = {RESOURCE_IDENTIFIER: external_id.resource_identifier,
                  OPTIONS: self._generate_options(filters or (ManagedObject(mo_type=external_id.type),))}
        mo_visitor = _MoVisitor()
        mo_visitor.visit_object(self.__connection.request_json(
            HttpMethod.GET, CmClient.RESOURCE_PATH.format(cm_handle=external_id.cm_handle), params=params))
        return mo_visitor.result

    @staticmethod
    @lru_cache
    def _generate_options(mos):
        attributes, filters, scopes = [], [], []

        for mo in dict(map(lambda x: (x.type, x), mos)).values():
            prefix = f"{mo.type}/{ATTRIBUTES}" if mo.type else ATTRIBUTES
            filters.append(f"{prefix}({';'.join(mo.attributes.keys()) or '*'})")
            scope = [(k, v) for k, v in mo.attributes.items() if v is not None]
            if scope:
                scopes.append(f"{prefix}({';'.join(map(lambda y: f'{y[0]}={y[1]}', scope))})")

        if filters:
            attributes.append(f"fields={';'.join(filters)}")
        if scopes:
            attributes.append(f"scope={';'.join(scopes)}")

        return ",".join(attributes)
