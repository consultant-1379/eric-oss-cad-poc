#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Automation - Build Solution
# Main execution file for building the complete ERAN configuration from PM, CM and CTR data
# Functionality and script layout:
#   - Set up environment
#   - Read data
#   - Create data frames
#   - Aggregate and filter to get usability matrix
#   - Build complete BB configuration - greedy algorithm
#   - Loop over random alterations
#   - Evaluate and pick best solution
#
# Scrap area with ideas and things to do:
#  - Build REST handling
#    - Add APIs (functions in separate files) for input and output
#    - Add concurrency with background task (= runOptimization())
#  - Containerize
#  - Read limits from source behind API (function call)
#  - Get new NR data from ITK, EPP or RDI
import datetime
import logging

from entities.api_response import ResponseStatus
from helper import optimization_threading_helper
from helper.optimization_threading_helper import stop_event_is_set
from optimization import (
    bb_configuration,
    capability_matrix,
    definitions,
    evaluate,
    get_data,
    report,
    usability_matrix,
)

"""
Summary: Exception wrapper for run_optimization
Description:
        - Calls run_optimization and checks if any unhandled exception is raised
params:
    bb_dict({gnb_id: "cm_handle")}) : dictionary containing gNbIds and corresponding cmHandles
    unwanted_bb_links([(p_gnbdu_id, s_gnbdu_id)]) : list containing primary and secondary gNbId tuples
    mandatory_bb_links([(p_gnbdu_id, s_gnbdu_id)]) : list containing mandatory primary and secondary gNbId tuples
"""
logger = logging.getLogger(__name__)


def run_optimization_service(bb_dict, unwanted_bb_links, mandatory_bb_links, document):
    try:
        run_optimization(bb_dict, unwanted_bb_links, mandatory_bb_links, document)
    except Exception:
        logger.exception(
            "Exception caught while executing optimization ID: {}".format(
                optimization_threading_helper.task_opt_id
            )
        )
        document.update(status=ResponseStatus.OPTIMIZATION_UNEXPECTED_ERROR)


"""
Summary: Builds a BB Optimization
Description:
        - Builds and evaluates a solution
params:
    bb_dict({gnb_id: "cm_handle")}) : dictionary containing gNbIds and corresponding cmHandles
    unwanted_bb_links([(p_gnbdu_id, s_gnbdu_id)]) : list containing unwanted primary and secondary gNbId tuples
    mandatory_bb_links([(p_gnbdu_id, s_gnbdu_id)]) : list containing mandatory primary and secondary gNbId tuples
"""


def run_optimization(bb_dict, unwanted_bb_links, mandatory_bb_links, document):
    df_coverage_data = get_data.get_coverage_data(bb_dict, document)
    bb_link_df = bb_configuration.check_coverage_data_and_mandatory_links(
        df_coverage_data, mandatory_bb_links, document
    )
    if df_coverage_data.empty and bb_link_df.empty:
        return
    if not df_coverage_data.empty:
        df_capacity_pred = get_data.get_predicted_capacity(document)
        if stop_event_is_set(document):
            return
        logger.info(ResponseStatus.OPTIMIZATION_BUILDING_CAPABILITY)
        df_capability = capability_matrix.build_capability_matrix(
            df_coverage_data, df_capacity_pred, document
        )
        if stop_event_is_set(document):
            return
        df_usability = usability_matrix.build_usability_matrix(
            df_capability, definitions.CAPABILITY_LIMIT, document
        )
        if stop_event_is_set(document):
            return
        logger.info(ResponseStatus.OPTIMIZATION_BUILDING_BB_CONFIGURATIONS)
        bb_link_value_list = bb_configuration.create_bb_link_value_list(
            df_usability, definitions.MAX_EXTERNAL_CELLS_SECONDARY_GNB
        )
        filtered_bb_link_value_list = bb_configuration.check_unwanted_bb_link(
            bb_link_value_list, unwanted_bb_links
        )
        bb_link_df = bb_configuration.add_mandatory_bb_links(
            filtered_bb_link_value_list, mandatory_bb_links
        )
        if bb_link_df.empty:
            document.update(
                status=ResponseStatus.OPTIMIZATION_FINISHED,
                optimization_end_date=datetime.datetime.now(),
            )
            return
    bb_link_resulted_df = bb_configuration.assign_bb_links(
        bb_link_df, definitions.MAX_BB_PARTNERS
    )
    bb_link_used = bb_configuration.get_bb_link_list(bb_link_resulted_df, bb_dict)
    if stop_event_is_set(document):
        return
    bb_configuration.save_bb_links_result(bb_link_used, document)
    total_arc_value = evaluate.calculate_arc_value(bb_link_resulted_df)
    report.save_in_bucket(bb_link_df, mandatory_bb_links, optimization_threading_helper.task_opt_id)
    logger.debug("Total ARC value: {:.3f}".format(total_arc_value))

# EOF
