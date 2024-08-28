#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import unittest

from optimization import common_functions


class TestCommonFunctions(unittest.TestCase):

    """
    GIVEN coded usefulness
    THEN decoded usefulness is returned
    """

    def test_decoded_usefulness(self):
        test_coded_usefulness = 4
        test_normalized_usefulness = 0.02364354
        action_output = common_functions.decoded_usefulness(test_coded_usefulness)
        self.assertAlmostEqual(action_output, test_normalized_usefulness)

    """
    GIVEN NCI and the length of the cell ID
    THEN gNb ID is extracted and returned
    """

    def test_gnb_id_from_nci(self):
        test_nci = [53588735, 53588736]
        test_digits_for_cell_id = 2
        expected_gnb_id = [209330, 209331]
        for i in range(len(expected_gnb_id)):
            with self.subTest(
                "SubTest with inputs: nci={}, digits_for_cell_id={}".format(
                    test_nci[i], test_digits_for_cell_id
                )
            ):
                action_output = common_functions.gnb_id_from_nci(
                    test_nci[i], test_digits_for_cell_id
                )
                self.assertEqual(action_output, expected_gnb_id[i])


if __name__ == "__main__":
    unittest.main()
# EOF
