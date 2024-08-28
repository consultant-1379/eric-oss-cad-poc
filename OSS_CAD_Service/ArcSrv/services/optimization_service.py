# !/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
import datetime
import logging
import threading
import time

import pymongoose
from entities.api_response import ResponseStatus
from entities.configuration_status_get_parameters import (
    ConfigurationStatusGetResponse,
    ConfigurationStatusGetResponseCls,
    ConfigurationStatusGetResponseMessage,
)
from entities.database_collection_models import Gnbdus
from entities.optimization_create_instance_post_parameters import (
    OptimizationCreateInstancePostResponse,
    OptimizationCreateInstancePostResponseCls,
    OptimizationCreateInstancePostResponseResult,
)
from entities.optimization_start_post_parameters import (
    OptimizationStartPostResponse,
    OptimizationStartPostResponseCls,
    OptimizationStartPostResponseResult,
)
from entities.optimization_status_get_parameters import (
    OptimizationStatusGetResponse,
    OptimizationStatusGetResponseCls,
)
from entities.optimization_stop_post_parameters import (
    OptimizationStopPostResponse,
    OptimizationStopPostResponseCls,
    OptimizationStopPostResponseResult,
)
from entities.partner_configuration_create_instance_post_parameters import (
    PartnerConfigurationCreateInstancePostResponse,
    PartnerConfigurationCreateInstancePostResponseCls,
    PartnerConfigurationCreateInstancePostResponseResult,
)
from fastapi import status
from helper import optimization_threading_helper
from optimization import build_solution
from services import database_service

logger = logging.getLogger(__name__)

# Configuration Instances IDs and logic
configuration_instance_id_counter = 0
configuration_instances_with_ids_and_data = {}


def create_optimization_instance(request_body_as_json):
    response_code = status.HTTP_400_BAD_REQUEST
    response_orm = OptimizationCreateInstancePostResponseCls(
        status=ResponseStatus.FAILED,
        optimization_id=str(None),
        result=OptimizationCreateInstancePostResponseResult.OPTIMIZATION_CREATE_INSTANCE_POST_MISSING_DATA_400,
    )
    response = OptimizationCreateInstancePostResponse.from_orm(response_orm)
    if (
        "selected_nodes" in request_body_as_json
        and "unwanted_node_pairs" in request_body_as_json
        and "mandatory_node_pairs" in request_body_as_json
    ):
        selected_nodes = request_body_as_json["selected_nodes"]
        unwanted_node_pairs = request_body_as_json["unwanted_node_pairs"]
        mandatory_node_pairs = request_body_as_json["mandatory_node_pairs"]
        if (
            "gnb_id_list" in selected_nodes
            and "gnb_pairs_list" in unwanted_node_pairs
            and "gnb_pairs_list" in mandatory_node_pairs
        ):
            database_service.persist_gnbdus(selected_nodes)
            optimization_identifier = (
                database_service.persist_newly_created_optimization_instance(
                    datetime.datetime.now(),
                    ResponseStatus.CREATED,
                    selected_nodes,
                    unwanted_node_pairs,
                    mandatory_node_pairs,
                )
            )
            response_orm = OptimizationCreateInstancePostResponseCls(
                status=ResponseStatus.SUCCESS,
                optimization_id=get_str(optimization_identifier),
                result=(
                    OptimizationCreateInstancePostResponseResult.OPTIMIZATION_CREATE_INSTANCE_POST_CREATED_SUCCESS_200
                ),
            )
            response = OptimizationCreateInstancePostResponse.from_orm(response_orm)
            response_code = status.HTTP_200_OK
    return response, response_code


def get_str(id):
    if isinstance(id, str):
        return id
    else:
        return id.__str__()


