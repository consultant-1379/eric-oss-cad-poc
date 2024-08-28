# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from enum import Enum
from .connections import (
    RestConnection,
    HttpMethod)


class TopologyType(Enum):
    GNBDU = "ctw/gnbdu"
    GNBCUCP = "ctw/gnbcucp"
    GNBCUUP = "ctw/gnbcuup"
    NRCELL = "ctw/nrcell"
    NETFUNCON = "ctw/netfunctioncon"


class TopologyClient(object):
    TOPOLOGY = "topology"
    CONTEXT_PATH = "/oss-core-ws/rest/"

    def __init__(self, connection: RestConnection):
        self.__connection = connection

    def get(self, topology_type: TopologyType, identity: int):
        return self.__connection.request_json(
            HttpMethod.GET, f"{TopologyClient.CONTEXT_PATH}{topology_type.value}/{identity}")

    def count(self, topology_type: TopologyType, **kwargs) -> int:
        params = dict((k, v) for k, v in kwargs.get('params', dict()).items() if not k.startswith("fs."))
        return self.__connection.request_json(
            HttpMethod.GET, f"{TopologyClient.CONTEXT_PATH}{topology_type.value}Task/{'countByCriteria' if params else 'count'}", params=params)

    def _query(self, topology_type: TopologyType, **kwargs):
        return self.__connection.request_json(
                HttpMethod.GET, TopologyClient.CONTEXT_PATH + str(topology_type.value), **kwargs)

    def list(self, topology_type: TopologyType, **kwargs):
        count = self.count(topology_type, params=kwargs.get("params", dict()))
        objects = []
        while count > 0:
            params = kwargs.get("params", {"criteria": f"(objectInstId > {objects[-1]['id'] if objects else 0}L)", "sort": "objectInstId"})
            batch = self._query(topology_type, params=params)
            count -= len(batch)
            objects += batch
        return objects
