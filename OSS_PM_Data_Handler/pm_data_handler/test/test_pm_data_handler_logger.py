#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import io
import unittest
from unittest import main
from unittest.mock import patch

from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger


@patch("os.path.exists")
class TestPMDataHandlerLogger(unittest.TestCase):
    def tearDown(self):
        """
        As LoggerConfig follows the singleton pattern, deleting class instance after each test will prevent
        working on the same  instance
        """
        PMDataHandlerLogger._instances = {}

    @classmethod
    def setUpClass(cls):
        PMDataHandlerLogger._instances = {}

    """
    GIVEN a non existing Logger config path
    WHEN LoggerConfig is instantiated
    THEN Logger is set to default
    """

    @patch(
        "pm_data_handler.entities.pm_data_handler_logger.PMDataHandlerLogger.set_default_logging"
    )
    def test_providing_wrong_config_file_location(
        self, mock_default_logging, mock_os_call
    ):
        # GIVEN
        mock_os_call.return_value = False
        # WHEN
        PMDataHandlerLogger()
        # THEN
        mock_default_logging.assert_called_once()

    """
    GIVEN a correct config file location
    WHEN LoggerConfig is instantiated
    THEN set logger correctly
    """

    @patch(
        "pm_data_handler.entities.pm_data_handler_logger.PMDataHandlerLogger.customized_logger.setLogRecordFactory"
    )
    @patch("builtins.open")
    def test_logger_with_correct_config_file(
        self, mock_open, mock_set_record, mock_os_call
    ):
        # GIVEN
        mock_os_call.return_value = True
        mock_open.return_value = self.mock_valid_yaml_file()
        # WHEN
        PMDataHandlerLogger().get_logger()
        # THEN
        mock_set_record.assert_called_once()

    """
    GIVEN a correct config file path with invalid content
    WHEN LoggerConfig is instantiated
    THEN throw exception
    """

    @patch(
        "pm_data_handler.entities.pm_data_handler_logger.PMDataHandlerLogger.set_default_logging"
    )
    @patch("builtins.open")
    def test_logger_with_invalid_config_file_content(
        self, mock_open, mock_default_logging, mock_os_call
    ):
        # GIVEN
        mock_os_call.return_value = True
        mock_open.return_value = self.mock_invalid_yaml_file()
        # WHEN
        try:
            PMDataHandlerLogger().get_logger()
        # THEN
        except ValueError:
            mock_default_logging.assert_called_once()

    def mock_valid_yaml_file(self):
        return io.StringIO(
            """
                version: 1
                handlers:
                  console:
                    class: logging.StreamHandler
                    stream: 'ext://sys.stdout'
                loggers:
                  dev:
                    handlers:
                      - console
        """
        )

    def mock_invalid_yaml_file(self):
        return io.StringIO("wrongContent!")

    if __name__ == "__main__":
        main()
