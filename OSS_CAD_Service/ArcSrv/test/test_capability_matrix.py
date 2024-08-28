#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import datetime
import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd
from entities import database_collection_models
from entities.api_response import ResponseStatus
from entities.database_collection_models import Optimization
from optimization import capability_matrix


class TestCapabilityMatrix(unittest.TestCase):
    """
    GIVEN coverage and capacity DataFrames
    THEN capability is calculated basing on hit rate and predictedCapacity
    AND a new DataFrame with this information is returned
    """ ""

    @patch.object(database_collection_models.Optimization, "update")
    def test_build_capability_matrix(self, mocked_update):
        test_pnci = [53588737, 53588737, 53588738]
        test_snci = [53510146, 53510672, 53434115]
        test_primary_gnb = [209331, 209331, 209331]
        test_secondary_gnb = [209024, 209026, 208727]
        test_coverage = pd.DataFrame(
            data={
                "pNCI": test_pnci,
                "sNCI": test_snci,
                "coded_usefulness": [9, 4, 1],
                "hitrate": [0.008, 0.023, 0.133],
                "primary_gnb": test_primary_gnb,
                "secondary_gnb": test_secondary_gnb,
            }
        )
        test_capacity = pd.DataFrame(
            data={
                "pmMacRBSymAvailDl": [204, 204, 204],
                "pmMacRBSymUsedPdschTypeA": [191, 187, 181],
                "sNCI": test_snci,
                "RBSymFree": [13, 17, 23],
                "secondary_gnb": test_secondary_gnb,
                "RBSymFreeNorm": [0.06, 0.08, 0.10],
                "predictedCapacity": [0.06, np.nan, 0.10],
            }
        )
        expected_capability_matrix = pd.DataFrame(
            data={
                "pNCI": test_pnci,
                "sNCI": test_snci,
                "primary_gnb": test_primary_gnb,
                "capability": [0.00048, 0, 0.0133],
                "secondary_gnb": test_secondary_gnb,
            }
        )
        optimization_document = Optimization(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        action_output = capability_matrix.build_capability_matrix(
            test_coverage, test_capacity, optimization_document
        )
        mocked_update.assert_called_once()
        pd.testing.assert_frame_equal(expected_capability_matrix, action_output)


if __name__ == "__main__":
    unittest.main()
