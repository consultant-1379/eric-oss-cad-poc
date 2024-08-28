#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import unittest
from unittest.mock import patch

import pandas as pd
from optimization import predict_cell_capability
from . import DIR


class TestPredictCellCapability(unittest.TestCase):
    test_capacity_data = pd.DataFrame(
        data={"col1": [12, 23, 34], "RBSymFree": [43, 23, 56], "col3": [42, 34, 56]}
    )

    """
    GIVEN model filepath and a separator character
    THEN model parameters: intercept, coefficient0, normalizeConstant0 are extracted and returned
    """

    @patch.object(pd, "read_csv")
    def test_get_model_returns_model_parameters(self, mocked_read_csv):
        test_file_name = "test_filename"
        test_separator = ","
        test_intercept = [3, 5]
        test_rb_sym_free = [23, 545]
        test_df = pd.DataFrame(
            data={"intercept": test_intercept, "RBSymFree": test_rb_sym_free}
        )
        mocked_read_csv.return_value = test_df
        action_output = predict_cell_capability.get_model(
            test_file_name, test_separator
        )
        mocked_read_csv.assert_called_once_with(test_file_name, sep=test_separator)
        self.assertTupleEqual(
            (test_intercept[0], test_rb_sym_free[0], test_rb_sym_free[1]), action_output
        )

    """
    GIVEN DataFrame, column list and normalization coefficient list
    WHEN input DataFrame has all columns provided on the column list
    THEN DataFrame containing normalized columns is returned
    """

    def test_normalize_data(self):
        action_output = predict_cell_capability.normalize_data(
            self.test_capacity_data, ["col1", "col3"], [100, 200]
        )
        self.assertListEqual([0.12, 0.23, 0.34], action_output["col1Norm"].to_list())
        self.assertListEqual([0.21, 0.17, 0.28], action_output["col3Norm"].to_list())

    """
    GIVEN model filepath and capacity DataFrame
    THEN capacity data is converted to predicted capacity basing on the model parameters
    AND returned as a DataFrame with 'predictedCapacity' column
    """

    def test_add_predicted_cell_capacity(self):
        action_output = predict_cell_capability.add_predicted_cell_capacity(
            f"{DIR}/resources/modelParametersLinRegr_60.csv", ",", self.test_capacity_data
        )
        self.assertAlmostEqual(
            5.53182107087134196697018433254e-5,
            action_output["predictedCapacity"].to_list()[0],
            delta=4.8e-11,
        )
        self.assertAlmostEqual(
            5.5317233003503154707049823174051e-5,
            action_output["predictedCapacity"].to_list()[1],
            delta=4.8e-11,
        )
        self.assertAlmostEqual(
            5.5318846217100091895425656423777e-5,
            action_output["predictedCapacity"].to_list()[2],
            delta=4.8e-11,
        )


if __name__ == "__main__":
    unittest.main()
