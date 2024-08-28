# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import asyncio
import logging

import services.cm_service as cm_service
import services.topology_service as topology_service


async def wait_for_service(ready_event, service_name, health_check):
    while True:
        logging.info("Waiting for %s service to become online...", service_name)
        if health_check():
            break
        await asyncio.sleep(10)
    logging.info("%s service is available", service_name)
    ready_event.set()


async def node_relation_discovery(topology_ready_event, cm_ready_event):
    await topology_ready_event.wait()
    await cm_ready_event.wait()
    logging.info("Starting NodeRelation discovery")
    logging.info("NodeRelation discovery finished, %i nodes were discovered", len(tuple(cm_service.query_node_relations())))


async def start_scheduler():
    topology_ready_event = asyncio.Event()
    cm_ready_event = asyncio.Event()

    tasks = [
        asyncio.create_task(wait_for_service(topology_ready_event, "CTS", topology_service.health_check)),
        asyncio.create_task(wait_for_service(cm_ready_event, "NCMP", cm_service.health_check)),
        asyncio.create_task(node_relation_discovery(topology_ready_event, cm_ready_event))
    ]

    return tasks


async def stop_scheduler(tasks):
    for task in tasks:
        task.cancel()

    await asyncio.sleep(0.1)
    await asyncio.get_running_loop().shutdown_asyncgens()
