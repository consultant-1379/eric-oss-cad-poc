#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Configuration - Build the usability matrix
# File with routines for building the matrix of benefit levels for a PCell/SCell pair, called from build_solution.py
# Functionality and script layout:
#   - Find any limits, conditions or weights that should be taken into account
#   - Create and return the final usability matrix

from entities.api_response import ResponseStatus

"""
Summary: Build final usability matrix for all P/S cell pairs
Description:
        -  Take limits, conditions and weights into account to create the resulting usability matrix:
        - Should any cells be excluded?
        - Initial version:
            - Only the capability limit is checked
            - Values below the limit is set to zero
            - Previous versions normalized the result - here this is skipped
              to enhance compatibility between configurations
                - Unclear: The hit rate is an absolute value, but
                the capacity is relative in its measured configuration
params:
    capability_matrix([]) : A data frame that holds pNCI, sNCI, primary_gnb, capability, secondary_gnb data arrays.
    capability_limit(double) : A usability limit value
""" ""


def build_usability_matrix(capability_matrix, capability_limit, document):
    document.update(status=ResponseStatus.OPTIMIZATION_BUILDING_USABILITY)
    usability_matrix = capability_matrix.copy()
    usability_matrix["usability"] = usability_matrix["capability"]
    usability_matrix["usability"].where(
        usability_matrix["capability"] > capability_limit, other=0, inplace=True
    )
    return usability_matrix


# EOF