def start_optimization(optimization_id: str):
    response_code = status.HTTP_404_NOT_FOUND
    if pymongoose.ObjectId.is_valid(optimization_id):
        response_orm = OptimizationStartPostResponseCls(
            status=ResponseStatus.ERROR,
            result=OptimizationStartPostResponseResult.OPTIMIZATION_START_POST_ID_NOT_EXIST_404,
        )
        response = OptimizationStartPostResponse.from_orm(response_orm)
        current_optimization = database_service.get_optimization_by_id(
            optimization_id, "Optimization"
        )
        if current_optimization:
            stop_ongoing_optimization(optimization_id)
            if not optimization_threading_helper.task_thread.is_alive():
                (
                    gnb_dict,
                    unwanted_bb_links,
                    mandatory_bb_links,
                ) = parse_start_optimization_body(
                    current_optimization.target_gnbdus,
                    current_optimization.restricted_links,
                    current_optimization.mandatory_links,
                )
                response_orm = OptimizationStartPostResponseCls(
                    status=ResponseStatus.ERROR,
                    result=OptimizationStartPostResponseResult.OPTIMIZATION_START_POST_DATA_NOT_PROVIDED_400,
                )
                response = OptimizationStartPostResponse.from_orm(response_orm)
                response_code = status.HTTP_400_BAD_REQUEST
                if gnb_dict:
                    response_orm = OptimizationStartPostResponseCls(
                        status=ResponseStatus.SUCCESS,
                        result=OptimizationStartPostResponseResult.OPTIMIZATION_START_POST_START_SUCCESS_200,
                    )
                    response = OptimizationStartPostResponse.from_orm(response_orm)
                    response_code = status.HTTP_200_OK
                    current_optimization.update(
                        status=ResponseStatus.OPTIMIZATION_IN_QUEUE
                    )
                    start_optimization_thread(
                        gnb_dict,
                        unwanted_bb_links,
                        mandatory_bb_links,
                        current_optimization,
                    )
    else:
        response_orm = OptimizationStartPostResponseCls(
            status=ResponseStatus.OPTIMIZATION_INVALID_ID,
            result=OptimizationStartPostResponseResult.OPTIMIZATION_START_POST_INVALID_ID_404,
        )
        response = OptimizationStartPostResponse.from_orm(response_orm)
    return response, response_code


def stop_ongoing_optimization(optimization_id):
    existing_thread_opt_id = optimization_threading_helper.task_opt_id
    if optimization_id != existing_thread_opt_id:
        # Updating the status of the running optimization instance to Interrupted
        if (
            optimization_threading_helper.task_thread.is_alive()
            and existing_thread_opt_id != "None"
        ):
            already_running_optimization = database_service.get_optimization_by_id(
                existing_thread_opt_id, "Optimization"
            )
            already_running_optimization.update(
                status=ResponseStatus.OPTIMIZATION_FINISHED,
                optimization_end_date=datetime.datetime.now(),
            )
            # Stop the running optimization for the specific optimization instance
            stop_optimization(existing_thread_opt_id)
        optimization_threading_helper.task_opt_id = optimization_id


def parse_start_optimization_body(target_gnbdus, restricted_links, mandatory_links):
    selected_gnb_output = {}
    if target_gnbdus:
        for bb in target_gnbdus:
            if "gnbdu_id" in bb:
                gnbdu = Gnbdus.objects.get(gnbdu_id=bb["gnbdu_id"])
                if "gnbdu_id" in gnbdu and "cm_handle" in gnbdu:
                    selected_gnb_output[gnbdu["gnbdu_id"]] = gnbdu["cm_handle"]
    logger.debug(
        "selected gnb output data from parser - {}".format(str(selected_gnb_output))
    )
    unwanted_gnb_pairs_output = parse_node_partner_constraints_list(restricted_links)
    mandatory_gnb_pairs_output = parse_node_partner_constraints_list(mandatory_links)
    return selected_gnb_output, unwanted_gnb_pairs_output, mandatory_gnb_pairs_output


def parse_node_partner_constraints_list(gnb_pairs_partner_name):
    gnb_pairs_list = []
    if gnb_pairs_partner_name:
        for bb_pair in gnb_pairs_partner_name:
            if "p_gnbdu_id" in bb_pair and "s_gnbdu_id" in bb_pair:
                gnb_pairs_list.append((bb_pair["p_gnbdu_id"], bb_pair["s_gnbdu_id"]))
    logger.debug(
        "selected {} output data from parser - {}".format(
            gnb_pairs_partner_name, str(gnb_pairs_list)
        )
    )
    return gnb_pairs_list


def start_optimization_thread(
    gnb_dict, unwanted_bb_pairs, mandatory_bb_links, current_optimization
):
    logger.debug("Optimization service starting ...")
    optimization_threading_helper.task_thread = threading.Thread(
        target=build_solution.run_optimization_service,
        args=(gnb_dict, unwanted_bb_pairs, mandatory_bb_links, current_optimization),
    )
    try:
        optimization_threading_helper.task_thread.start()
    except RuntimeError:
        logger.exception(
            "RuntimeError: Failed to start optimization thread - already running."
        )
    else:
        current_optimization.update(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            optimization_start_date=datetime.datetime.now(),
        )
        logger.debug("Optimization service started")


