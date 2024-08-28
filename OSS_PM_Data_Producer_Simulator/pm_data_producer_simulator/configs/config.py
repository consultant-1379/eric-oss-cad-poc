# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

# ABC Module create abstract classes
from abc import ABC
from typing import Final


class PMDataProducerParams(ABC):
    """
    PMDataProducerParams Abstract configuration class

    Final Static Attributes :
        TOPIC (Final str): Topic name
        SCHEMA_FILE_NAME (Final str): Avro schema file name (inside resources/schemas without .avsc extension)
        SCHEMA_REGISTRY_URL (Final str): Schema Registry URL
        BOOTSTRAP_SERVERS (Final str): Kafka broker server
        DATA_SOURCE_FILE_NAME (Final str): Name of the CSV File that contains the mocked data to be produced
        P_INTERVAL (Final float): Interval in minutes between each loop of producing the full data source file
        VERBOSE (Final bool): If set to True , the app will log more details for each sent record
    """

    TOPIC: Final = "pm_data"
    SCHEMA_FILE_NAME: Final = "pm_data"
    SCHEMA_REGISTRY_URL: Final = "http://schema-registry:8081"
    BOOTSTRAP_SERVERS: Final = "broker:29092"
    DATA_SOURCE_FILE_NAME: Final = "pm_data_from_lte"
    P_INTERVAL: Final = 15.0
    VERBOSE: Final = False
