#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import unittest
from unittest import TestCase

from pm_data_handler.entities.transformer import PMDataTransformer


class TestPMDataTransformer(TestCase):
    transformer: PMDataTransformer = PMDataTransformer()

    """
    GIVEN correct pm_record
    WHEN extracting cad pm data from the full data
    THEN ensure mapping is done correctly
    """

    def test_pm_data_extraction_with_accepted_record(self):
        # GIVEN
        mocked_full_pm_record = self.merge_dict(
            self.mock_obliged_part(), dict({"cell_id": 123, "gnb_id": 123})
        )
        # WHEN
        pm_data = self.transformer.extract_cad_pm_data_from_full_pm_data(
            self.transformer, mocked_full_pm_record
        )
        # THEN
        self.assertEqual(pm_data.metadata.cell_id, mocked_full_pm_record.get("cell_id"))
        self.assertEqual(pm_data.metadata.gnb_id, mocked_full_pm_record.get("gnb_id"))

    """
    GIVEN pm_record with missing cell_id
    WHEN extracting cad pm data from the full data
    THEN cell_id is set correctly
    """

    def test_pm_data_extraction_with_missing_cell_id(self):
        # GIVEN
        mocked_dict = self.merge_dict(
            self.mock_obliged_part(), dict({"gnb_id": 123, "s_n_c_i": 1123})
        )
        # WHEN
        pm_data = self.transformer.extract_cad_pm_data_from_full_pm_data(
            self.transformer, mocked_dict
        )
        # THEN
        self.assertEqual(pm_data.metadata.cell_id, 1000)

    """
    GIVEN pm_record with missing cell_id and snci
    WHEN extracting cad pm data from the full data
    THEN keyError is raised
    """

    def test_pm_data_extraction_with_missing_cell_id_and_snci(self):
        mocked_dict = self.merge_dict(self.mock_obliged_part(), dict({"gnb_id": 123}))
        try:
            self.transformer.extract_cad_pm_data_from_full_pm_data(
                self.transformer, mocked_dict
            )
        except KeyError:
            self.assertRaises(KeyError)

    """
    GIVEN pm_record with missing gnbid
    WHEN extracting cad pm data from the full data
    THEN cell_id and gnb_id are set correctly
    """

    def test_pm_data_extraction_with_missing_gnbid(self):
        # GIVEN
        mocked_dict = self.merge_dict(
            self.mock_obliged_part(), dict({"cell_id": 123, "s_n_c_i": 1123})
        )
        # WHEN
        pm_data = self.transformer.extract_cad_pm_data_from_full_pm_data(
            self.transformer, mocked_dict
        )
        # THEN
        self.assertEqual(pm_data.metadata.cell_id, 123)
        self.assertEqual(pm_data.metadata.gnb_id, 1000)

    """
     GIVEN pm_record with missing gnbid and snci
     WHEN extracting cad pm data from the full data
     THEN keyError is raised
     """

    def test_pm_data_extraction_with_missing_gnbid_and_missing_snci(self):
        # GIVEN
        mocked_dict = self.merge_dict(self.mock_obliged_part(), dict({"cell_id": 123}))
        # WHEN
        try:
            self.transformer.extract_cad_pm_data_from_full_pm_data(
                self.transformer, mocked_dict
            )
        # THEN
        except KeyError:
            self.assertRaises(KeyError)

    """
    GIVEN pm_record with missing gnbid and cellid
    WHEN extracting cad pm data from the full data
    THEN cell_id and gnb_id are set correctly
    """

    def test_pm_data_extraction_with_missing_gnbid_and_cellid(self):
        # GIVEN
        mocked_dict = self.merge_dict(
            self.mock_obliged_part(), dict({"gnb_id_len": 20, "s_n_c_i": 19420140448})
        )
        # WHEN
        pm_data = self.transformer.extract_cad_pm_data_from_full_pm_data(
            self.transformer, mocked_dict
        )
        # THEN
        self.assertEqual(pm_data.metadata.gnb_id, 296327.0)
        self.assertEqual(pm_data.metadata.cell_id, 19419844121.0)

    """
    GIVEN pm_record with missing gnbid, snci, gnbidlen and cellid
    WHEN extracting cad pm data from the full data
    THEN keyError is raised
    """

    def test_pm_data_extraction_with_missing_gnbid_snci_gnbidlen_and_cellid(self):
        try:
            self.transformer.extract_cad_pm_data_from_full_pm_data(
                self.transformer, self.mock_obliged_part()
            )
        except KeyError:
            self.assertRaises(KeyError)

    """
    GIVEN Singleton pattern
    WHEN creating two different instances
    THEN both instances should be  equal
    """

    def test_singleton_pattern(self):
        # WHEN
        pm_transformer_instance_1 = PMDataTransformer()
        pm_transformer_instance_2 = PMDataTransformer()
        # THEN
        self.assertEqual(pm_transformer_instance_1, pm_transformer_instance_2)

    """
     GIVEN pm_record with at least one missing pm counter
     WHEN extracting cad pm data from the full data
     THEN keyError is raised
     """

    def test_full_pm_data_record_contains_no_such_pm_counter_value(self):
        # GIVEN
        full_pm_data_record = {"gnb_id": 1, "cell_id": 2}
        # WHEN
        try:
            self.transformer.extract_cad_pm_data_from_full_pm_data(
                self.transformer, full_pm_data_record
            )
        # THEN
        except KeyError:
            self.assertRaises(KeyError)

    def mock_obliged_part(self):
        return dict(
            {
                "pm_mac_vol_dl_s_cell_ext": 20442253104,
                "pm_mac_vol_dl": 2044225310,
                "pm_mac_vol_dl_drb": 2044225310,
                "pm_active_ue_dl_sum": 20442253104,
                "pm_mac_r_b_sym_avail_dl": 20442253104,
                "pm_mac_r_b_sym_used_pdsch_type_a": 20442253104,
            }
        )

    def merge_dict(self, dict1, dict2):
        res = {**dict1, **dict2}
        return res

    if __name__ == "__main__":
        unittest.main()
