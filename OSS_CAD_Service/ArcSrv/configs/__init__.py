# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import os
import sys
import yaml
from dotenv import load_dotenv, dotenv_values
from types import ModuleType
from typing import Final
from functools import cached_property

from .config import ConfigHandler
from .logging import logger

load_dotenv(override=False)

mongo_db_config = None
object_store_config = None
optimization_config = None
environ_config = None
topology_config = None
cm_config = None


class Configs(ModuleType):
    CLIENT_CONFIG: Final = "client_config.yaml"
    MONGO_DB_CONFIG_FILE: Final = "mongo_db_config.yaml"
    REPORT_CONFIG_FILE: Final = "report_config.yaml"

    @property
    def logger(self):
        return logger

    @property
    def environ_config(self):
        return {
            **dotenv_values(".env"),
            **os.environ
        }

    @cached_property
    def mongo_db_config(self):
        return self.__read_yaml_config(self.MONGO_DB_CONFIG_FILE, "mongodb")

    @cached_property
    def object_store_config(self):
        return self.__read_yaml_config(self.REPORT_CONFIG_FILE, "object_storage")

    @cached_property
    def optimization_config(self):
        return self.__read_yaml_config(self.REPORT_CONFIG_FILE, "optimization")

    @cached_property
    def topology_config(self):
        return self.__read_yaml_config(self.CLIENT_CONFIG, "topology")

    @cached_property
    def cm_config(self):
        return self.__read_yaml_config(self.CLIENT_CONFIG, "cm")

    @staticmethod
    def __read_yaml_config(config_file, sub_category=None):
        with open(os.path.join(os.path.dirname(__file__), config_file), "r") as stream:
            try:
                raw_config = yaml.safe_load(stream)
                if sub_category:
                    raw_config = raw_config[sub_category]
                return ConfigHandler(raw_config)
            except yaml.YAMLError as exc:
                logger.error(exc)


sys.modules[__name__] = Configs(__name__)
