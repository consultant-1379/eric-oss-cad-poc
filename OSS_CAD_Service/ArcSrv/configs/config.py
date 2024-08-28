# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import os
import re
from typing import Mapping


class ConfigHandler(object):
    VALUE_PATTERN = re.compile("\\${(?P<env_key>[a-zA-Z_]+[a-zA-Z0-9_]*)(?::(?P<default_value>.*))?}")

    def __init__(self, config: Mapping):
        self.__config = config

    def __getattr__(self, key):
        if key in self.__config:
            value = self.__config[key]
            if m := re.match(self.VALUE_PATTERN, str(value)):
                value = os.environ.get(m.group("env_key"), m.group("default_value"))
            return value
        else:
            raise AttributeError(f"{self.__name__} has no attribute '{key}'")
