# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import os
import time
from datetime import datetime
from uuid import uuid4

import pandas as pd
from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer

from pm_data_producer_simulator.configs.config import PMDataProducerParams
from pm_data_producer_simulator.entities.pm_data import PMData
from pm_data_producer_simulator.utils.converters import pm_data_to_dict
from pm_data_producer_simulator.utils.reporters import delivery_report


class PMDataProducer(object):
    topic: str
    schema_file_name: str
    schema_str: str
    schema_registry_url: str
    schema_registry_conf: dict
    schema_registry_client: SchemaRegistryClient
    avro_serializer: AvroSerializer
    bootstrap_servers: str
    producer_conf: dict
    producer: SerializingProducer
    p_interval: int

    """
    PMData Producer (With Avro Serialization)

    Attributes:
        topic (str): Topic's name
        schema_file_name (str): The name of the avro schema file inside resources (without .avsc)
        schema_str (str): Loaded schema from file to str format
        schema_registry_url (str) : schema registry url
        schema_registry_conf (dict): Dict containing the schema registry client configurations
        schema_registry_client (SchemaRegistryClient): Schema registry client
        avro_serializer (AvroSerializer): Avro serializer (For data serialization)
        producer_conf (dict): Dict containing the configuration of the kafka producer
        producer (SerializingProducer): Kafka Serializing Producer
        p_interval (int): Producing interval in minutes

    Note:
        The schema file must be present in /resources/schemas

    """

    def __init__(
            self,
            topic=PMDataProducerParams.TOPIC,
            schema_file_name=PMDataProducerParams.SCHEMA_FILE_NAME,
            schema_registry_url=PMDataProducerParams.SCHEMA_REGISTRY_URL,
            bootstrap_servers=PMDataProducerParams.BOOTSTRAP_SERVERS,
    ):
        print(
            datetime.now().strftime("%m/%d/%Y %r"),
            "~> Init topic \"", topic, "\" | IN PROGRESS ...",
        )
        self.topic = topic
        self.schema_file_name = schema_file_name
        schema_file_path = (
                os.getcwd()
                + "/pm_data_producer_simulator/resources/schemas/"
                + schema_file_name
                + ".avsc"
        )
        schema_file_path = schema_file_path.replace("\\", "/")
        self.schema_str = open(schema_file_path, "r").read()
        self.schema_registry_url = schema_registry_url
        self.schema_registry_conf = {"url": self.schema_registry_url}
        self.schema_registry_client = SchemaRegistryClient(self.schema_registry_conf)
        self.avro_serializer = AvroSerializer(
            self.schema_registry_client, self.schema_str, pm_data_to_dict
        )
        self.bootstrap_servers = bootstrap_servers
        self.producer_conf = {
            "bootstrap.servers": self.bootstrap_servers,
            "key.serializer": StringSerializer("utf_8"),
            "value.serializer": self.avro_serializer,
        }
        self.producer = SerializingProducer(self.producer_conf)
        print(
            datetime.now().strftime("%m/%d/%Y %r"),
            "~> Producer is publishing on \"", topic, "\" topic | DONE",
        )
    """
          PMData Producer load_data_from_source_file() method

          Desc:
              This method will loads the csv file that contains the pm data records , creates corresponding dataframe,
              delete the unneeded index column and return it.

          Args:
              data_source_file (str): csv file name without extension (without .csv)

          Returns:
              clean dataframe containing the full pm data records.

          Note:
              The data source file must be present in /resources/data/

       """

    @staticmethod
    def load_data_from_source_file(data_source_file):
        # Load data from csv file to pandas dataframe
        try:
            pm_data_records = pd.read_csv(
                (os.getcwd())
                + "/pm_data_producer_simulator/resources/data/"
                + data_source_file
                + ".csv"
            ).replace("\\", "/")
            # Deleting index column
            pm_data_records.drop("index", inplace=True, axis=1)
            return pm_data_records

        except BaseException as e:
            print(datetime.now().strftime("%m/%d/%Y %r"), "~> Exception : {}".format(e))
            return None

    """
       PMData Producer produce() method

       Desc:
           This method takes a pm_data record (1Row) and produce it to the cluster using the initial provided topic name
           It uses a random generated unique key

       Args:
           pm_data (PMData): PMData record
    """

    def produce(self, pm_data):
        # Check if VERBOSE Config Attr is set to true
        # If yes then produce and get back delivery report logs
        if PMDataProducerParams.VERBOSE:
            self.producer.produce(
                topic=self.topic,
                key=str(uuid4()),
                value=pm_data,
                on_delivery=delivery_report,
            )
        else:
            self.producer.produce(topic=self.topic, key=str(uuid4()), value=pm_data)

    """
       PMData Producer produce() method

       Desc:
           This method will call the load data source and prepare the data frame
           After that, Each {p_interval} minutes, we lunch the process of sending the full content of pm data file
           record by record (line by line).

       Args:
           data_source_file (str): PMData csv file name (without .csv)
           p_interval (int): Producing interval in minutes

       Note:
           The data source file must be present in /resources/data/
    """

    def run(
            self,
            data_source_file=PMDataProducerParams.DATA_SOURCE_FILE_NAME,
            p_interval=PMDataProducerParams.P_INTERVAL,
    ):
        print(
            datetime.now().strftime("%m/%d/%Y %r"),
            "~> Importing PM Simulation Data source file | IN PROGRESS ...",
        )
        pm_data_records_df = self.load_data_from_source_file(data_source_file)
        print(
            datetime.now().strftime("%m/%d/%Y %r"),
            "~> Importing PM Simulation Data source file | DONE",
        )
        self.p_interval = p_interval
        # Iterate over the pm data records and produce each row
        print(
            datetime.now().strftime("%m/%d/%Y %r"),
            "~> Producer | Simulator is launched successfully | LAUNCHED",
        )
        # Initialize Loops over data source file count
        n_loops = 0
        while True:
            n_loops = n_loops + 1
            print(
                datetime.now().strftime("%m/%d/%Y %r"),
                "~> Sending PM Data file rows | Loop Number",
                n_loops,
                "| IN PROGRESS ...",
            )
            for index, row in pm_data_records_df.iterrows():
                # Serve on_delivery callbacks from previous calls to produce()
                # Get the delivery reports of previous sent pm_data record
                self.producer.poll(0)
                # Create the corresponding PMData object
                # Note : The p_time attribute is initialized by default with current system data & time
                pm_data = PMData(
                    row["gnbId"],
                    row["cellId"],
                    row["pmMacVolDlSCellExt"],
                    row["pmMacVolDl"],
                    row["pmMacVolDlDrb"],
                    row["pmActiveUeDlSum"],
                    row["pmMacRBSymAvailDl"],
                    row["pmMacRBSymUsedPdschTypeA"],
                    row["gnbIdLength"],
                    row["sNCI"],
                )
                # Produce the PMData Record
                self.produce(pm_data)
                # Delete the created instance of PM Data Record
                del pm_data
            # Flushing records (wait until delivery report of all pm data records is acknowledged)
            self.producer.flush()
            # Wait for {p_interval} minutes until next generation of pm data.
            print(
                datetime.now().strftime("%m/%d/%Y %r"),
                "~> Sending PM Data file rows | Loop Number",
                n_loops,
                "| DONE",
            )
            print(
                datetime.now().strftime("%m/%d/%Y %r"),
                "~> Sending PM Data file rows | Loop Number",
                (n_loops + 1),
                "| SCHEDULED IN",
                self.p_interval,
                "MINS ",
            )
            time.sleep(self.p_interval * 60)
