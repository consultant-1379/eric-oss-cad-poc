#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Configuration - Build the BB configuration
# File with routines for building the best BB configuration from the usability matrix, called from build_solution.py
# Functionality and script layout:
#   - Read the usability matrix
#   - Create a BB configuration data structure
#   - Aggregate the usability per link for all BB-BB uni-directional links
#   - Sort the links in decreasing usability order
#   - Add links to BBs in usability order
#   - If any BB has reached maximum allowed partners per direction, indicate this in link list, skip link
#   - If any BB has reached maximum allowed external cells, indicate this in link list, skip link
#   - Create and return the final usability matrix
import datetime
import logging

import numpy as np
import pandas as pd
from entities.api_response import ResponseStatus

"""
Summary: Builds PS cell pair value list:
Description:
        - Prepare the PS cells pair list from the dfusability:
        - Remove all rows with no usability
        - Add column with tuple of (primary_gnb, secondary_gnb) to be used for aggregating to BB-BB links
        - Remove rows with PeNB = SeNB, but keep external PS pairs
        - Sort the table according to primary_gnb to prepare for BB-BB link aggregation
params:
    df() :
"""

logger = logging.getLogger(__name__)


def improve_ps_cell_value_list(df):
    logger.debug("Total PS cell pairs: {}".format(len(df)))
    # Remove zero usability rows:
    ps_cell_value_list = df[df["usability"] != 0].copy()
    logger.debug(
        "Total PS cell pairs with valid usability: {}".format(len(ps_cell_value_list))
    )
    # Add tuple with (primary_gnb, secondary_gnb):
    ps_cell_value_list["gNbs"] = list(
        zip(ps_cell_value_list["primary_gnb"], ps_cell_value_list["secondary_gnb"])
    )
    # Remove rows with cells in the same gNB - will also remove rows where pCell == sCell:
    ps_cell_value_list_new = (
        ps_cell_value_list[
            ps_cell_value_list["primary_gnb"] != ps_cell_value_list["secondary_gnb"]
        ]
    ).copy()
    logger.debug(
        "Total PS cell pairs with valid usability and on different gNBs: {}".format(
            len(ps_cell_value_list_new)
        )
    )
    # Sort according to primary_gnb:
    ps_cell_value_list_new.sort_values("primary_gnb", inplace=True)
    return ps_cell_value_list_new


"""
Summary: Creates a BB-BB link list with aggregated usability
Description:
        - From the P/S cell pair list, aggregate to BB-BB connections:
        - Sort the gNBs in the pairs, maintaining the direction
        - Sort the entire table on the primary_gnb-secondary_gnb pairs and usability
        - Add a column for indicating if maximum number of partners is reached, meaning PS pair is not available
        - Group per gNB pair and aggregate the link value
params:
    ps_cell_value_list() :
    max_external_cells_secondary_gnb() :
"""


def create_bb_link_value_list(df_usability, max_external_cells_secondary_gnb):
    # Create a new data frame from ps_cell_value_list
    ps_cell_value_list = improve_ps_cell_value_list(df_usability)
    bb_link_value_list = ps_cell_value_list.copy()
    # Sort the entire data frame based on the columns of gNBs and the usability
    bb_link_value_list.sort_values(
        ["primary_gnb", "secondary_gnb", "usability"], inplace=True, ascending=False
    )
    # Add column with cumulative number of primary_gnb and secondary_gnb pairs
    bb_link_value_list["externalCellsOnsecondary_gnb"] = bb_link_value_list.groupby(
        ["primary_gnb", "secondary_gnb"]
    ).cumcount()
    # Only include rows that not violates the max # of external cells on secondary_gnb from primary_gnb
    bb_link_value_list = bb_link_value_list.query(
        "externalCellsOnsecondary_gnb < @max_external_cells_secondary_gnb"
    ).copy()
    # Aggregate usability value per eNB pair and sort the list in descending usability order
    bb_link_value_list = (
        bb_link_value_list[["gNbs", "usability"]]
        .groupby("gNbs")
        .agg({"usability": "sum"})
        .sort_values("usability", ascending=False)
    )
    # Set numerical index
    bb_link_value_list.reset_index(inplace=True)
    # Add columns with the first and second eNb per link
    bb_link_value_list["gNb0"] = bb_link_value_list["gNbs"].map(lambda x: x[0])
    bb_link_value_list["gNb1"] = bb_link_value_list["gNbs"].map(lambda x: x[1])
    # Add column indicating if link is used in configuration
    bb_link_value_list["linkUsed"] = False
    return bb_link_value_list


