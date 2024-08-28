#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Configuration - Build the capability table
# File with routines for building the matrix of gross capability levels for a PCell/SCell pair before limits etc,
# called from build_solution.py
# Functionality and script layout:
#   - Use the coverage data to build a list of pCell-sCell pairs
#   - To build a rich level of capability per cell pair, multiply the coverage weight with the value for predicted
#     available capacity per sCell
#   - Return the final capability matrix
import logging
from entities.api_response import ResponseStatus

"""
Summary:  Build and return the final capability matrix for all P/S cell pairs
Description:
        - The capability_matrix is created by multiplying the coverage in dfCoverageData
        by the predicted capacity column in
        dfCapacityPred (scaling the coverage per cell with the corresponding capacity)
        - The key is the sCell ID, sNCI, to keep the table showing the capability
        for a specific sCell to be used from a specific pCell
        - Since the optimization is uni-directional each sCell value per a pCell is valid
        - For sCells with missing predicted capacity, the capability will be 0
            - This can be a topic for improvement - use previous value, average value, or indicate in log?
params:
    coverage_data() : A data frame that holds pNCI, sNCI, coded_usefulness, hitrate, primary_gnb, secondary_gnb data
    capacity_data() : A data frame that holds pmMacRBSymAvailDl, pmMacRBSymUsedPdschTypeA,
     sNCI, RBSymFree, secondary_gnb, RBSymFreeNorm, predictedCapacity data
""" ""
logger = logging.getLogger(__name__)


def build_capability_matrix(coverage_data, capacity_data, document):
    document.update(status=ResponseStatus.OPTIMIZATION_BUILDING_CAPABILITY)
    logger.info(
        "Optimization status msg : {}".format(
            ResponseStatus.OPTIMIZATION_BUILDING_CAPABILITY
        )
    )
    capability_matrix = coverage_data.copy()
    # Get predicted capacity for matching sNCI
    capability_matrix = capability_matrix.merge(capacity_data, how="left", on="sNCI")
    # Change NaN to 0, if any
    capability_matrix["predictedCapacity"].fillna(0, inplace=True)
    # Multiply hit rate with predicted capacity
    capability_matrix["capability"] = (
        capability_matrix["hitrate"] * capability_matrix["predictedCapacity"]
    )
    # Clean up the resulting data frame
    capability_matrix["secondary_gnb"] = capability_matrix["secondary_gnb_x"]
    capability_matrix.drop(
        columns=[
            "coded_usefulness",
            "hitrate",
            "pmMacRBSymAvailDl",
            "pmMacRBSymUsedPdschTypeA",
            "RBSymFree",
            "secondary_gnb_y",
            "secondary_gnb_x",
            "RBSymFreeNorm",
            "predictedCapacity",
        ],
        inplace=True,
    )
    return capability_matrix


# EOF
