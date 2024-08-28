# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from datetime import datetime

from pm_data_producer_simulator.app import launch_simulator
from pm_data_producer_simulator.configs.config import PMDataProducerParams

if __name__ == "__main__":
    print(
        datetime.now().strftime("%m/%d/%Y %r"),
        "~> Setting up connections with kafka broker and schema registry | IN PROGRESS ...",
    )
    # Launch the producer simulator
    launch_simulator(
        topic=PMDataProducerParams.TOPIC,
        schema_file_name=PMDataProducerParams.SCHEMA_FILE_NAME,
        schema_registry_url=PMDataProducerParams.SCHEMA_REGISTRY_URL,
        bootstrap_servers=PMDataProducerParams.BOOTSTRAP_SERVERS,
        data_source_file=PMDataProducerParams.DATA_SOURCE_FILE_NAME,
    )
