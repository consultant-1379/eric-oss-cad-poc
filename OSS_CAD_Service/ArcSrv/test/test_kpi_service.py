#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import json
import unittest

from fastapi import status
from services import kpi_service


class TestKpiService(unittest.TestCase):
    mock_attrs = {"start.side_effect": RuntimeError}
    """
           GIVEN JSON request body
           RETURNS the result in success, along with a map of KpiData
           AND return code 200 (OK)
       """

    def test_retrieve_kpi(self):
        test_body = """
                               {
                                   "gnb_list": [
                                       {
                                           "gnb_id": 208728,
                                           "cm_handle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449"
                                       },
                                       {
                                           "gnb_id": 208731,
                                           "cm_handle": "07148148A84D38E0404CFAEB5CA09309"
                                       }
                                   ]
                               }
               """
        test_body_json = json.loads(test_body)
        test_response_body = {
            "status": "Success",
            "result": "KPIs for the provided data has been retrieved successfully.",
            "kpi_data": {"KPI1": 879, "KPI2": 545, "KPI3": 479},
        }
        test_response_code = status.HTTP_200_OK
        action_output = kpi_service.retrieve_kpi(test_body_json)
        self.assertEqual(test_response_body["status"], action_output[0].status)
        self.assertEqual(test_response_body["result"], action_output[0].result)
        self.assertEqual(test_response_code, action_output[1])

        self.assertTrue(type(action_output[0].kpi_data["KPI1"]) is int)
        self.assertTrue(type(action_output[0].kpi_data["KPI2"]) is int)
        self.assertTrue(type(action_output[0].kpi_data["KPI3"]) is int)

    """
    GIVEN JSON request body (a wrong one)
    RETURN a status with failure code 400 (bad request)
    """

    def test_not_retrieve_kpi_due_to_wrong_request_body(self):
        test_body = """
                               {
                                   "gnb_list": [
                                       {
                                           "gnb_idX": 208728,
                                           "cm_handle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449"
                                       },
                                       {
                                           "gnb_idY": 208731,
                                           "cm_handle": "07148148A84D38E0404CFAEB5CA09309"
                                       }
                                   ]
                               }
               """
        test_body_json = json.loads(test_body)
        test_response_body = {
            "status": "Failed",
            "result": "One of the required entities needed for retrieving KPIs is missing in the provided data.",
            "kpi_data": {},
        }
        test_response_code = status.HTTP_400_BAD_REQUEST
        action_output = kpi_service.retrieve_kpi(test_body_json)
        self.assertEqual((test_response_body, test_response_code), action_output)


if __name__ == "__main__":
    unittest.main()
# EOF
