#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC_CommonFunctions - Common Functions for ARC Automation
# Definitions of common functions for the ARC Automation
# Functionality:
#   - This file holds only separate functions, called from other modules in the ARC Automation package


import logging

"""
Summary: Disguises the coverage data in CM
Description:
        - The Usefulness, given as a normalized float 0..1, will be disguised in the CM data.
        - A temporary formula for this is as follows: (has to be reversed in the rApp to decode the information
        and will be updated once the CM attribute format is finalized:
            - cUfn = (nUfn ^ k) / k
            - nUfn = (cUfn * k) ^ (1 / (k - 1))
            - where:
            - cUfn: Coded Usefulness, float
            - nUfn: Normalized Usefulness, float 0..1
            - k: Constant, set to 0.2 for now
params:
    coded_usefulness() :
"""
logger = logging.getLogger(__name__)


def decoded_usefulness(coded_usefulness):
    # Sets the constant use (basically only a scaling):
    k = 0.2
    return (coded_usefulness / k) ** (1 / (k - 1))


"""
Summary: Cell ID conversion
Description:
        - Extract the gNB ID from the NCI number
params:
    nci() :
    digits_for_cell_id() :
"""


def gnb_id_from_nci(nci, digits_for_cell_id):
    return int(nci / (16**digits_for_cell_id))


# EOF
