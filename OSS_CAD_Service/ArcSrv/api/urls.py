# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

OPTIMIZATION = "/optimizations/"
OPTIMIZATION_START_WITH_ID = "/optimizations/{optimization_id}/start"
OPTIMIZATION_STATUS_WITH_ID = "/optimizations/{optimization_id}/status"
OPTIMIZATION_STOP_WITH_ID = "/optimizations/{optimization_id}/stop"

CONFIGURATION = "/configurations/"
CONFIGURATION_STATUS_WITH_ID = "/configurations/{configuration_id}/status"

KPIS = "/kpis/"

TOPOLOGY = "/topology"
TOPOLOGY_LIST_GNBS = "/gnbs"
TOPOLOGY_GET_GNB_BY_ID = "/gnbs/{gnb_id}"
TOPOLOGY_GET_GNB_BY_NAME = "/gnbByName/{gnb_name}"

CM = "/CM"
CM_QUERY_NODE_RELATIONS = "/nodeRelations"
