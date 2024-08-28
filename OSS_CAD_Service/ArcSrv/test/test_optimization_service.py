#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import datetime
import json
import threading
import unittest
from unittest.mock import patch

from entities import database_collection_models
from entities.api_response import ResponseStatus
from entities.database_collection_models import Optimization
from fastapi import status
from helper import optimization_threading_helper
from optimization import build_solution
from services import database_service, optimization_service


class TestOptimizationService(unittest.TestCase):

    OPTIMIZATION_ID_NONE = "None"
    OPTIMIZATION_ID = "6385d9f53ced2cd471234123"
    STATUS_ERROR = "Error"
    STATUS_SUCCESS = "Success"
    STATUS_CREATED = "Created"
    STATUS_FAILED = "Failed"
    STATUS_OPTIMIZATION_ID_NOT_FOUND = (
        "No optimization instance is associated with the provided ID"
    )
    HTTP_STATUS_200 = status.HTTP_200_OK
    HTTP_STATUS_404 = status.HTTP_404_NOT_FOUND
    HTTP_STATUS_500 = status.HTTP_500_INTERNAL_SERVER_ERROR

    test_body = """
                                               {
                                                    "selected_nodes": {
                                                        "gnb_id_list": [
                                                            {
                                                                "gnb_id": 20866259,
                                                                "cm_handle": "9321F0EA5DD12A97B6F72ED2ED1ADD1D449"
                                                            },
                                                            {
                                                                "gnb_id": 208733,
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
                                                                "p_gnbdu_id": 208727,
                                                                "p_gnbdu_id": 208731
                                                            }
                                                        ]
                                                    }
                                                }
                                """

    test_gnb_dict = {
        208727: "3F0EA5DD12A97B6F72ED2ED1ADD1D449",
        208731: "07148148A84D38E0404CFAEB5CA09309",
    }
    test_unwanted_bb_links = [(208727, 208731)]
    test_mandatory_bb_links = [(208731, 208727)]
    mock_attrs = {"start.side_effect": RuntimeError}

    def tearDown(self):
        optimization_threading_helper.task_thread = threading.Thread()
        optimization_threading_helper.task_opt_id = self.OPTIMIZATION_ID_NONE
        optimization_service.configuration_instance_id_counter = 0
        optimization_service.configuration_instances_with_ids_and_data = {}

    """
        GIVEN JSON request body
        RETURNS an ID assigned after creating an optimization instance
        AND return code 200 (OK)
    """

    @patch.object(database_collection_models.Optimization, "save")
    @patch.object(database_collection_models.Gnbdus, "save")
    @patch.object(database_collection_models.Gnbdus, "objects")
    def test_create_optimization_instance_with_an_optimization_id(
        self, mocked_opt_save, mocked_gnbdu_save, mocked_q
    ):
        test_body_json = json.loads(self.test_body)
        action_output = optimization_service.create_optimization_instance(
            test_body_json
        )
        test_response_body = {
            "status": self.STATUS_SUCCESS,
            "optimization_id": action_output[0].optimization_id,
            "result": "Optimization instance created.",
        }
        test_response_code = status.HTTP_200_OK
        self.assertEqual((test_response_body, test_response_code), action_output)

    # """
    #             GIVEN JSON request body
    #             WHEN optimizationId is already assigned
    #             THEN return a failure in assigning an ID
    #             AND return code 400 (BAD_REQUEST)
    #             """
    #
    # def test_replacing_existing_optimization_id_with_new_optimization_idlimiting_multiple_assignment_of_optimization
    # _id( self, ): test_json_request_body_optimization_id_new = { "flow_id": "com.ericsson.oss.flow.arc-automation.-.
    # APIDC_145721.-.6" } test_response_body = {"status": self.STATUS_SUCCESS, "optimization_id": "APIDC_145721"}
    # test_response_code = status.HTTP_200_OK optimization_threading_helper.task_opt_id = self.OPTIMIZATION_ID_VALID
    # action_output = optimization_service.assign_optimization_id( test_json_request_body_optimization_id_new )
    #
    #     self.assertEqual((test_response_body, test_response_code), action_output)
    """
    GIVEN JSON request body (a wrong one)
    RETURNS an ID equal to None, means no optimization instance has been created
    AND a status with failure
    AND return code 400 (bad request)
    """

    def test_not_creating_an_optimization_instance_due_to_wrong_request_body(self):
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
                                    }
                                }
                """
        test_body_json = json.loads(test_body)
        test_response_body = {
            "status": self.STATUS_FAILED,
            "optimization_id": self.OPTIMIZATION_ID_NONE,
            "result": "One of the required entities needed for optimization is missing in the provided data.",
        }
        test_response_code = status.HTTP_400_BAD_REQUEST
        action_output = optimization_service.create_optimization_instance(
            test_body_json
        )
        self.assertEqual((test_response_body, test_response_code), action_output)

    """
    GIVEN an Optimization ID
    WHEN optimization has not been started
    THEN start optimization thread
    AND return message stating optimization has started
    AND return code 200 (OK)
    """

    @patch.object(optimization_service.logger, "debug")  # suppress logging messages
    @patch.object(database_collection_models.Optimization, "save")
    @patch.object(database_collection_models.Gnbdus, "save")
    @patch.object(database_collection_models.Optimization, "objects")
    @patch.object(database_collection_models.Gnbdus, "objects")
    @patch.object(
        optimization_service,
        "start_optimization",
        return_value={
            "status": ResponseStatus.SUCCESS,
            "result": "Optimization started.",
        },
    )
    def test_start_optimization(
        self,
        mocked_logger,
        mocked_opt_save,
        mocked_gnbdu_save,
        mocked_opt_q,
        mocked_gnbdu_q,
        mocked_start_optimization,
    ):
        test_body_json = json.loads(self.test_body)
        test_response = {
            "status": self.STATUS_SUCCESS,
            "result": "Optimization started.",
        }
        response = optimization_service.create_optimization_instance(test_body_json)
        action_output = optimization_service.start_optimization(
            response[0].optimization_id
        )
        self.assertEqual(test_response, action_output)

    """
        GIVEN an Optimization ID (that does not exist)
        WHEN optimization has not been started
        Return a message stating that optimization id has not been found
        AND return code 400 (Bad Request)
        """

    @patch.object(optimization_service.logger, "debug")  # suppress logging messages
    @patch.object(database_collection_models.Optimization, "objects")
    @patch.object(database_service, "get_optimization_by_id", return_value=None)
    def test_start_optimization_in_case_optimization_instance_id_not_found(
        self,
        mocked_id,
        mocked_opt_obj,
        mocked_logger,
    ):
        test_response = {
            "status": self.STATUS_ERROR,
            "result": "Optimization instance ID does not exist.",
        }
        test_response_code = status.HTTP_404_NOT_FOUND
        action_output = optimization_service.start_optimization(
            "6385d9f53ced2cd471234123"
        )
        self.assertEqual((test_response, test_response_code), action_output)

    """
    GIVEN gNb dictionary
    WHEN optimization thread is already running
    THEN raise an exception
    AND keep optimization status, message and result intact
    """

    @patch("threading.Thread", spec=True, **mock_attrs)
    @patch.object(optimization_service.logger, "exception")
    @patch.object(optimization_service.logger, "debug")  # suppress logging messages
    def test_start_optimization_thread_while_already_started(
        self, mocked_debug_logger, mocked_exception_logger, mocked_thread
    ):
        self.assertIsNone(
            optimization_service.start_optimization_thread(
                self.test_gnb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                self.OPTIMIZATION_ID,
            )
        )
        mocked_thread.assert_called_once_with(
            target=build_solution.run_optimization_service,
            args=(
                self.test_gnb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                self.OPTIMIZATION_ID,
            ),
        )
        optimization_threading_helper.task_thread.start.assert_called_once_with()
        mocked_exception_logger.assert_called_with(
            "RuntimeError: Failed to start optimization thread - already running."
        )

    """
    GIVEN gNb dictionary
    THEN create and start optimization thread
    """

    @patch("threading.Thread")
    @patch.object(optimization_service.logger, "debug")  # suppress logging messages
    @patch.object(database_collection_models.Optimization, "update")
    def test_start_optimization_thread(
        self, mocked_update, mocked_debug_logger, mocked_thread
    ):
        optimization_document = Optimization(
            status=ResponseStatus.CREATED,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
        )
        self.assertIsNone(
            optimization_service.start_optimization_thread(
                self.test_gnb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                optimization_document,
            )
        )
        mocked_thread.assert_called_once_with(
            target=build_solution.run_optimization_service,
            args=(
                self.test_gnb_dict,
                self.test_unwanted_bb_links,
                self.test_mandatory_bb_links,
                optimization_document,
            ),
        )
        optimization_threading_helper.task_thread.start.assert_called_once_with()
        optimization_document.status = ResponseStatus.OPTIMIZATION_IN_PROGRESS
        self.assertEqual(
            optimization_document.status,
            ResponseStatus.OPTIMIZATION_IN_PROGRESS,
        )

    """
    GIVEN stop optimization request with a Correct Optimization ID
    THEN stop optimization thread
    AND return message stating optimization has been stopped
    """

    @patch("helper.optimization_threading_helper.task_thread")
    @patch("helper.optimization_threading_helper.task_stop_event")
    @patch.object(database_service, "get_optimization_by_id")
    def test_stop_optimization_with_correct_optimization_id(
        self, mocked_id, mocked_task_stop_event, mocked_task_thread
    ):
        mocked_task_thread.is_alive.side_effect = 2 * [True] + [False]
        optimization_threading_helper.task_opt_id = self.OPTIMIZATION_ID
        action_output = optimization_service.stop_optimization(self.OPTIMIZATION_ID)
        mocked_task_stop_event.set.assert_called_once_with()
        self.assertEqual(3, mocked_task_thread.is_alive.call_count)
        mocked_task_stop_event.clear.assert_called_once_with()
        self.assertEqual(
            (
                {
                    "status": self.STATUS_SUCCESS,
                    "result": "Optimization stopped successfully.",
                },
                self.HTTP_STATUS_200,
            ),
            action_output,
        )

    """
    GIVEN stop optimization request with a Wrong Optimization ID
    THEN return a JSON having keys 'status' and 'result' with 'error' and 'Optimization ID not found', respectively
    """

    @patch("helper.optimization_threading_helper.task_thread")
    @patch.object(database_service, "get_optimization_by_id")
    def test_stop_optimization_with_wrong_optimization_id(
        self, mocked_id, mocked_task_thread
    ):
        mocked_task_thread.is_alive.side_effect = 2 * [True] + [False]
        action_output = optimization_service.stop_optimization(self.OPTIMIZATION_ID)
        self.assertEqual(
            (
                {
                    "status": self.STATUS_ERROR,
                    "result": "No optimization instance is associated with the provided ID.",
                },
                self.HTTP_STATUS_404,
            ),
            action_output,
        )

    """
    GIVEN get optimization status request for a wrong ID is provided.
    THEN return optimization status, message and result
    """

    @patch.object(database_service, "get_optimization_by_id", return_value=None)
    def test_get_optimization_status_when_a_wrong_id_is_provided(self, mocked_id):
        test_status = self.STATUS_OPTIMIZATION_ID_NOT_FOUND
        test_result = []
        self.assertEqual(
            (
                {"status": test_status, "result": test_result},
                self.HTTP_STATUS_404,
            ),
            optimization_service.get_optimization_status(self.OPTIMIZATION_ID),
        )

    """
    GIVEN get optimization status request of a specific assigned ID
    THEN return optimization status, message and result
    """

    @patch.object(database_service, "get_optimization_by_id")
    @patch.object(
        optimization_service,
        "get_optimization_status",
        return_value={"status": ResponseStatus.CREATED, "result": []},
    )
    def test_get_optimization_status_with_id_when_id_has_been_assigned(
        self, mocked_status, mocked_id
    ):
        optimization_document = Optimization(
            id=self.OPTIMIZATION_ID,
            status=ResponseStatus.CREATED,
            creation_date=datetime.datetime.now(),
            target_gnbdus=[{"gnbdu_id": 20866259}, {"gnbdu_id": 20866260}],
            result_links=[],
        )
        test_status = self.STATUS_CREATED
        test_result = []
        response = optimization_service.get_optimization_status(
            optimization_document.id
        )
        self.assertEqual(({"status": test_status, "result": test_result}), response)

    """
        GIVEN JSON request body
        RETURNS an ID assigned after creating an configuration instance
        AND return code 200 (OK)
    """

    def test_create_configuration_instance_with_an_configuration_id(self):
        test_body = """
                                {
                                }
                """
        test_body_json = json.loads(test_body)
        test_response_body = {
            "status": self.STATUS_SUCCESS,
            "configuration_id": 0,
            "result": "Partner configuration instance created successfully.",
        }
        test_response_code = status.HTTP_200_OK
        action_output = optimization_service.create_configuration_instance(
            test_body_json
        )
        self.assertEqual((test_response_body, test_response_code), action_output)

    """
    GIVEN get configuration status request when no ID has been assigned
    THEN return status and message
    """

    def test_get_configuration_status_when_no_id_has_been_assigned(self):
        test_status = self.STATUS_ERROR
        test_message = "No configuration instance is associated with the provided ID."
        self.assertEqual(
            ({"status": test_status, "message": test_message}, self.HTTP_STATUS_404),
            optimization_service.get_configuration_status(1),
        )

    """
        GIVEN get configuration status request of a specific assigned ID
        THEN return configuration status and message
        """

    def test_get_configuration_status_with_id_when_id_has_been_assigned(self):
        test_status = self.STATUS_SUCCESS
        test_message = "The configuration id has been found."
        optimization_service.configuration_instances_with_ids_and_data[0] = "Dummy Data"
        self.assertEqual(
            ({"status": test_status, "message": test_message}, self.HTTP_STATUS_200),
            optimization_service.get_configuration_status(0),
        )
        """
        GIVEN get configuration status request for a wrong ID when ID has already been assigned
        THEN return status and message
        """

    def test_get_configuration_status_when_a_wrong_id_is_provided(self):
        test_output = {
            "status": self.STATUS_ERROR,
            "message": "No configuration instance is associated with the provided ID.",
        }
        self.assertEqual(
            (test_output, self.HTTP_STATUS_404),
            optimization_service.get_configuration_status(1),
        )


if __name__ == "__main__":
    unittest.main()
