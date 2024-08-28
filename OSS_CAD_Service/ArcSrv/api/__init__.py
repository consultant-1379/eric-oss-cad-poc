# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import asyncio
import os

from fastapi import FastAPI
from . import urls
from .api_server import (
    router as app_router,
    tags_metadata as app_tags_metadata
)
from .topology import (
    router as topology_router,
    tag_metadata as topology_tag_metadata
)
from .cm import (
    router as cm_router,
    tag_metadata as cm_tag_metadata
)
from services.scheduler_service import start_scheduler, stop_scheduler

__TITLE = "ARC Rest API"
__DESCRIPTION = """

Welcome to **ARC Rest API** Official Documentation & Testing tool 🚀

This API exposes the following services:
* Optimization Service
* KPIs Service

"""
__VERSION = os.getenv('APP_VERSION', '0.0.1')
__TERMS_OF_SERVICE = "#"
__DOCS_URL = "/"
__REDOC_URL = "/redoc"

app = FastAPI(
    title=__TITLE,
    description=__DESCRIPTION,
    version=__VERSION,
    terms_of_service=__TERMS_OF_SERVICE,
    docs_url=__DOCS_URL,
    redoc_url=__REDOC_URL,
    openapi_tags=app_tags_metadata + [topology_tag_metadata, cm_tag_metadata],
)

app.include_router(app_router)
app.include_router(topology_router, prefix=urls.TOPOLOGY)
app.include_router(cm_router, prefix=urls.CM)


@app.on_event("startup")
async def startup_event():
    app.state.scheduler_tasks = await asyncio.gather(start_scheduler())


@app.on_event("shutdown")
async def shutdown_event():
    await stop_scheduler(app.state.scheduler_tasks)
