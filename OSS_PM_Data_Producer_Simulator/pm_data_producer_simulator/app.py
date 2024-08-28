# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from datetime import datetime

from pm_data_producer_simulator.configs.config import PMDataProducerParams
from pm_data_producer_simulator.entities.pm_data_producer import PMDataProducer


def launch_simulator(
    topic=PMDataProducerParams.TOPIC,
    schema_file_name=PMDataProducerParams.SCHEMA_FILE_NAME,
    schema_registry_url=PMDataProducerParams.SCHEMA_REGISTRY_URL,
    bootstrap_servers=PMDataProducerParams.BOOTSTRAP_SERVERS,
    data_source_file=PMDataProducerParams.DATA_SOURCE_FILE_NAME,
):
    pm_data_producer = PMDataProducer(
        topic, schema_file_name, schema_registry_url, bootstrap_servers
    )
    print(
        datetime.now().strftime("%m/%d/%Y %r"),
        "~> Setting up connections to kafka broker and schema registry | DONE",
    )
    print(
        datetime.now().strftime("%m/%d/%Y %r"),
        "~> Producer | Successfully connected to the environment | DONE",
    )
    print(
        datetime.now().strftime("%m/%d/%Y %r"),
        "~> Producer | Initialized successfully | READY",
    )
    pm_data_producer.run(data_source_file, PMDataProducerParams.P_INTERVAL)
