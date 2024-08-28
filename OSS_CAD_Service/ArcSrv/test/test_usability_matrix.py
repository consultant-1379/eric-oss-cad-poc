#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import datetime
import unittest
from unittest.mock import patch

import pandas as pd
from entities import database_collection_models
from entities.api_response import ResponseStatus
from entities.database_collection_models import Optimization
from optimization import usability_matrix


class TestUsabilityMatrix(unittest.TestCase):
    """
    GIVEN capability DataFrame and a capability limit
    THEN return usability DataFrame where usability equals capability
    AND usability is replaced by 0 (zero) if it's below the limit
    """ ""

    @patch.object(database_collection_models.Optimization, "update")
    def test_build_usability_matrix(self, mocked_update):
        test_capability_matrix = pd.DataFrame(
            data={
                "pNCI": [53588737, 53588737, 53588738],
                "sNCI": [53510146, 53510672, 53434115],
                "primary_gnb": [209331, 209331, 209331],
                "capability": [0.00048, 0.2, 0.133],
                "secondary_gnb": [209024, 209026, 208727],
            }
        )
        optimization_document = Optimization(
            status=ResponseStatus.OPTIMIZATION_IN_PROGRESS,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        action_output = usability_matrix.build_usability_matrix(
            test_capability_matrix, 0.1, optimization_document
        )
        mocked_update.assert_called_once()
        self.assertListEqual([0, 0.2, 0.133], action_output["usability"].to_list())


if __name__ == "__main__":
    unittest.main()
