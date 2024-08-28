# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging

from . import urls
from .utils import streaming_json_generator
from fastapi import APIRouter
from fastapi.encoders import jsonable_encoder
from fastapi.responses import StreamingResponse, JSONResponse
from services import topology_service

tag = "Topology Service"
tag_metadata = {
    "name": tag,
    "description": "ARC Topology Service is working as a proxy layer towards to the platforms topology service. "
                   "Through this API the Automation Flow can access necessary topology information."
}

router = APIRouter(tags=[tag])
logger = logging.getLogger(__package__)


@router.get(urls.TOPOLOGY_LIST_GNBS, name="List gNodeBs")
async def list_gnodebs():
    """
    Get the list of all gNodeBs
    """

    logger.info("Retrieving all gNodeBs")
    return StreamingResponse(streaming_json_generator(topology_service.stream_nodes(), by_alias=True),
                             media_type=JSONResponse.media_type)


@router.get(urls.TOPOLOGY_GET_GNB_BY_ID, name="Get gNodeB by gNBId")
async def get_gnbdu(gnb_id: int):
    """
    Get detailed info of a specific gNB
    """

    logger.info(f"Retrieving gnbdu with id {gnb_id}")
    return JSONResponse(content=jsonable_encoder(topology_service.get_node_by_gnb_id(gnb_id), by_alias=True))


@router.get(urls.TOPOLOGY_GET_GNB_BY_NAME, name="Get gNodeB by name")
async def get_gnbdu(gnb_name: str):
    """
    Get detailed info of a specific gNB
    """

    logger.info(f"Retrieving gnbdu with name {gnb_name}")
    return JSONResponse(content=jsonable_encoder(topology_service.get_node_by_name(gnb_name), by_alias=True))