"""
Summary:  Get unique gNb list from bb_link_value_list
Description:
        - Creates a list with all gNBs
params:
    bb_link_value_list([]) : BB-BB link list with aggregated usability
"""


def get_unique_gnb_list(bb_link_value_list):
    if bb_link_value_list.empty:
        unique_gnbs = []
    else:
        unique_gnbs = sorted(
            list(set(bb_link_value_list["gNbs"].apply(lambda x: [x[0], x[1]]).sum()))
        )
    return unique_gnbs


"""
Summary:  Assigns links to the BB units
Description:
        - Runs through the greedy algorithm, one BB link at a time in the bb_link_value_list
        - Only updates the 'linkUsed' column in bb_link_value_list
        - For each link:
            - Checks limit for both BBs in a BB unit pair, if ok set 'linkUsed' to True in bb_link_value_list
params:
    unique_gnbs([]) : list of unique gNbs in the bb_link_value_list
    bb_link_value_list(DataFrame)) : BB-BB link list with aggregated usability
    max_bb_partners() : limit of how many in/out links a gNb can have
"""


def assign_bb_links(bb_link_value_list, max_bb_partners):
    def assign_bb_links_inner(gnbs, gnb0, gnb1, max_bb_partners):
        # Create arrays initialized with zero links per gNB
        links_per_gnb_to_sec_col = np.zeros(len(gnbs))
        links_per_gnb_to_prim_col = np.zeros(len(gnbs))
        # Create array initialized to False for every link usage
        link_used_col = np.full(len(gnb0), False)
        # Go through the gNB pairs (= link) and see if it is assignable
        for link in range(len(link_used_col)):
            # Check if gNb0 and gNb1 both has fewer links than allowed limit
            index_0 = np.where(gnbs == gnb0[link])[0]
            index_1 = np.where(gnbs == gnb1[link])[0]
            if (links_per_gnb_to_sec_col[index_0][0] < max_bb_partners) and (
                links_per_gnb_to_prim_col[index_1][0] < max_bb_partners
            ):
                # Both gNBs have available links, set link to be used and increment number of links
                link_used_col[link] = True
                links_per_gnb_to_sec_col[index_0] += 1
                links_per_gnb_to_prim_col[index_1] += 1
        return link_used_col

    unique_gnbs = get_unique_gnb_list(bb_link_value_list)
    bb_link_value_list.loc[:, "linkUsed"] = assign_bb_links_inner(
        np.array(unique_gnbs),
        bb_link_value_list["gNb0"].values,
        bb_link_value_list["gNb1"].values,
        max_bb_partners,
    ).tolist()

    return bb_link_value_list


"""
Summary:  Builds a simpler list of the resulting BB-links to be set up
Description:
    - This will be sent back to the Flow as a result, and also used to set the correct attributes to set up the links
    - The list will contain pairs (gNb0 and gNb1) of baseband units that should be connected with an E5 link,
      their corresponding cmHandles and usability score
Params:
    bb_link_value_list(DataFrame(columns=["gNbs",
                                          "usability",
                                          "gNb0",
                                          "gNb1",
                                          "linkUsed"])) : DataFrame containing BB links
    bb_dict({gNbId: "cmHandle"}) : dictionary containing gNbIds and their corresponding cmHandles
"""


def get_bb_link_list(bb_link_value_list, bb_dict):
    logger.debug("BB link value list : %s ", bb_link_value_list.to_string())
    bb_link_list = bb_link_value_list.query("linkUsed == True")[
        ["gNb0", "gNb1", "usability"]
    ]
    bb_link_list.reset_index(drop=True, inplace=True)
    return bb_link_list


