# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging
import threading

from configs import topology_config
from clients.connections import RestConnection
from clients.topology import TopologyClient, TopologyType
from clients.utils import validate_json
from entities.topology import GNodeB
from entities.references import reverse_engineer_fdn, ExternalId
from typing import Tuple

__connection = RestConnection(topology_config.hostname, topology_config.username, topology_config.password)
__client = TopologyClient(__connection)
__lock = threading.Lock()
__node_catalog = dict()
__node_local_catalog = dict()


def health_check() -> bool:
    try:
        count_nodes()
        return True
    except Exception as e:
        logging.debug("Topology healthcheck failed", e)
        return False


@validate_json("topology/count.json")
def count_nodes():
    return __client.count(TopologyType.GNBDU)


@validate_json("topology/count.json")
def count_cells():
    return __client.count(TopologyType.NRCELL)


@validate_json("topology/gnb.json")
def get_node(identity: int):
    return __client.get(TopologyType.GNBDU, identity)


@validate_json("topology/nrcell.json")
def get_cell(identity: int):
    return __client.get(TopologyType.NRCELL, identity)


@validate_json("topology/gnbs.json")
def list_nodes(**kwargs):
    return __client.list(TopologyType.GNBDU, **kwargs)


@validate_json("topology/nrcells.json")
def list_cells(**kwargs):
    return __client.list(TopologyType.NRCELL, **kwargs)


@validate_json("topology/function-connections.json")
def list_function_connections(**kwargs):
    return __client.list(TopologyType.NETFUNCON, **kwargs)


def get_node_by_name(gnb_name):
    for gnbdu in __client.list(TopologyType.GNBDU, params={"name.lk": f"'%/{gnb_name}/%'", "fs.wirelessNFConnections": "assoc"}):
        node = __extract_node_data(gnbdu)
        for connection in gnbdu['wirelessNFConnections']:
            _, con_data = __extract_connection_data(connection['value'])
            node |= con_data
        return GNodeB.parse_obj(node)


def get_node_by_gnb_id(gnb_id):
    for connection in __client.list(TopologyType.NETFUNCON,
                                    params={"name.lk": f"'%-{gnb_id}-__'", "fs.wirelessNetFunctions": "assoc"}):
        return __extract_data(connection)


# Todo: Implement a proper topology node lookup if the link create got fixed
# Todo: Don't forget that 3gpp allows multiple DU & CUUP for a node (ericsson not implementing this at the moment)
def stream_nodes():
    for connection in __client.list(TopologyType.NETFUNCON, params={"fs.wirelessNetFunctions": "assoc"}):
        yield __extract_data(connection)


def __extract_data(connection):
    cgi, node = __extract_connection_data(connection)
    for wireless in connection['wirelessNetFunctions']:
        node |= __extract_node_data(wireless['value'])
    with __lock:
        __node_catalog[cgi] = node
        __node_local_catalog[cgi[2]] = node
    return GNodeB.parse_obj(node)


def __extract_node_data(node):
    external_id, name = ExternalId.from_str(node['externalId']), node['name']
    function_ref = f"{external_id.type}Ref"
    function_ref = function_ref[0].lower() + function_ref[1:]
    return {
        "name": external_id.identifier[0].value,
        "cmHandle": external_id.cm_handle,
        function_ref: reverse_engineer_fdn(external_id, name)
    }


def __extract_connection_data(connection):
    mcc, mnc, gnb_id, gnb_id_length = tuple(map(int, connection['name'].split('-')))
    return (mcc, mnc, gnb_id, gnb_id_length), {'mcc': mcc, 'mnc': mnc, 'gNBId': gnb_id, 'gNBIdLength': gnb_id_length}


def _catalog(func):
    def wrapper(*args, **kwargs):
        if not __node_catalog:
            tuple(stream_nodes())
        with __lock:
            return func(*args, **kwargs)
    return wrapper


@_catalog
def iter_catalog():
    for v in __node_catalog.values():
        yield v
    return


@_catalog
def get_node_by_global_id(global_id: Tuple[int, int, int, int]):
    return __node_catalog.get(global_id)


@_catalog
def get_node_by_local_id(gnb_id: int):
    return __node_local_catalog.get(gnb_id)
