#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import json
import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd
import requests
from entities import database_collection_models
from entities.api_response import ResponseStatus
from entities.database_collection_models import Optimization
from optimization import (
    common_functions,
    definitions,
    get_data,
    predict_cell_capability,
)


class TestGetData(unittest.TestCase):
    GNB_ID_LIST = '{{"gnb_id_list": {}}}'
    digits_for_cell_id = 2
    test_snci_get_clean_coverage_data = [53510146, 53539082, 53588736]
    test_snci = [53510146, 53539082]
    mocked_secondary_gnb = [209333, 209334, 209331]
    initial_bb_list = [209333, 209334]
    test_coded_usefulness = [9, 1, 1]
    test_pnci = [53588737, 53589250, 53589250]
    expected_primary_gnb = [209331, 209332]
    expected_secondary_gnb = [209333, 209334]
    expected_hit_rate = [0.05, 0.3]
    expected_coded_usefulness = [9, 1]
    expected_pnci = [53588737, 53589250]
    expected_snci = [53510146, 53539082]
    test_raw_coverage_data = pd.DataFrame(
        data={
            "pNCI": test_pnci,
            "sNCI": test_snci_get_clean_coverage_data,
            "coded_usefulness": test_coded_usefulness,
        }
    )
    empty_raw_coverage_data = pd.DataFrame(
        columns=["pNCI", "sNCI", "coded_usefulness"],
        dtype=np.int64,
        index=pd.RangeIndex(stop=0),
    )
    optimization_document = Optimization(
        status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
    )

    """
    GIVEN non-empty BB dictionary
    WHEN data for some of BBs is returned from NCMP
    THEN received data should be parsed, processed
    AND returned as a DataFrame
    """

    @unittest.skip
    @patch.object(requests, "request")
    @patch.object(get_data.logger, "debug")
    @patch.object(database_collection_models.Optimization, "update")
    def test_get_raw_coverage_data_ncmp_two_gnbs_found(
        self, mocked_update, mocked_debug_logger, mocked_request
    ):
        test_bb_dict = {
            209331: "12341",
            209333: "12342",
            123456: "12343",
            234567: "12344",
        }
        test_response = requests.Response()
        response_content = {
            "gNBwithResult": [
                {
                    "gNB": 209331,
                    "nrCells": [
                        {
                            "nci": 53588737,
                            "cellUsefullness": [
                                {"nci": 53510146, "CoverageRate": 9},
                                {"nci": 53510672, "CoverageRate": 4},
                            ],
                        },
                        {
                            "nci": 53588738,
                            "cellUsefullness": [
                                {"nci": 53434115, "CoverageRate": 1},
                                {"nci": 53434626, "CoverageRate": 1},
                            ],
                        },
                    ],
                },
                {
                    "gNB": 209333,
                    "nrCells": [
                        {
                            "nci": 53589249,
                            "cellUsefullness": [
                                {"nci": 53539074, "CoverageRate": 1},
                                {"nci": 53588993, "CoverageRate": 11},
                            ],
                        },
                        {
                            "nci": 53589250,
                            "cellUsefullness": [
                                {"nci": 53539073, "CoverageRate": 1},
                                {"nci": 53539082, "CoverageRate": 1},
                            ],
                        },
                    ],
                },
            ],
            "gNBNotFound": [123456, 234567],
        }
        test_response._content = json.dumps(response_content).encode("utf-8")
        expected_output = pd.DataFrame(
            data={
                "pNCI": [
                    53588737,
                    53588737,
                    53588738,
                    53588738,
                    53589249,
                    53589249,
                    53589250,
                    53589250,
                ],
                "sNCI": [
                    53510146,
                    53510672,
                    53434115,
                    53434626,
                    53539074,
                    53588993,
                    53539073,
                    53539082,
                ],
                "coded_usefulness": [9, 4, 1, 1, 1, 11, 1, 1],
            }
        )
        mocked_request.return_value = test_response
        action_output = get_data.get_raw_coverage_data_ncmp(
            test_bb_dict, self.optimization_document
        )
        mocked_request.assert_called_once_with(
            method="post",
            url=definitions.NCMP_CM_URL,
            data=self.GNB_ID_LIST.format(list(test_bb_dict)),
        )
        pd.testing.assert_frame_equal(expected_output, action_output)
        mocked_update.assert_called_once()
        self.assertEqual(4, mocked_debug_logger.call_count)

    """
    GIVEN non-empty BB dictionary
    WHEN no data to parse is returned from NCMP for any of BB
    THEN empty DataFrame should be returned
    """

    @unittest.skip
    @patch.object(requests, "request")
    @patch.object(get_data.logger, "debug")
    @patch.object(database_collection_models.Optimization, "update")
    def test_get_raw_coverage_data_ncmp_no_gnbs_found(
        self, mocked_update, mocked_debug_logger, mocked_request
    ):
        test_bb_dict = {123456: "12341", 234567: "12342"}
        test_response = requests.Response()
        response_content = {"gNBwithResult": [], "gNBNotFound": [123456, 234567]}
        test_response._content = json.dumps(response_content).encode("utf-8")
        mocked_request.return_value = test_response
        action_output = get_data.get_raw_coverage_data_ncmp(
            test_bb_dict, self.optimization_document
        )
        mocked_request.assert_called_once_with(
            method="post",
            url=definitions.NCMP_CM_URL,
            data=self.GNB_ID_LIST.format(list(test_bb_dict)),
        )
        mocked_update.assert_called_once()
        pd.testing.assert_frame_equal(self.empty_raw_coverage_data, action_output)
        self.assertEqual(4, mocked_debug_logger.call_count)

    """
    GIVEN empty BB dictionary
    THEN no data to parse should be received from NCMP
    AND empty DataFrame should be returned
    """

    @unittest.skip
    @patch.object(requests, "request")
    @patch.object(get_data.logger, "debug")
    @patch.object(database_collection_models.Optimization, "update")
    def test_get_raw_coverage_data_ncmp_empty_bb_list(
        self, mocked_update, mocked_debug_logger, mocked_request
    ):
        test_bb_dict_empty = {}
        test_response = requests.Response()
        response_content = {"gNBwithResult": [], "gNBNotFound": []}
        test_response._content = json.dumps(response_content).encode("utf-8")
        mocked_request.return_value = test_response
        action_output = get_data.get_raw_coverage_data_ncmp(
            test_bb_dict_empty, self.optimization_document
        )
        mocked_request.assert_called_once_with(
            method="post",
            url=definitions.NCMP_CM_URL,
            data=self.GNB_ID_LIST.format(list(test_bb_dict_empty)),
        )
        mocked_update.assert_called_once()
        pd.testing.assert_frame_equal(self.empty_raw_coverage_data, action_output)
        self.assertEqual(4, mocked_debug_logger.call_count)

    """
    GIVEN raw coverage data (pNCI, sNCI, coded_usefulness)
    THEN hitrate is calculated from coded_usefulness
    AND gNb IDs are calculated from respective NCIs
    """

    @patch.object(common_functions, "decoded_usefulness")
    @patch.object(common_functions, "gnb_id_from_nci")
    def test_get_clean_coverage_data(
        self, mocked_gnb_id_from_nci, mocked_decoded_usefulness
    ):
        mocked_hit_rate = [0.05, 0.3, 0.3]
        mocked_primary_gnb = [209331, 209332, 209332]
        mocked_decoded_usefulness.side_effect = mocked_hit_rate
        mocked_gnb_id_from_nci.side_effect = (
            mocked_primary_gnb + self.mocked_secondary_gnb
        )
        action_output = get_data.get_clean_coverage_data(
            self.test_raw_coverage_data, self.digits_for_cell_id, self.initial_bb_list
        )
        self.assertEqual(
            self.test_raw_coverage_data.shape[0], mocked_decoded_usefulness.call_count
        )
        self.assertEqual(
            self.test_raw_coverage_data.shape[0] * 2, mocked_gnb_id_from_nci.call_count
        )
        self.assertListEqual(self.expected_hit_rate, action_output["hitrate"].to_list())
        self.assertListEqual(
            self.expected_primary_gnb, action_output["primary_gnb"].to_list()
        )
        self.assertListEqual(
            self.expected_secondary_gnb, action_output["secondary_gnb"].to_list()
        )

    """
    GIVEN raw capacity data (pmMacRBSymAvailDl, pmMacRBSymUsedPdschTypeA, sNCI)
    THEN RBSymFree is calculated from pmMacRBSymAvailDl and pmMacRBSymUsedPdschTypeA
    AND gNb IDs are calculated from NCIs
    """

    @patch.object(common_functions, "gnb_id_from_nci")
    def test_get_clean_capacity_data(self, mocked_gnb_id_from_nci):
        test_raw_capacity_data = pd.DataFrame(
            data={
                "pmMacRBSymAvailDl": [20442253104, 20442253104],
                "pmMacRBSymUsedPdschTypeA": [19420140448, 19283858754],
                "sNCI": self.test_snci,
            }
        )
        expected_rb_sym_free = [1022112656, 1158394350]
        mocked_gnb_id_from_nci.side_effect = self.expected_secondary_gnb
        action_output = get_data.get_clean_capacity_data(
            test_raw_capacity_data, self.digits_for_cell_id
        )
        self.assertEqual(
            test_raw_capacity_data.shape[0], mocked_gnb_id_from_nci.call_count
        )
        for i in range(len(self.test_snci)):
            self.assertEqual(
                (self.test_snci[i], self.digits_for_cell_id),
                mocked_gnb_id_from_nci.call_args_list[i].args,
            )
        self.assertListEqual(expected_rb_sym_free, action_output["RBSymFree"].to_list())
        self.assertListEqual(
            self.expected_secondary_gnb, action_output["secondary_gnb"].to_list()
        )

    """
        GIVEN pm capacity data (pmMacRBSymAvailDl, pmMacRBSymUsedPdschTypeA, sNCI)
        THEN clean capacity data is calculated and cell capability is predicted
    """

    @patch.object(pd, "read_csv")
    @patch.object(get_data.logger, "debug")
    @patch.object(get_data.logger, "info")
    @patch.object(database_collection_models.Optimization, "update")
    @patch.object(get_data, "get_clean_capacity_data")
    @patch.object(predict_cell_capability, "add_predicted_cell_capacity")
    def test_get_predicted_capacity(
        self,
        mocked_add_predicted_cell_capacity,
        mocked_clean_capacity,
        mocked_update,
        mocked_info,
        mocked_debug,
        mocked_read_csv,
    ):
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
        test_clean_capacity_data["secondary_gnb"] = [
            208730,
            208731,
            208730,
            208733,
            208737,
        ]
        test_predicted_cell_capacity = test_clean_capacity_data.copy()
        test_capacity = [0.696, 0.909, 0.723, 0.999, 0.829]
        test_predicted_cell_capacity["RBSymFreeNorm"] = test_capacity
        test_predicted_cell_capacity["predictedCapacity"] = test_capacity
        mocked_read_csv.return_value = test_raw_capacity_data
        mocked_clean_capacity.return_value = test_clean_capacity_data
        mocked_add_predicted_cell_capacity.return_value = test_predicted_cell_capacity
        action_output = get_data.get_predicted_capacity(self.optimization_document)
        self.assertEqual(2, mocked_info.call_count)
        self.assertEqual(4, mocked_debug.call_count)
        self.assertEqual(2, mocked_update.call_count)
        mocked_read_csv.assert_called_once_with(
            definitions.PM_DATA_FILE, sep=definitions.MODEL_SEPARATOR, index_col=0
        )
        mocked_clean_capacity.assert_called_once_with(
            test_raw_capacity_data, definitions.DIGITS_FOR_CELL_ID
        )
        mocked_add_predicted_cell_capacity.assert_called_once_with(
            definitions.MODEL_FILENAME,
            definitions.MODEL_SEPARATOR,
            test_clean_capacity_data,
        )
        pd.testing.assert_frame_equal(test_predicted_cell_capacity, action_output)

    """
          GIVEN bb dict and the collection model document
          WHEN coverage data not empty
          THEN return clean coverage data for provided bb in the bb dict
    """

    @patch.object(get_data.logger, "info")
    @patch.object(get_data, "get_raw_coverage_data_ncmp")
    @patch.object(get_data, "get_clean_coverage_data")
    def test_get_coverage_data_when_raw_coverage_data_not_empty(
        self,
        mocked_get_clean_coverage_data,
        mocked_get_raw_coverage_data,
        mocked_logger,
    ):
        test_expected_clean_coverage_data = pd.DataFrame(
            data={
                "pNCI": self.expected_pnci,
                "sNCI": self.expected_snci,
                "coded_usefulness": self.expected_coded_usefulness,
                "hitrate": self.expected_hit_rate,
                "primary_gnb": self.expected_primary_gnb,
                "secondary_gnb": self.expected_secondary_gnb,
            }
        )
        test_bb_dict = {
            209331: "12341",
            209333: "12342",
            209334: "12343",
            209332: "12344",
        }
        mocked_get_raw_coverage_data.return_value = self.test_raw_coverage_data
        mocked_get_clean_coverage_data.return_value = test_expected_clean_coverage_data
        action_output = get_data.get_coverage_data(
            test_bb_dict, self.optimization_document
        )
        mocked_logger.assert_called_once()
        pd.testing.assert_frame_equal(test_expected_clean_coverage_data, action_output)
        mocked_get_raw_coverage_data.assert_called_once_with(
            test_bb_dict, self.optimization_document
        )
        mocked_get_clean_coverage_data.assert_called_once_with(
            self.test_raw_coverage_data,
            self.digits_for_cell_id,
            list(map(int, list(test_bb_dict))),
        )

    """
          GIVEN bb dict and the collection model document
          WHEN coverage data is empty
          THEN return empty clean coverage data dataframe
    """

    @patch.object(get_data.logger, "info")
    @patch.object(get_data, "get_raw_coverage_data_ncmp")
    @patch.object(get_data, "get_clean_coverage_data")
    def test_get_coverage_data_when_raw_coverage_data_empty(
        self,
        mocked_get_clean_coverage_data,
        mocked_get_raw_coverage_data,
        mocked_logger,
    ):
        test_expected_clean_coverage_data = pd.DataFrame()
        test_bb_dict = {
            209331: "12341",
            209333: "12342",
            209334: "12343",
            209332: "12344",
        }
        mocked_get_raw_coverage_data.return_value = self.empty_raw_coverage_data
        action_output = get_data.get_coverage_data(
            test_bb_dict, self.optimization_document
        )
        mocked_get_raw_coverage_data.assert_called_once_with(
            test_bb_dict, self.optimization_document
        )
        mocked_logger.assert_called_once()
        pd.testing.assert_frame_equal(test_expected_clean_coverage_data, action_output)
        mocked_get_clean_coverage_data.assert_not_called()


if __name__ == "__main__":
    unittest.main()
# EOF
