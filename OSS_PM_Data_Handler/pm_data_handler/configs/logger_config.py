# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging
from abc import ABC, abstractmethod
from typing import Final


class LoggerParams(ABC):
    """
    Final Static Attributes :

     DEFAULT_LEVEL (Final Logging Level): The default level used when the setup function is failing
     DEFAULT_CONFIG_PATH (Final string): Path to the logging config file  used to set up the logger
     DEFAULT_ENV (Final string): the environment used in the loggerConfig file to select the correct Handler
     LOGGER_FORMATTER_FIELD_SERVICE_ID (Final string): PM Data Handler service name
     LOGGER_FORMATTER_FIELD_APP_VERSION (Final string):  PM Data Handler current version

    """

    DEFAULT_LEVEL: Final = logging.INFO
    DEFAULT_CONFIG_PATH: Final = "/PM_Data_Handler/pm_data_handler/resources/cfg.yaml"
    DEFAULT_ENV: Final = "dev"
    LOGGER_FORMATTER_FIELD_SERVICE_ID: Final = "pmDataHandler"
    LOGGER_FORMATTER_FIELD_APP_VERSION: Final = "0.0.1"

    """
        PMDataHandler logger config Parameters - abstract_method() -> abstract method

        Desc:
            Prevent instantiating of this configuration class
    """

    @abstractmethod
    def abstract_method(self):
        pass
