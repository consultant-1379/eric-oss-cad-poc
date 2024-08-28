#!/usr/bin/env python

# Test file for bb_configuration

# Copyright Ericsson (c) 2022
import unittest
from unittest.mock import patch

import pandas as pd
from entities import database_collection_models
from entities.database_collection_models import Optimization
from optimization import bb_configuration


class TestBbConfiguration(unittest.TestCase):
    test_ps_cell_value_list = pd.DataFrame(
        data={
            "pNCI": [53588739, 53588737, 53588738, 53589848, 53588741],
            "sNCI": [53510147, 53510146, 53434115, 53510672, 53510149],
            "primary_gnb": [209331, 209331, 209331, 209335, 209331],
            "capability": [0.133, 0.3, 0.12, 0.2, 0.34],
            "secondary_gnb": [209024, 209024, 208727, 209026, 209024],
            "usability": [0.133, 0.3, 0.12, 0.2, 0.34],
            "gNbs": [
                (209331, 209024),
                (209331, 209024),
                (209331, 208727),
                (209335, 209026),
                (209331, 209024),
            ],
        }
    )
    test_unique_gnb_list = [208727, 209024, 209026, 209331, 209335]
    test_bb_link_value_list = pd.DataFrame(
        data={
            "gNbs": [(209331, 209024), (209335, 209026), (209331, 208727)],
            "usability": [0.64, 0.2, 0.12],
            "gNb0": [209331, 209335, 209331],
            "gNb1": [209024, 209026, 208727],
            "linkUsed": 3 * [False],
        }
    )
    test_mandatory_bb_links = [(209331, 209024), (209335, 209027), (209331, 208727)]
    test_updated_bb_link_value_list = test_bb_link_value_list.copy()
    test_updated_bb_link_value_list["linkUsed"] = pd.Series([True, False, True])
    expected_save_bb_links_response = [
        {"p_gnbdu_id": 209331, "s_gnbdu_id": 209024, "usability": 0.433},
        {"p_gnbdu_id": 209335, "s_gnbdu_id": 209024, "usability": 0.2},
        {"p_gnbdu_id": 209331, "s_gnbdu_id": 208727, "usability": 0.12},
        {"p_gnbdu_id": 209026, "s_gnbdu_id": 209024, "usability": 0.23},
        {"p_gnbdu_id": 209331, "s_gnbdu_id": 209026, "usability": 0.34},
    ]

    def setUp(self):
        self.test_bb_link_value_list_expanded = pd.DataFrame(
            data={
                "gNbs": [
                    (209331, 209024),
                    (209335, 209024),
                    (209331, 208727),
                    (209026, 209024),
                    (209331, 209026),
                ],
                "usability": [0.433, 0.2, 0.12, 0.23, 0.34],
                "gNb0": [209331, 209335, 209331, 209026, 209331],
                "gNb1": [209024, 209024, 208727, 209024, 209026],
                "linkUsed": 5 * [False],
            }
        )

    """
    GIVEN valid usability matrix
    WHEN it contains rows with zero usability
    AND it contains rows with equal primary_gnb and secondary_gnb
    THEN PS cell value list is returned
    """

    @patch.object(bb_configuration.logger, "debug")
    def test_improve_ps_cell_value_list(self, mocked_logging):
        test_usability_matrix = pd.DataFrame(
            data={
                "pNCI": [53588737, 53589848, 53588738, 53588738],
                "sNCI": [53510146, 53510672, 53434115, 53588736],
                "primary_gnb": [209331, 209335, 209331, 209331],
                "capability": [0.00048, 0.2, 0.133, 0.4],
                "secondary_gnb": [209024, 209026, 208727, 209331],
                "usability": [0, 0.2, 0.133, 0.4],
            }
        )
        expected_ps_cell_value_list = pd.DataFrame(
            data={
                "pNCI": [53588738, 53589848],
                "sNCI": [53434115, 53510672],
                "primary_gnb": [209331, 209335],
                "capability": [0.133, 0.2],
                "secondary_gnb": [208727, 209026],
                "usability": [0.133, 0.2],
                "gNbs": [(209331, 208727), (209335, 209026)],
            },
            index=pd.Index(data=[2, 1]),
        )
        action_output = bb_configuration.improve_ps_cell_value_list(
            test_usability_matrix
        )
        self.assertEqual(3, mocked_logging.call_count)
        pd.testing.assert_frame_equal(expected_ps_cell_value_list, action_output)

    """
    GIVEN valid PS cell value list
    AND limit of maximum number of external cells
    WHEN PS cell values list contains more cell links than limit allows
    THEN BbLink value list is created and returned
    """

    @patch.object(bb_configuration, "improve_ps_cell_value_list")
    def test_create_bb_link_value_list(self, mocked_improve_ps_cell_value_list):
        test_usability_matrix = pd.DataFrame(
            data={
                "pNCI": [53588739, 53588737, 53588738, 53589848, 53588741, 53588746],
                "sNCI": [53510147, 53510146, 53434115, 53510672, 53510149, 53510145],
                "primary_gnb": [209331, 209331, 209331, 209335, 209331, 209333],
                "capability": [0.133, 0.3, 0.12, 0.2, 0.34, 0.2],
                "secondary_gnb": [209024, 209024, 208727, 209026, 209024, 209333],
                "usability": [0.133, 0.3, 0.12, 0.2, 0.34, 0.2],
            }
        )
        mocked_improve_ps_cell_value_list.return_value = self.test_ps_cell_value_list
        test_max_external_cells_secondary_gnb = 2
        action_output = bb_configuration.create_bb_link_value_list(
            test_usability_matrix, test_max_external_cells_secondary_gnb
        )
        mocked_improve_ps_cell_value_list.assert_called_once_with(test_usability_matrix)
        pd.testing.assert_frame_equal(self.test_bb_link_value_list, action_output)

    """
    GIVEN valid PS cell value list with gNbs
    THEN return list of unique gNbs
    """

    def test_get_unique_gnb_list(self):
        action_output = bb_configuration.get_unique_gnb_list(
            self.test_bb_link_value_list
        )
        self.assertEqual(self.test_unique_gnb_list, action_output)

    """
    GIVEN empty PS cell value list
    THEN return empty list
    """

    def test_empty_links_per_gnb_returns_empty_data_frame(self):
        test_empty_ps_cell_value_list = pd.DataFrame(
            columns=[
                "pNCI",
                "sNCI",
                "primary_gnb",
                "capability",
                "secondary_gnb",
                "usability",
                "gNbs",
            ]
        )
        test_empty_unique_gnb_list = []
        action_output = bb_configuration.get_unique_gnb_list(
            test_empty_ps_cell_value_list
        )
        self.assertEqual(test_empty_unique_gnb_list, action_output)

    """
    GIVEN unique gNb list, gNb pairs with their usability and BB partner limit
    WHEN there are more gNb links than the limit allows
    THEN gNb pairs are marked as linked if they fit in the limit
    """

    @patch.object(bb_configuration, "get_unique_gnb_list")
    def test_assign_bb_links(self, mocked_get_unique_gnb_list):
        test_max_bb_partners = 2
        expected_link_used = [True, True, True, False, False]
        expected_bb_link_value_list = self.test_bb_link_value_list_expanded.copy()
        expected_bb_link_value_list["linkUsed"] = expected_link_used
        mocked_get_unique_gnb_list.return_value = [
            208727,
            209024,
            209026,
            209331,
            209335,
        ]
        action_output = bb_configuration.assign_bb_links(
            self.test_bb_link_value_list_expanded,
            test_max_bb_partners,
        )
        mocked_get_unique_gnb_list.assert_called_once()
        pd.testing.assert_frame_equal(expected_bb_link_value_list, action_output)

    """
    GIVEN valid BB link value list
    THEN gNb pairs for which a link is proposed are returned
    """

    @patch.object(bb_configuration.logger, "debug")
    def test_get_bb_link_list(self, mocked_logger):
        test_bb_dict = {209331: "12341", 209024: "12342", 208727: "12343"}
        expected_bb_link_list = pd.DataFrame(
            data={
                "gNb0": [209331, 209331],
                "gNb1": [209024, 208727],
                "usability": [0.64000, 0.12000],
            },
            index=pd.Index(data=[0, 1]),
        )
        action_output = bb_configuration.get_bb_link_list(
            self.test_updated_bb_link_value_list, test_bb_dict
        )
        mocked_logger.assert_called_once()
        pd.testing.assert_frame_equal(expected_bb_link_list, action_output)

    """
     GIVEN gNb pairs list and list of unwanted gNB pairs
     WHEN there are gNb links that exists in the unwanted gNB pairs
     THEN those gNb pairs are removed from gNB pairs list
     """

    def test_check_unwanted_bb_link(self):
        test_unwanted_bb_links = [(209331, 209024), (209335, 209027), (209331, 208727)]
        expected_filtered_bb_link_list = self.test_bb_link_value_list_expanded.drop(
            [0, 2]
        )
        expected_filtered_bb_link_list = expected_filtered_bb_link_list.reset_index(
            drop=True
        )
        action_output = bb_configuration.check_unwanted_bb_link(
            self.test_bb_link_value_list_expanded, test_unwanted_bb_links
        )
        pd.testing.assert_frame_equal(expected_filtered_bb_link_list, action_output)

    """
        GIVEN gNb pairs list and list of mandatory gNB pairs
        WHEN there is gNb link that doesn't exist in the mandatory gNB pairs
        THEN this link is added and linkUsed is set to True
        WHEN there is gNb link that exists in the mandatory gNB pairs
        THEN the linkUsed is set to True
    """

    def test_add_mandatory_bb_link(self):
        expected_bb_link_list = pd.DataFrame(
            data={
                "gNbs": [
                    (209331, 209024),
                    (209331, 208727),
                    (209335, 209027),
                    (209331, 209026),
                    (209026, 209024),
                    (209335, 209024),
                ],
                "usability": [0.433, 0.12, 0, 0.34, 0.23, 0.2],
                "gNb0": [209331, 209331, 209335, 209331, 209026, 209335],
                "gNb1": [209024, 208727, 209027, 209026, 209024, 209024],
                "linkUsed": 3 * [True] + 3 * [False],
            }
        )
        action_output = bb_configuration.add_mandatory_bb_links(
            self.test_bb_link_value_list_expanded, self.test_mandatory_bb_links
        )
        pd.testing.assert_frame_equal(expected_bb_link_list, action_output)

    """
            GIVEN gNb pairs result dataframe and the collection model document
            THEN the gNb pairs result will be saved in list
            AND this list is saved in the database
            AND optimization status will be updated
    """

    @patch.object(bb_configuration.logger, "debug")
    @patch.object(database_collection_models.Optimization, "update")
    def test_save_bb_links_result(self, mocked_update, mocked_logging):
        optimization_document = Optimization()
        response_test = bb_configuration.save_bb_links_result(
            self.test_bb_link_value_list_expanded, optimization_document
        )
        mocked_update.assert_called_once()
        mocked_logging.assert_called_once()
        self.assertEqual(self.expected_save_bb_links_response, response_test)

    """
            GIVEN coverage data, mandatory bb_link and the collection model document
            WHEN coverage data df is empty
            AND mandatory BB links is empty
            THEN optimization collection document is updated
            AND empty dataframe is returned
    """

    @patch.object(bb_configuration.logger, "info")
    @patch.object(database_collection_models.Optimization, "update")
    def test_check_coverage_data_and_mandatory_links_when_empty(
        self, mocked_update, mocked_logging
    ):
        expected_result = pd.DataFrame()
        optimization_document = Optimization()
        test_mandatory_bb_links = []
        raw_coverage_data_test = pd.DataFrame(
            columns=["pNCI", "sNCI", "coded_usefulness"]
        )
        response_test = bb_configuration.check_coverage_data_and_mandatory_links(
            raw_coverage_data_test, test_mandatory_bb_links, optimization_document
        )
        mocked_update.assert_called_once()
        mocked_logging.assert_called_once()
        pd.testing.assert_frame_equal(expected_result, response_test)

    """
            GIVEN coverage data, mandatory bb_link and the collection model document
            WHEN coverage data df is empty
            AND mandatory BB links is not empty
            THEN optimization collection document is updated
            AND dataframe filled with mandatory links is returned
    """

    @patch.object(bb_configuration, "add_mandatory_bb_links")
    def test_check_coverage_data_and_mandatory_links_when_mandatory_links_not_empty(
        self, mocked_add_mandatory_bb_links
    ):
        expected_result = pd.DataFrame(
            data={
                "gNbs": [(209331, 209024), (209331, 208727), (209335, 209027)],
                "usability": [0.433, 0.12, 0],
                "gNb0": [209331, 209331, 209335],
                "gNb1": [209024, 208727, 209027],
                "linkUsed": 3 * [True],
            }
        )
        mocked_add_mandatory_bb_links.return_value = expected_result
        optimization_document = Optimization()
        raw_coverage_data_test = pd.DataFrame(
            columns=["pNCI", "sNCI", "coded_usefulness"]
        )
        response_test = bb_configuration.check_coverage_data_and_mandatory_links(
            raw_coverage_data_test, self.test_mandatory_bb_links, optimization_document
        )
        mocked_add_mandatory_bb_links.assert_called_once()
        pd.testing.assert_frame_equal(expected_result, response_test)


if __name__ == "__main__":
    unittest.main()
