#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import json
import unittest
from unittest.mock import patch

from api import api_server
from entities.kpi_retrieve_post_parameters import KpiRetrievePostRequestBody
from entities.optimization_create_instance_post_parameters import (
    OptimizationCreateInstancePostRequestBody,
)
from fastapi import Response, status
from services import kpi_service, optimization_service


class TestApiServer(unittest.IsolatedAsyncioTestCase):

    HTTP_STATUS_400 = status.HTTP_400_BAD_REQUEST
    OPTIMIZATION_ID = "1"

    dummy_status_message_result = {
        "status": "dummy status",
        "message": "dummy message",
        "result": "dummy result",
    }
    dummy_status_result = {"status": "dummy status", "result": "dummy result"}
    """
    GIVEN GET optimization status request
    THEN return optimization service status
    """

    @patch.object(
        optimization_service,
        "get_optimization_status",
        return_value=(dummy_status_message_result, HTTP_STATUS_400),
    )
    async def test_get_optimize(self, mocked_get_optimization_status):
        test_response = Response()
        self.assertEqual(
            self.dummy_status_message_result,
            await api_server.get_optimize(self.OPTIMIZATION_ID, test_response),
        )
        mocked_get_optimization_status.assert_called_once_with(self.OPTIMIZATION_ID)
        self.assertEqual(self.HTTP_STATUS_400, test_response.status_code)

    """
    GIVEN POST create optimization instance request
    THEN return an optimization ID with a creation of an optimization instance
    """

    @patch.object(
        optimization_service,
        "create_optimization_instance",
        return_value=(dummy_status_result, HTTP_STATUS_400),
    )
    @patch.object(api_server.logger, "info")  # suppress logging messages
    async def test_post_create_optimization_instance(
        self, mocked_logger, mocked_create_optimization_instance
    ):
        test_body = """
                        {
                            "selected_nodes": {
                                "gnb_id_list": [
                                    {
                                        "gnb_id": 208727,
                                        "cm_handle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449"
                                    },
                                    {
                                        "gnb_id": 208731,
                                        "cm_handle": "07148148A84D38E0404CFAEB5CA09309"
                                    }
                                ]
                            },
                            "unwanted_node_pairs": {
                                "gnb_pairs_list": [
                                    {
                                        "p_gnbdu_id": 208727,
                                        "s_gnbdu_id": 208731
                                    }
                                ]
                            },
                            "mandatory_node_pairs": {
                                "gnb_pairs_list": [
                                    {
                                        "p_gnbdu_id": 208731,
                                        "s_gnbdu_id": 208727
                                    }
                                ]
                            }
                        }
        """
        test_body_json = json.loads(test_body)
        test_body_request_object = OptimizationCreateInstancePostRequestBody(
            **test_body_json
        )
        test_response = Response()
        test_status_code = (
            status.HTTP_400_BAD_REQUEST
        )  # any would do, non-default value chosen
        mocked_create_optimization_instance.return_value = (
            self.dummy_status_result,
            test_status_code,
        )
        self.assertEqual(
            self.dummy_status_result,
            await api_server.post_create_optimization_instance(
                test_body_request_object, test_response
            ),
        )
        mocked_create_optimization_instance.assert_called_once_with(
            json.loads(test_body)
        )
        self.assertEqual(self.HTTP_STATUS_400, test_response.status_code)

    """
    GIVEN POST stop optimization request
    THEN return stop optimization service status
    """

    @patch.object(
        optimization_service,
        "stop_optimization",
        return_value=(dummy_status_result, HTTP_STATUS_400),
    )
    async def test_post_stop_optimization(self, mocked_post_stop_optimization):
        test_response = Response()
        self.assertEqual(
            self.dummy_status_result,
            await api_server.post_stop_optimization(
                self.OPTIMIZATION_ID, test_response
            ),
        )
        mocked_post_stop_optimization.assert_called_once_with(self.OPTIMIZATION_ID)

    """
            GIVEN POST KPIs request
            THEN return KPIs
            """

    @patch.object(kpi_service, "retrieve_kpi", return_value=dummy_status_result)
    @patch.object(api_server.logger, "info")  # suppress logging messages
    async def test_post_retrieving_kpi(self, mocked_logger, mocked_retrieve_kpi):
        test_body = """
                                {
                                    "gnb_list": [
                                        {
                                            "gnb_id": 208727,
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
        test_body_request_object = KpiRetrievePostRequestBody(**test_body_json)
        test_response = Response()
        test_status_code = (
            status.HTTP_400_BAD_REQUEST
        )  # any would do, non-default value chosen
        mocked_retrieve_kpi.return_value = (
            self.dummy_status_result,
            test_status_code,
        )
        self.assertEqual(
            self.dummy_status_result,
            await api_server.post_retrieving_kpi(
                test_body_request_object, test_response
            ),
        )
        mocked_retrieve_kpi.assert_called_once_with(json.loads(test_body))
        self.assertEqual(test_status_code, test_response.status_code)


if __name__ == "__main__":
    unittest.main()
# EOF