"""
Summary:  Remove the unwanted BB-links from the BB-links list
Description:
    - Filter the unwanted node pairs from the BB-links list
Params:
    bb_link_value_list(DataFrame(columns=["gNbs",
                                          "usability",
                                          "gNb0",
                                          "gNb1",
                                          "linkUsed"])) : DataFrame containing BB links
    unwanted_bb_links([]) : BB-BB link list
"""


def check_unwanted_bb_link(bb_link_value_list, unwanted_bb_links):
    filtered_bb_link_list = bb_link_value_list[
        ~bb_link_value_list.gNbs.isin(unwanted_bb_links)
    ].copy()
    filtered_bb_link_list.reset_index(drop=True, inplace=True)
    return filtered_bb_link_list


"""
Summary: Add the mandatory BB-links to the filtered BB-links list
Description:
    - Add the mandatory node pairs to the BB-links list if it doesn't exist
    - Set the linkUsed column for the mandatory links to True
Params:
    filtered_bb_link_value_list(DataFrame)) : BB-BB link list with aggregated usability
                                              after removing unwanted links
    mandatory_bb_links : BB-BB mandatory link list
"""


def add_mandatory_bb_links(bb_link_value_list, mandatory_bb_links):
    for bb in mandatory_bb_links:
        if bb in set(bb_link_value_list["gNbs"]):
            bb_link_value_list.loc[bb_link_value_list["gNbs"] == bb, "linkUsed"] = True
        else:
            bb_link_value_list.loc[len(bb_link_value_list.index)] = [
                bb,
                0,
                bb[0],
                bb[1],
                True,
            ]
    sorted_bb_link = bb_link_value_list.sort_values(
        ["linkUsed", "usability"], ascending=False
    )
    sorted_bb_link.reset_index(drop=True, inplace=True)
    return sorted_bb_link


"""
Summary: Save resulted BB links after optimization to the database
Description:
    - Build the final response list
    - Save the bb links list to the database
Params:
    bb_link_list(DataFrame)) : BB-BB link list with aggregated usability
                                              after removing unwanted links
    document : collection model document
"""


def save_bb_links_result(bb_link_list, document):
    logger.debug("Resulted BB link list : %s ", bb_link_list.to_string())
    response = []
    for i in range(len(bb_link_list["gNb0"])):
        response.append(
            {
                "p_gnbdu_id": int(bb_link_list["gNb0"][i]),
                "s_gnbdu_id": int(bb_link_list["gNb1"][i]),
                "usability": bb_link_list["usability"][i],
            }
        )
    document.update(
        status=ResponseStatus.OPTIMIZATION_FINISHED,
        result_links=response,
        optimization_end_date=datetime.datetime.now(),
    )
    return response


"""
Summary: Check coverage data and mandatory links
Description:
    - Check if coverage data and mandatory links are empty return empty dataframe
    - Check if coverage data is empty and mandatory links isn't empty return dataframe filled with mandatory links
Params:
    raw_coverage_data : dataframe contains data from NCMP
    mandatory_bb_links : BB-BB mandatory pair of nodes list
    document : collection model document
"""


def check_coverage_data_and_mandatory_links(
    raw_coverage_data, mandatory_bb_links, document
):
    mandatory_link_value_df = pd.DataFrame()
    if raw_coverage_data.empty and len(mandatory_bb_links) == 0:
        document.update(
            status=ResponseStatus.OPTIMIZATION_FINISHED,
            optimization_end_date=datetime.datetime.now(),
        )
        logger.info("No data returned from NCMP - optimization finished prematurely")
    if raw_coverage_data.empty and len(mandatory_bb_links) != 0:
        df = pd.DataFrame(
            columns=["gNbs", "usability", "gNb0", "gNb1", "linkUsed"], dtype=np.int64
        )
        mandatory_link_value_df = add_mandatory_bb_links(df, mandatory_bb_links)
    return mandatory_link_value_df


# EOF
