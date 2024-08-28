#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022

import logging.config
import os

import yaml

from pm_data_handler.configs.logger_config import LoggerParams
from pm_data_handler.utils.meta import SingletonMeta


class PMDataHandlerLogger(object, metaclass=SingletonMeta):
    old_factory = logging.getLogRecordFactory()
    customized_logger: logging = logging

    """
        PM Data Handler Logger

        Args:
            customized_logger (logging): the customized  logger that will be used in the entire service
        Description:
            Create a Customized  logger for our PMDataHandler service which is mainly based on a yaml config file

        Purpose of the Singleton Pattern:
            Setting the logger each time we instantiate  this class  can be  considered as heavy non-needed operation
            as we need to rely on the same logger configs for the entire  service.
    """

    def __init__(
        self,
        log_cfg_path=LoggerParams.DEFAULT_CONFIG_PATH,
    ):
        """
        Reads the logging config file associated with  the `log_cfg_path``
        Setting logger based on the read config file's content
        Fallback to the default_logger if any exception is raised
        """

        if os.path.exists(log_cfg_path):
            with open(log_cfg_path, "rt") as cfg_file:
                try:
                    config = yaml.safe_load(cfg_file.read())
                    self.customized_logger.config.dictConfig(config)
                    self.customized_logger.setLogRecordFactory(self.get_record_factory)
                except ValueError:
                    self.set_default_logging()
                    self.customized_logger.warning(
                        "Error with file, using Default logging",
                        exc_info=True,
                    )
        else:
            self.set_default_logging()
            self.customized_logger.warning(
                "Config file not found, using Default logging"
            )

    def get_record_factory(self, *args, **kwargs):
        """
        Add custom fields to Python log formatter string defined in **cfg.yaml**
        :return: configured record_factory
        """
        record = self.old_factory(*args, **kwargs)
        record.service_id = LoggerParams.LOGGER_FORMATTER_FIELD_SERVICE_ID
        record.version = LoggerParams.LOGGER_FORMATTER_FIELD_APP_VERSION

        return record

    def set_default_logging(self):
        """
        Configure the default logger
        """
        self.customized_logger.basicConfig(level=LoggerParams.DEFAULT_LEVEL)

    def get_logger(self, env_name=LoggerParams.DEFAULT_ENV):
        """
        Set customized logger based on passed env variable
        Prevent duplicate logs  behavior
        :return: final configured logger
        """
        logger = self.customized_logger.getLogger(env_name)  # Get logger based on env
        # prevent duplicated logs
        if logger.hasHandlers():
            # Logger is already configured, remove all handlers
            logger.handlers = []

        return logger
