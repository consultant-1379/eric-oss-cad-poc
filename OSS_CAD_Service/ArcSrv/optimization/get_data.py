#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022

# ARC Configuration - Get Data
# File with routines for getting the ERAN data from PM and CM
# Functionality and script layout:
#   - Read data
#   - Create data frames
#   - Return the data frames'

import json
import logging

import numpy as np
import pandas as pd
import requests
from entities.api_response import ResponseStatus
from optimization import common_functions, definitions, predict_cell_capability

OPTIMIZATION_STATUS_MSG = "Optimization status msg : {}"
logger = logging.getLogger(__name__)

"""
Summary: Query data from NCMPService
Description:
        - Function to deserialize the CM data obtained from NCMPService API
params:
    bb_dict({gnb_id: "cm_handle"}) : dictionary containing gNbIds and corresponding cmHandles
"""


# TODO: Needs to be modified when compose/integrate with NCMP service.
def get_raw_coverage_data_ncmp(bb_dict, document):
    document.update(status=ResponseStatus.OPTIMIZATION_COLLECTING_CM_DATA)
    logger.debug("Read data from NCMP")
    request_data = {"gnb_id_list": list(bb_dict)}
    # Make sure NCMPService mockup container is running ..
    logger.debug("Raw NCMP request data: %s", json.dumps(request_data))
    # response = requests.request(method="post", url=definitions.NCMP_CM_URL, data=json.dumps(request_data))
    # df_result = response.json()
    df_result = {"gNBwithResult": []}
    logger.debug("NCMP response received!")

    df_total = pd.DataFrame([{
        "pNCI": row["nci"],
        "sNCI": usefullness["nci"],
        "coded_usefulness": usefullness["CoverageRate"]
    }
        for gnb_cm in df_result["gNBwithResult"]
        for row in gnb_cm["nrCells"]
        for usefullness in row["cellUsefullness"]
    ], dtype=np.int64, columns=["pNCI", "sNCI", "coded_usefulness"])

    logger.debug("Read data from NCMP, Done!")
    return df_total.reset_index(drop=True)


"""
Summary: Prepare the coverage data for usage in the optimization
Description:
    - Convert the coded coverage data to original hit rate value
    - Add the gNB IDs for both nodes in the coverage data
    - Remove the rows that contains possible links with non selected gNBs (not present in initial bb list)
params:
    raw_coverage_data([]): A data frame that holds pNCI, sNCI, coded_usefulness data arrays.
    digits_for_cell_id(int): An integer value that holds the length of a Cell Id
    initial_bb_list([]): A List that contains selected gNBs Ids

"""


def get_clean_coverage_data(raw_coverage_data, digits_for_cell_id, initial_bb_list):
    logger.debug("Coverage data from NCMP shape : %s", raw_coverage_data.shape)
    df_coverage_data = raw_coverage_data.copy()
    df_coverage_data["hitrate"] = [
        common_functions.decoded_usefulness(val)
        for val in df_coverage_data["coded_usefulness"]
    ]
    df_coverage_data["primary_gnb"] = [
        common_functions.gnb_id_from_nci(nci, digits_for_cell_id)
        for nci in df_coverage_data["pNCI"]
    ]
    df_coverage_data["secondary_gnb"] = [
        common_functions.gnb_id_from_nci(nci, digits_for_cell_id)
        for nci in df_coverage_data["sNCI"]
    ]

    # Keep only rows that contains secondary_gnb that is present on the initial bbList
    df_coverage_data = df_coverage_data[
        df_coverage_data["secondary_gnb"].isin(initial_bb_list)
    ]
    logger.debug("Clean coverage data shape : %s ", df_coverage_data.shape)
    return df_coverage_data


"""
Summary: Prepare the capacity data for usage in the optimization
Description:
    - Add column with free capacity
    - Add gNB ID for the host of the sCell in the table, to enable matching with other cells on  the same node
params:
    raw_capacity_data([]): A data frame that holds pmMacRBSymAvailDl, pmMacRBSymUsedPdschTypeA, sNCI data arrays.
    digits_for_cell_id(int): An integer value that holds the length of a Cell Id
"""


def get_clean_capacity_data(raw_capacity_data, digits_for_cell_id):
    df_capacity_data = raw_capacity_data.copy()
    df_capacity_data["RBSymFree"] = (
        df_capacity_data["pmMacRBSymAvailDl"]
        - df_capacity_data["pmMacRBSymUsedPdschTypeA"]
    )
    df_capacity_data["secondary_gnb"] = [
        common_functions.gnb_id_from_nci(nci, digits_for_cell_id)
        for nci in df_capacity_data["sNCI"]
    ]
    return df_capacity_data


"""
Summary: Calculate predicted capacity
Description:
    - Update optimization response status
    - Get capacity from pm handler
    - Get clean pm data
params:
    document : collection model document
"""


def get_predicted_capacity(document):
    document.update(status=ResponseStatus.OPTIMIZATION_COLLECTING_PM_DATA)
    logger.info(
        OPTIMIZATION_STATUS_MSG.format(ResponseStatus.OPTIMIZATION_COLLECTING_PM_DATA)
    )
    logger.debug("Read PM data.")
    raw_capacity_data = pd.read_csv(definitions.PM_DATA_FILE, sep=",", index_col=0)
    logger.debug("Capacity data from PM handler shape : %s", raw_capacity_data.shape)
    logger.debug("Read raw PM data done!")
    df_capacity_data = get_clean_capacity_data(
        raw_capacity_data, definitions.DIGITS_FOR_CELL_ID
    )
    logger.debug("PM data cleaning done!")
    document.update(status=ResponseStatus.OPTIMIZATION_PREDICTING_CELL_CAPABILITIES)
    logger.info(ResponseStatus.OPTIMIZATION_PREDICTING_CELL_CAPABILITIES)
    return predict_cell_capability.add_predicted_cell_capacity(
        definitions.MODEL_FILENAME,
        definitions.MODEL_SEPARATOR,
        df_capacity_data,
    )


"""
Summary: Get coverage data
Description:
    - Get raw coverage data from ncmp
    - Clean coverage data if it is not empty
params:
    bb_dict({gnb_id: "cm_handle")}) : dictionary containing gNbIds and corresponding cmHandles
    document : collection model document
"""


def get_coverage_data(bb_dict, document):
    df_coverage_data = pd.DataFrame()
    logger.info(ResponseStatus.OPTIMIZATION_COLLECTING_CM_DATA)
    raw_coverage_data = get_raw_coverage_data_ncmp(bb_dict, document)
    if not raw_coverage_data.empty:
        df_coverage_data = get_clean_coverage_data(
            raw_coverage_data,
            definitions.DIGITS_FOR_CELL_ID,
            list(map(int, list(bb_dict))),
        )
    return df_coverage_data


# EOF
