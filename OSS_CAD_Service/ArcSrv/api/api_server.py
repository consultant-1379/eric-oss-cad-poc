#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
import json
import logging
from entities.configuration_status_get_parameters import ConfigurationStatusGetResponse
from entities.kpi_retrieve_post_parameters import (
    KpiRetrievePostRequestBody,
    KpiRetrievePostResponse,
)
from entities.optimization_create_instance_post_parameters import (
    OptimizationCreateInstancePostRequestBody,
    OptimizationCreateInstancePostResponse,
)
from entities.optimization_start_post_parameters import OptimizationStartPostResponse
from entities.optimization_status_get_parameters import OptimizationStatusGetResponse
from entities.optimization_stop_post_parameters import OptimizationStopPostResponse
from entities.partner_configuration_create_instance_post_parameters import (
    PartnerConfigurationCreateInstancePostRequestBody,
    PartnerConfigurationCreateInstancePostResponse,
)
from fastapi import APIRouter, Response
from services import kpi_service, optimization_service

from . import urls

logger = logging.getLogger(__package__)

router = APIRouter()
tags_metadata = [
    {
        "name": "Optimization Service",
        "description": "ARC Optimization is working on predicting best BB Links configuration in order to "
                       "maximize network performance. This service handles optimization and configuration related"
                       " requests.",
    },
    {
        "name": "KPIs Service",
        "description": "ARC KPIs is responsible of managing the recent BB Links configuration's Key Performance "
                       "Indicators. This service retrieves from the the latest KPIs from the currently stored PM "
                       "Data.",
    },
]


#####################################
# OPTIMIZATION SERVICE
#####################################


@router.get(
    urls.OPTIMIZATION_STATUS_WITH_ID,
    tags=["Optimization Service"],
    response_model=OptimizationStatusGetResponse,
    response_model_by_alias=True,
    name="Check current optimization status of an instance against its ID. If finished successfully, it will contain "
    "the list of optimal BB pairs.",
)
async def get_optimize(optimization_id: str, response: Response):
    logger.info("Check Optimization Status: %s", optimization_id)
    (
        optimization_status_outcome,
        response_code,
    ) = optimization_service.get_optimization_status(optimization_id)
    response.status_code = response_code
    return optimization_status_outcome


@router.post(
    urls.OPTIMIZATION,
    tags=["Optimization Service"],
    response_model=OptimizationCreateInstancePostResponse,
    name="Creating an optimization instance for target gNBs provided in the request body, and assigning an optimization"
    " ID to a newly created instance.",
)
async def post_create_optimization_instance(
    request_body: OptimizationCreateInstancePostRequestBody, response: Response
):
    logger.info("Before Create Optimization Instance")
    request_body_as_json = json.loads(
        json.dumps(request_body, default=lambda o: o.to_json())
    )
    logger.info("Create Optimization Instance : %s", json.dumps(request_body_as_json))
    (
        optimization_create_outcome,
        response_code,
    ) = optimization_service.create_optimization_instance(request_body_as_json)
    response.status_code = response_code
    return optimization_create_outcome


@router.post(
    urls.OPTIMIZATION_START_WITH_ID,
    tags=["Optimization Service"],
    response_model=OptimizationStartPostResponse,
    name="Launches an BB pair optimization for a relevant optimization instance using an ID for target gNBs provided "
    "in the request body.",
)
async def post_start_optimization(optimization_id: str, response: Response):
    logger.info("Start Optimization %s", optimization_id)
    (
        optimization_start_outcome,
        response_code,
    ) = optimization_service.start_optimization(optimization_id)
    response.status_code = response_code
    return optimization_start_outcome


@router.post(
    urls.OPTIMIZATION_STOP_WITH_ID,
    tags=["Optimization Service"],
    response_model=OptimizationStopPostResponse,
    name="Cancels (stop) an optimization given its ID.",
)
async def post_stop_optimization(optimization_id: str, response: Response):
    logger.info("Stop Optimization Instance : %s", optimization_id)
    (optimization_stop_outcome, response_code) = optimization_service.stop_optimization(
        optimization_id
    )
    response.status_code = response_code
    return optimization_stop_outcome


#####################################
# CONFIGURATION
#####################################


@router.post(
    urls.CONFIGURATION,
    tags=["Configuration Service"],
    response_model=PartnerConfigurationCreateInstancePostResponse,
    name="Creating/Starting a configuration instance.",
)
async def post_create_configuration_instance(
    request_body: PartnerConfigurationCreateInstancePostRequestBody, response: Response
):
    request_body_as_json = json.loads(
        json.dumps(request_body, default=lambda o: o.to_json())
    )
    logger.info("Create Configuration Instance : %s", json.dumps(request_body_as_json))
    (
        create_configuration_instance_outcome,
        response_code,
    ) = optimization_service.create_configuration_instance(request_body_as_json)
    response.status_code = response_code
    return create_configuration_instance_outcome


@router.get(
    urls.CONFIGURATION_STATUS_WITH_ID,
    tags=["Configuration Service"],
    response_model=ConfigurationStatusGetResponse,
    response_model_by_alias=True,
    name="Check current configuration status of an instance against its ID.",
)
async def get_configuration_status(configuration_id: int, response: Response):
    logger.info("Check Configuration Status : %s", configuration_id)
    (
        configuration_status,
        response_code,
    ) = optimization_service.get_configuration_status(configuration_id)
    response.status_code = response_code
    return configuration_status


#####################################
# KPI SERVICE
#####################################


@router.post(
    urls.KPIS,
    tags=["KPIs Service"],
    response_model=KpiRetrievePostResponse,
    name="Retrieve the latest KPIs for the current optimization.",
)
async def post_retrieving_kpi(
    request_body: KpiRetrievePostRequestBody, response: Response
):
    request_body_as_json = json.loads(
        json.dumps(request_body, default=lambda o: o.to_json())
    )
    logger.info("Retrieve the KPIs : %s", json.dumps(request_body_as_json))
    (
        retrieve_kpi,
        response_code,
    ) = kpi_service.retrieve_kpi(request_body_as_json)
    response.status_code = response_code
    return retrieve_kpi


###########################################
