#!/usr/bin/env python
# Test file for build_solution
# Copyright Ericsson (c) 2022
import datetime
import unittest
from unittest.mock import patch

import pandas as pd
from entities import database_collection_models
from entities.api_response import ResponseStatus
from entities.database_collection_models import Optimization
from helper import optimization_threading_helper
from optimization import (
    bb_configuration,
    build_solution,
    capability_matrix,
    definitions,
    evaluate,
    get_data,
    usability_matrix,
    report
)


class TestBuildSolutionCommon:
    """Class containing common stuff shared by TestCases"""

    test_bb_dict = {
        208727: "12341",
        208728: "12342",
        208730: "12343",
        208731: "12344",
        208733: "12345",
    }
    test_unwanted_bb_links = [(208727, 208731), (208733, 208731)]
    test_mandatory_bb_links = [(208731, 208733)]


class TestBuildSolutionRunOptimizationService(
    TestBuildSolutionCommon, unittest.TestCase
):
    """
    GIVEN BB dictionary and unwanted BB pairs
    WHEN run_optimization raises an exception
    THEN catch and log the exception
    AND set task_status, task_message and task_result
    """

    @patch.object(build_solution.logger, "exception")
    @patch.object(
        build_solution, "run_optimization", side_effect=ZeroDivisionError("Oops!")
    )
    @patch.object(database_collection_models.Optimization, "update")
    def test_exception_caught(
        self, mocked_update, mocked_run_optimization, mocked_logger
    ):
        optimization_document = Optimization(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        self.assertIsNone(
            build_solution.run_optimization_service(
                self.test_bb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                optimization_document,
            )
        )
        mocked_update.assert_called_once_with(
            status=ResponseStatus.OPTIMIZATION_UNEXPECTED_ERROR
        )
        mocked_run_optimization.assert_called_once_with(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            optimization_document,
        )
        mocked_logger.assert_called_once()

    """
    GIVEN BB dictionary and unwanted BB pairs
    WHEN run_optimization does not raise an exception
    THEN return
    """

    @patch.object(build_solution, "run_optimization")
    def test_no_exception(self, mocked_run_optimization):
        optimization_document = Optimization(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        self.assertIsNone(
            build_solution.run_optimization_service(
                self.test_bb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                optimization_document,
            )
        )
        mocked_run_optimization.assert_called_once_with(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            optimization_document,
        )

    """
    GIVEN None as BB dictionary and unwanted BB pairs
    WHEN run_optimization will raise an exception
    THEN catch and log exception
    AND set task_status, task_message and task_result
    """

    @patch.object(build_solution.logger, "exception")
    @patch.object(database_collection_models.Optimization, "update")
    def test_none_as_parameter(
        self,
        mocked_update,
        mocked_exception_logger,
    ):
        optimization_document = Optimization(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        self.assertIsNone(
            build_solution.run_optimization_service(
                None, None, None, optimization_document
            )
        )
        mocked_update.assert_called_with(
            status=ResponseStatus.OPTIMIZATION_UNEXPECTED_ERROR
        )
        mocked_exception_logger.assert_called_once()


class TestBuildSolutionRunOptimization(TestBuildSolutionCommon, unittest.TestCase):
    test_raw_coverage_data = pd.DataFrame(
        data={
            "pNCI": 2 * [53434113]
            + 2 * [53434114]
            + 2 * [53434369]
            + 2 * [53434370]
            + 2 * [53435078],
            "sNCI": [
                53434885,
                53435140,
                53434885,
                53434886,
                53435659,
                53436676,
                53435659,
                53436676,
                53435137,
                53434369,
            ],
            "coded_usefulness": [3, 3, 6, 1, 25, 7, 34, 14, 3, 6],
        }
    )
    test_digits_for_cell_id = 2
    test_capacity_file_path = definitions.PM_DATA_FILE
    test_separator = ","
    test_column_index = 0
    test_pnci = 2 * [53434113] + 2 * [53434114] + [53434369, 53434370] + 2 * [53435078]
    test_snci = (
        [53434885, 53435140, 53434885, 53434886] + 2 * [53435659] + [53435137, 53434369]
    )
    test_primary_gnb = 4 * [208727] + 2 * [208728] + 2 * [208730]
    test_secondary_gnb = (
        [208730, 208731] + 2 * [208730] + 2 * [208733] + [208731, 208728]
    )
    test_clean_coverage_data = pd.DataFrame(
        data={
            "pNCI": test_pnci,
            "sNCI": test_snci,
            "coded_usefulness": [3, 3, 6, 1, 25, 34, 3, 6],
            "hitrate": [0.034, 0.034, 0.014, 0.134, 0.002, 0.002, 0.034, 0.014],
            "pGnb": test_primary_gnb,
            "sGnb": test_secondary_gnb,
        }
    )
    test_raw_capacity_data = pd.DataFrame(
        data={
            "pmMacRBSymAvailDl": 5 * [20442253104],
            "pmMacRBSymUsedPdschTypeA": [
                6200816768,
                1839802779,
                5655690032,
                0,
                3475183027,
            ],
            "sNCI": [53434885, 53435140, 53434886, 53435659, 53435137],
        }
    )
    test_clean_capacity_data = test_raw_capacity_data.copy()
    test_clean_capacity_data["RBSymFree"] = [
        14241436336,
        18602450325,
        14786563072,
        20442253104,
        16967070077,
    ]
    test_clean_capacity_data["secondary_gnb"] = [208730, 208731, 208730, 208733, 208737]
    test_model_file_path = definitions.MODEL_FILENAME
    test_capacity = [0.696, 0.909, 0.723, 0.999, 0.829]
    test_predicted_cell_capacity = test_clean_capacity_data.copy()
    test_predicted_cell_capacity["RBSymFreeNorm"] = test_capacity
    test_predicted_cell_capacity["predictedCapacity"] = test_capacity
    test_capability = [0.0237, 0.0309, 0.0097, 0.0969, 0.0020, 0.0020, 0.0088, 0]
    test_capability_matrix = pd.DataFrame(
        data={
            "pNCI": test_pnci,
            "sNCI": test_snci,
            "primary_gnb": test_primary_gnb,
            "secondary_gnb": test_secondary_gnb,
            "capability": test_capability,
        }
    )
    test_capability_limit = 0.1
    test_usability_matrix = test_capability_matrix.copy()
    test_usability = test_capability.copy()
    test_usability_matrix["usability"] = test_usability
    test_gnbs = list(zip(test_primary_gnb, test_secondary_gnb))
    test_ps_cell_value_list = test_usability_matrix.copy()
    test_ps_cell_value_list["gNbs"] = test_gnbs
    test_gnb_0 = 2 * [208727] + [208728] + 2 * [208730]
    test_gnb_1 = [208730, 208731, 208733, 208731, 208728]
    test_filtered_gnb_0 = [208727] + [208728] + 2 * [208730]
    test_filtered_gnb_1 = [208730, 208733, 208731, 208728]
    test_gnb_0_with_mandatory_bb = [208731] + [208727] + [208728] + 2 * [208730]
    test_gnb_1_with_mandatory_bb = [208733, 208730, 208733, 208731, 208728]
    test_gnbs_grouped = list(zip(test_gnb_0, test_gnb_1))
    test_filtered_gnbs_grouped = list(zip(test_filtered_gnb_0, test_filtered_gnb_1))
    test_gnbs_grouped_with_mandatory_bb = list(
        zip(test_gnb_0_with_mandatory_bb, test_gnb_1_with_mandatory_bb)
    )
    test_usability_1 = [0.1303, 0.0309, 0.0040, 0.0088, 0]
    test_filtered_bb_usability_1 = [0.1303, 0.0040, 0.0088, 0]
    test_bb_usability_with_mandatory_links1 = [0, 0.1303, 0.0040, 0.0088, 0]
    test_bb_link_value_list = pd.DataFrame(
        data={
            "gNbs": test_gnbs_grouped,
            "usability": test_usability_1,
            "gNb0": test_gnb_0,
            "gNb1": test_gnb_1,
            "linkUsed": 5 * [False],
        }
    )
    test_filtered_bb_link_value_list = pd.DataFrame(
        data={
            "gNbs": test_filtered_gnbs_grouped,
            "usability": test_filtered_bb_usability_1,
            "gNb0": test_filtered_gnb_0,
            "gNb1": test_filtered_gnb_1,
            "linkUsed": 4 * [False],
        }
    )
    test_resulted_bb_link_with_mandatory_links_list = pd.DataFrame(
        data={
            "gNbs": test_gnbs_grouped_with_mandatory_bb,
            "usability": test_bb_usability_with_mandatory_links1,
            "gNb0": test_gnb_0_with_mandatory_bb,
            "gNb1": test_gnb_1_with_mandatory_bb,
            "linkUsed": [True] + 4 * [False],
        }
    )
    test_max_external_cells_secondary_gnb = 10
    test_unique_gnbs = [208727, 208728, 208730, 208731, 208733]
    test_max_bb_partners = 6
    test_updated_bb_link_value_list = test_filtered_bb_link_value_list.copy()
    test_updated_bb_link_value_list["linkUsed"] = 4 * [True]
    test_bb_link_list = pd.DataFrame(
        data={
            "gNb0": test_filtered_gnb_0,
            "gNb1": test_filtered_gnb_1,
            "usability": test_filtered_bb_usability_1,
            "pCmHandle": ["12341", "12342", "12343", "12343"],
            "sCmHandle": ["12343", "12345", "12344", "12342"],
        }
    )
    expected_bb_link_list_with_mandatory_links = pd.DataFrame(
        data={
            "gNbs": [(209331, 209333), (209330, 209328)],
            "usability": [0, 0],
            "gNb0": [209331, 209330],
            "gNb1": [209333, 209328],
            "linkUsed": True,
        }
    )
    test_bb_link_list_with_mandatory_links = pd.DataFrame(
        data={
            "gNb0": [209331, 209330],
            "gNb1": [209333, 209328],
            "usability": [0, 0],
            "pCmHandle": ["12344", "12345"],
            "sCmHandle": ["12345", "12342"],
        }
    )
    optimization_response_list = [
        {
            "p_gnbdu_id": 208727,
            "s_gnbdu_id": 208730,
            "usability": 0.1303,
        },
        {
            "p_gnbdu_id": 208728,
            "s_gnbdu_id": 208733,
            "usability": 0.004,
        },
        {
            "p_gnbdu_id": 208730,
            "s_gnbdu_id": 208731,
            "usability": 0.0088,
        },
        {
            "p_gnbdu_id": 208730,
            "s_gnbdu_id": 208728,
            "usability": 0.0,
        },
    ]
    optimization_document = Optimization(
        status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
        creation_date=datetime.datetime.now(),
        target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
    )

    # helper function to apply patches in setUp
    def apply_patch(self, target, member, **kwargs):
        patcher = patch.object(target, member, **kwargs)
        mock = patcher.start()
        self.addCleanup(patcher.stop)
        return mock

    def setUp(self):
        self.mocked_update = self.apply_patch(
            database_collection_models.Optimization, "update"
        )
        self.mocked_info_logger = self.apply_patch(
            build_solution.logger, "info"
        )  # suppress logging messages
        self.mocked_debug_logger = self.apply_patch(
            build_solution.logger, "debug"
        )  # suppress logging messages
        self.mocked_is_set = self.apply_patch(
            optimization_threading_helper.task_stop_event, "is_set"
        )
        self.mocked_build_capability_matrix = self.apply_patch(
            capability_matrix,
            "build_capability_matrix",
            return_value=self.test_capability_matrix,
        )
        self.mocked_build_usability_matrix = self.apply_patch(
            usability_matrix,
            "build_usability_matrix",
            return_value=self.test_usability_matrix,
        )
        self.mocked_improve_ps_cell_value_list = self.apply_patch(
            bb_configuration,
            "improve_ps_cell_value_list",
            return_value=self.test_ps_cell_value_list,
        )
        self.mocked_create_bb_link_value_list = self.apply_patch(
            bb_configuration,
            "create_bb_link_value_list",
            return_value=self.test_bb_link_value_list,
        )
        self.mocked_get_unique_gnb_list = self.apply_patch(
            bb_configuration,
            "get_unique_gnb_list",
            return_value=self.test_unique_gnbs,
        )
        self.mocked_check_unwanted_bb_link = self.apply_patch(
            bb_configuration,
            "check_unwanted_bb_link",
            return_value=self.test_filtered_bb_link_value_list,
        )
        self.mocked_add_mandatory_bb_link = self.apply_patch(
            bb_configuration,
            "add_mandatory_bb_links",
            return_value=self.test_resulted_bb_link_with_mandatory_links_list,
        )
        self.mocked_check_coverage_data_and_mandatory_links = self.apply_patch(
            bb_configuration,
            "check_coverage_data_and_mandatory_links",
            return_value=self.test_resulted_bb_link_with_mandatory_links_list,
        )
        self.mocked_assign_bb_links = self.apply_patch(
            bb_configuration,
            "assign_bb_links",
            return_value=self.test_updated_bb_link_value_list,
        )
        self.mocked_get_bb_link_list = self.apply_patch(
            bb_configuration, "get_bb_link_list", return_value=self.test_bb_link_list
        )
        self.mocked_save_report = self.apply_patch(report, "save_in_bucket")
        self.mocked_save_bb_links_result = self.apply_patch(
            bb_configuration,
            "save_bb_links_result",
            return_value=self.optimization_response_list,
        )
        self.mocked_get_predicted_capacity = self.apply_patch(
            get_data,
            "get_predicted_capacity",
            return_value=self.test_predicted_cell_capacity,
        )
        self.mocked_check_coverage_data_and_mandatory_links.return_value = (
            pd.DataFrame()
        )
        self.mocked_get_coverage_data = self.apply_patch(
            get_data,
            "get_coverage_data",
            return_value=self.test_clean_coverage_data,
        )

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory link list is empty
    WHEN no data is returned from NCMP for any of these BBs
    THEN optimization exits with an empty result BB list
    """

    def test_no_ncmp_data_with_no_mandatory_link(self):
        test_mandatory_bb_links = []
        self.mocked_get_coverage_data.return_value = pd.DataFrame(
            columns=["pNCI", "sNCI", "coded_usefulness"]
        )
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )
        self.assertIsNone(action_output)

    """
        GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
        WHEN no data is returned from NCMP for any of these BBs and mandatory links exists
        THEN optimization return mandatory BB list as a result
    """

    def test_no_ncmp_data_with_mandatory_link(self):
        self.mocked_is_set.side_effect = 4 * [False] + [True]
        test_mandatory_bb_links = [(209331, 209333), (209330, 209328)]
        self.mocked_get_coverage_data.return_value = pd.DataFrame(
            columns=["pNCI", "sNCI", "coded_usefulness"]
        )
        self.mocked_check_coverage_data_and_mandatory_links.return_value = (
            self.expected_bb_link_list_with_mandatory_links
        )
        self.mocked_assign_bb_links.return_value = (
            self.expected_bb_link_list_with_mandatory_links
        )
        self.mocked_get_bb_link_list.return_value = (
            self.test_bb_link_list_with_mandatory_links
        )
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.mocked_check_coverage_data_and_mandatory_links.assert_called_once()
        self.mocked_assign_bb_links.assert_called_once_with(
            self.expected_bb_link_list_with_mandatory_links,
            self.test_max_bb_partners,
        )

        self.mocked_get_bb_link_list.assert_called_once_with(
            self.expected_bb_link_list_with_mandatory_links,
            self.test_bb_dict,
        )
        self.mocked_save_bb_links_result.assert_called_once()
        self.assertIsNone(action_output)

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    WHEN no data is left after cleaning capacity data and mandatory link list is empty
    THEN optimization exits with an empty result BB list
    """

    def test_no_clean_coverage_data_and_mandatory_link_list_is_empty(self):
        self.mocked_get_coverage_data.return_value = pd.DataFrame()
        test_mandatory_bb_links = []
        self.assertIsNone(
            build_solution.run_optimization(
                self.test_bb_dict,
                self.test_unwanted_bb_links,
                test_mandatory_bb_links,
                self.optimization_document,
            )
        )
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )

    """
            GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
            WHEN no data is returned from NCMP for any of these BBs and mandatory link list is not empty
            THEN optimization return mandatory BB list as a result
    """

    def test_no_clean_coverage_data_with_mandatory_link(self):
        self.mocked_is_set.side_effect = 4 * [False] + [True]
        test_mandatory_bb_links = [(209331, 209333), (209330, 209328)]
        self.mocked_check_coverage_data_and_mandatory_links.return_value = (
            self.expected_bb_link_list_with_mandatory_links
        )
        self.mocked_get_coverage_data.return_value = pd.DataFrame(
            columns=["pNCI", "sNCI", "coded_usefulness", "hitrate", "pGnb", "sGnb"]
        )
        self.mocked_assign_bb_links.return_value = (
            self.expected_bb_link_list_with_mandatory_links
        )
        self.mocked_get_bb_link_list.return_value = (
            self.test_bb_link_list_with_mandatory_links
        )

        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.mocked_assign_bb_links.assert_called_once_with(
            self.expected_bb_link_list_with_mandatory_links,
            self.test_max_bb_partners,
        )

        self.mocked_get_bb_link_list.assert_called_once_with(
            self.expected_bb_link_list_with_mandatory_links,
            self.test_bb_dict,
        )
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )
        self.mocked_save_bb_links_result.assert_called_once()
        self.assertIsNone(action_output)

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    WHEN optimization is stopped during execution before predicting cell capacity
    THEN optimization exits prematurely after cleaning capacity data
    """

    def test_stopped_after_cleaning_capacity_data(self):
        self.mocked_is_set.side_effect = [False] + [True]
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.mocked_get_predicted_capacity.assert_called_once_with(
            self.optimization_document
        )
        self.assertEqual(2, self.mocked_is_set.call_count)
        self.assertIsNone(action_output)

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    WHEN optimization is stopped during execution between building capability and usability matrices
    THEN optimization exits prematurely after building capability matrix
    """

    def test_stopped_after_building_capability_matrix(self):
        self.mocked_is_set.side_effect = [False] + [True]
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.mocked_get_predicted_capacity.assert_called_once_with(
            self.optimization_document
        )
        self.mocked_build_capability_matrix.assert_called_once_with(
            self.test_clean_coverage_data,
            self.test_predicted_cell_capacity,
            self.optimization_document,
        )
        self.assertEqual(2, self.mocked_is_set.call_count)
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )
        self.assertIsNone(action_output)

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    WHEN optimization is stopped during execution between building usability matrix and improving PS cell value list
    THEN optimization exits prematurely after building usability matrix
    """

    def test_stopped_after_building_usability_matrix(self):
        self.mocked_is_set.side_effect = 2 * [False] + [True]
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_build_usability_matrix.assert_called_once_with(
            self.test_capability_matrix,
            self.test_capability_limit,
            self.optimization_document,
        )
        self.assertEqual(3, self.mocked_is_set.call_count)
        self.assertEqual(1, self.mocked_update.call_count)
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )
        self.assertIsNone(action_output)

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    WHEN optimization is stopped during execution after preparing BB linked list
    THEN optimization exits prematurely
    """

    def test_stopped_after_preparing_bb_linked_list(self):
        self.mocked_is_set.side_effect = 3 * [False] + [True]
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_debug_logger.assert_not_called()
        self.mocked_get_bb_link_list.assert_called_once_with(
            self.test_updated_bb_link_value_list, self.test_bb_dict
        )
        self.assertEqual(4, self.mocked_is_set.call_count)
        self.assertIsNone(action_output)
        self.mocked_save_bb_links_result.assert_not_called()

    """
    GIVEN BB dictionary and unwanted BB pairs and mandatory BB pairs
    THEN resulting BB list is stored in task_result
    """

    @patch.object(evaluate, "calculate_arc_value")
    def test_produces_bb_link_list(self, mocked_calculate_arc_value):
        self.mocked_is_set.return_value = False
        test_total_arc_value = 0.1792
        mocked_calculate_arc_value.return_value = test_total_arc_value
        action_output = build_solution.run_optimization(
            self.test_bb_dict,
            self.test_unwanted_bb_links,
            self.test_mandatory_bb_links,
            self.optimization_document,
        )
        self.mocked_get_coverage_data.assert_called_once_with(
            self.test_bb_dict, self.optimization_document
        )
        self.mocked_get_predicted_capacity.assert_called_once_with(
            self.optimization_document
        )
        self.mocked_build_capability_matrix.assert_called_once_with(
            self.test_clean_coverage_data,
            self.test_predicted_cell_capacity,
            self.optimization_document,
        )
        self.mocked_build_usability_matrix.assert_called_once_with(
            self.test_capability_matrix,
            self.test_capability_limit,
            self.optimization_document,
        )
        self.mocked_create_bb_link_value_list.assert_called_once_with(
            self.test_usability_matrix, self.test_max_external_cells_secondary_gnb
        )
        self.mocked_check_unwanted_bb_link.assert_called_once_with(
            self.test_bb_link_value_list, self.test_unwanted_bb_links
        )
        self.mocked_add_mandatory_bb_link.assert_called_once_with(
            self.test_filtered_bb_link_value_list, self.test_mandatory_bb_links
        )
        self.mocked_assign_bb_links.assert_called_once_with(
            self.test_resulted_bb_link_with_mandatory_links_list,
            self.test_max_bb_partners,
        )
        self.mocked_debug_logger.assert_called_once()
        self.mocked_get_bb_link_list.assert_called_once_with(
            self.test_updated_bb_link_value_list, self.test_bb_dict
        )
        self.mocked_save_bb_links_result.assert_called_once()
        self.assertEqual(4, self.mocked_is_set.call_count)
        mocked_calculate_arc_value.assert_called_once_with(
            self.test_updated_bb_link_value_list
        )
        self.assertEqual(2, self.mocked_info_logger.call_count)
        self.assertIsNone(action_output)
        self.assertEqual(
            1, self.mocked_check_coverage_data_and_mandatory_links.call_count
        )


if __name__ == "__main__":
    unittest.main()
