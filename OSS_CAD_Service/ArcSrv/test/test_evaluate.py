#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import unittest

import pandas as pd
from optimization import evaluate


class TestEvaluate(unittest.TestCase):

    """
    GIVEN valid bb_link_value_list
    WHEN there are both links marked as False and True
    THEN return aggregated arc value for links marked as True
    """

    def test_calculate_arc_value(self):
        test_bb_link_value_list = pd.DataFrame(
            data={
                "gNbs": [(209331, 209024), (209335, 209026), (209331, 208727)],
                "usability": [0.64, 0.2, 0.12],
                "gNb0": [209331, 209335, 209331],
                "gNb1": [209024, 209026, 208727],
                "linkUsed": [True, False, True],
            }
        )
        action_output = evaluate.calculate_arc_value(test_bb_link_value_list)
        self.assertEqual(0.76, action_output)


if __name__ == "__main__":
    unittest.main()
