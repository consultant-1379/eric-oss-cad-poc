# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging
import multiprocessing

from configs import cm_config
from clients.connections import RestConnection
from clients.cm import CmClient
from entities.managed_objects import ManagedObject
from entities.references import ExternalId
from entities.topology import GNodeB, NodeRelation
from services.topology_service import iter_catalog, get_node_by_global_id, get_node_by_local_id

__connection = RestConnection(cm_config.hostname, cm_config.username, cm_config.password)
__client = CmClient(__connection)

__cache_manage = multiprocessing.Manager()
__cache = __cache_manage.dict()

EXTERNAL_NODE_FILTER = ManagedObject(mo_type="ExternalGNBCUCPFunction")


def health_check() -> bool:
    try:
        __client.search_handle(json=dict())
        return True
    except Exception as e:
        logging.debug("CM healthcheck failed", e)
        return False


# Todo: response validation
def fetch_neighbor_ids(external_id: ExternalId):
    if external_id in __cache:
        return __cache[external_id]
    else:
        neighbor_ids = [
            (int(relation.pLMNId["mcc"]), int(relation.pLMNId["mnc"]), int(relation.gNBId), int(relation.gNBIdLength))
            for relation in __client.list_resources(external_id, (EXTERNAL_NODE_FILTER,))]
        __cache[external_id] = neighbor_ids
        return neighbor_ids


def __generate_node_relation(node):
    ext_id = node['gNBCUCPFunctionRef'].to_external_id()
    neighbors = tuple(map(lambda neighbor: GNodeB.parse_obj(neighbor),
                      filter(lambda potential_neighbor: potential_neighbor is not None,
                             map(get_node_by_global_id, fetch_neighbor_ids(ext_id)))))

    return NodeRelation(node=GNodeB.parse_obj(node), neighbors=neighbors)


def query_node_relations(gnb_ids=None):
    node_iterator = iter_catalog() if not gnb_ids else iter(
        filter(lambda y: y is not None, map(lambda x: get_node_by_local_id(x), gnb_ids)))

    with multiprocessing.Pool(6) as pool:
        for result in pool.imap(__generate_node_relation, node_iterator):
            yield result
        return