def stop_optimization(optimization_id: str):
    response_code = status.HTTP_404_NOT_FOUND
    if pymongoose.ObjectId.is_valid(optimization_id):
        response_orm = OptimizationStopPostResponseCls(
            status=ResponseStatus.ERROR,
            result=OptimizationStopPostResponseResult.OPTIMIZATION_STOP_POST_WRONG_ID,
        )
        current_optimization = database_service.get_optimization_by_id(
            optimization_id, "Optimization"
        )
        if (
            current_optimization is not None
            and optimization_id == optimization_threading_helper.task_opt_id
        ):
            optimization_threading_helper.task_stop_event.set()
            while optimization_threading_helper.task_thread.is_alive():
                time.sleep(0.1)
            optimization_threading_helper.task_stop_event.clear()
            response_orm = OptimizationStopPostResponseCls(
                status=ResponseStatus.SUCCESS,
                result=OptimizationStopPostResponseResult.OPTIMIZATION_STOP_POST_STOPPED_SUCCESS,
            )
            response_code = status.HTTP_200_OK
            optimization_threading_helper.task_opt_id = "None"
            current_optimization.update(
                status=ResponseStatus.OPTIMIZATION_FINISHED,
                optimization_end_date=datetime.datetime.now(),
            )
    else:
        response_orm = OptimizationStatusGetResponseCls(
            status=ResponseStatus.OPTIMIZATION_INVALID_ID,
            result=[],
        )
    return OptimizationStopPostResponse.from_orm(response_orm), response_code


def get_optimization_status(optimization_id: str):
    response_code = status.HTTP_404_NOT_FOUND
    if pymongoose.ObjectId.is_valid(optimization_id):
        response_orm = OptimizationStatusGetResponseCls(
            status=ResponseStatus.OPTIMIZATION_WRONG_ID,
            result=[],
        )
        current_optimization = database_service.get_optimization_by_id(
            optimization_id, "Optimization"
        )
        if current_optimization:
            result_links = current_optimization.result_links
            response = []
            for bb in result_links:
                response.append(bb.as_dict())
            response_orm = OptimizationStatusGetResponseCls(
                status=current_optimization.status, result=response
            )
            if (
                response_orm.status == ResponseStatus.ERROR
                or response_orm.status == ResponseStatus.OPTIMIZATION_UNEXPECTED_ERROR
            ):
                response_code = status.HTTP_500_INTERNAL_SERVER_ERROR
            else:
                response_code = status.HTTP_200_OK
    else:
        response_orm = OptimizationStatusGetResponseCls(
            status=ResponseStatus.OPTIMIZATION_INVALID_ID,
            result=[],
        )
    return OptimizationStatusGetResponse.from_orm(response_orm), response_code


def create_configuration_instance(request_body_as_json):
    global configuration_instance_id_counter
    response_code = status.HTTP_200_OK
    response_orm = PartnerConfigurationCreateInstancePostResponseCls(
        status=ResponseStatus.SUCCESS,
        configuration_id=configuration_instance_id_counter,
        result=PartnerConfigurationCreateInstancePostResponseResult.CREATED_SUCCESS_200,
    )
    response = PartnerConfigurationCreateInstancePostResponse.from_orm(response_orm)
    configuration_instances_with_ids_and_data[
        configuration_instance_id_counter
    ] = request_body_as_json
    configuration_instance_id_counter += 1
    return response, response_code


def get_configuration_status(configuration_id: int):
    response_code = status.HTTP_404_NOT_FOUND
    response_orm = ConfigurationStatusGetResponseCls(
        status=ResponseStatus.ERROR,
        message=ConfigurationStatusGetResponseMessage.CONFIGURATION_STATUS_GET_WRONG_ID,
    )
    if configuration_id >= 0:
        configuration_data = configuration_instances_with_ids_and_data.get(
            configuration_id
        )
        if configuration_data is not None:
            response_orm = ConfigurationStatusGetResponseCls(
                status=ResponseStatus.SUCCESS,
                message=ConfigurationStatusGetResponseMessage.CONFIGURATION_STATUS_ID_FOUND,
            )
            if response_orm.status == ResponseStatus.ERROR:
                response_code = status.HTTP_500_INTERNAL_SERVER_ERROR
            else:
                response_code = status.HTTP_200_OK
    return ConfigurationStatusGetResponse.from_orm(response_orm), response_code


# EOF
