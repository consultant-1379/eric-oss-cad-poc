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
from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse, JSONResponse
from services import cm_service

tag = "CM Service"
tag_metadata = {
    "name": tag,
    "description": "ARC CM Service is working as a proxy layer towards to the platforms cm service. "
                   "Through this API the Automation Flow can access necessary cm information."
}

router = APIRouter(tags=[tag])
logger = logging.getLogger(__package__)


@router.get(urls.CM_QUERY_NODE_RELATIONS, name="Query Node relations")
async def query_node_relations(gnb_id: list[int] | None = Query(None, description="Optional gNBId filter", alias="gNBId")):
    """
    Query node relations
    """

    logger.info("Retrieving node relations")
    return StreamingResponse(streaming_json_generator(cm_service.query_node_relations(gnb_id), by_alias=True),
                             media_type=JSONResponse.media_type)
