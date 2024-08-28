# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import os
import logging
from datetime import datetime
from pythonjsonlogger import jsonlogger


class CustomJsonFormatter(jsonlogger.JsonFormatter):
    def add_fields(self, log_record, record, message_dict):
        super(CustomJsonFormatter, self).add_fields(log_record, record, message_dict)
        if not log_record.get("timestamp"):
            now = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S.%fZ")
            log_record["timestamp"] = now
        if log_record.get("level"):
            log_record["severity"] = log_record["level"].lower()
        else:
            log_record["severity"] = record.levelname.lower()
        log_record["service_id"] = os.getenv('APP_NAME', 'arcoptimization')
        log_record["version"] = os.getenv('APP_VERSION', '0.0.1')


logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = CustomJsonFormatter("%(timestamp)s %(severity)s %(message)s %(name)s %(version)s %(service_id)s")
handler.setFormatter(formatter)
logger.propagate = False
logger.addHandler(handler)
logger.setLevel(logging.getLevelName(os.getenv("LOG_LEVEL", "INFO")))
