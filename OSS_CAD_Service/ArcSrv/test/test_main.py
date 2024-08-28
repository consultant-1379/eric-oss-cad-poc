#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import uvicorn
import threading
import unittest
from unittest.mock import patch

import main
from api import api_server


class TestMain(unittest.TestCase):
    mock_attrs = {"start.side_effect": RuntimeError}

    @patch("threading.Thread")
    @patch.object(main.logger, "debug")
    def test_start(self, mocked_logger, mocked_thread):
        arc_server_test = threading.Thread(target=None, args=())
        mocked_thread.return_value = arc_server_test
        main.start()
        self.assertEqual(2, mocked_logger.call_count)
        mocked_thread.assert_called_with(target=main.start_server, args=())
        self.assertTrue(arc_server_test.start.called)

    @patch.object(main.logger, "exception")
    @patch.object(main.logger, "debug")  # suppress logging messages
    @patch("threading.Thread", spec=True, **mock_attrs)
    def test_start_exception(
        self, mocked_thread, mocked_logger_debug, mocked_logger_exception
    ):
        arc_server_test = threading.Thread(target=None, args=())
        mocked_thread.return_value = arc_server_test
        self.assertIsNone(main.start())
        mocked_thread.assert_called_with(target=main.start_server, args=())
        arc_server_test.start.assert_called_once()
        mocked_logger_exception.assert_called_once()

    @patch.object(uvicorn, "run")
    @patch.object(api_server.logger, "info")  # suppress logging messages
    def test_start_server(self, mocked_logger, mocked_run):
        self.assertIsNone(main.start_server())
        mocked_run.assert_called_once_with(
            "api:app", host="0.0.0.0", port=5000, log_level="warning"
        )


if __name__ == "__main__":
    unittest.main()
