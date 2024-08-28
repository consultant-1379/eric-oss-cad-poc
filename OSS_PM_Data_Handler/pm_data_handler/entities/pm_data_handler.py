# coding: utf-8
#
# Copyright Ericsson (c) 2022
from confluent_kafka.error import (
    ConsumeError,
    KeyDeserializationError,
    ValueDeserializationError,
)
from confluent_kafka.schema_registry.avro import AvroDeserializer
from confluent_kafka.serialization import StringDeserializer
from pymongo.errors import PyMongoError, WriteError

from pm_data_handler.configs.kafka_config import KafkaParams
from pm_data_handler.entities.deserializing_cad_consumer import DeserializingCadConsumer
from pm_data_handler.entities.mongo_db_cad_client import MongoDBCadClient
from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger
from pm_data_handler.entities.schema_registry_cad_client import SchemaRegistryCadClient
from pm_data_handler.entities.transformer import PMDataTransformer
from pm_data_handler.utils.meta import SingletonMeta


class PMDataHandler(object, metaclass=SingletonMeta):
    RUNNING = True
    schema_registry_cad_client: SchemaRegistryCadClient
    deserializing_cad_consumer: DeserializingCadConsumer
    mongo_db_cad_client: MongoDBCadClient
    pm_data_transformer: PMDataTransformer
    logger = PMDataHandlerLogger().get_logger("dev")

    """
           PM Data Handler

           Desc:
               PM Data Handler Module is the final orchestrator (Manager Class)

               The main role of PM Data Handler is collecting Cad needed PM Data Records, filter based on
               gnb_id and mo, deserialize it, transform it and persist it into mongoDB timeseries Collection

    """

    def __init__(self):
        # Instantiating and setting PMDataHandler global logger

        self.logger.info("Init PMDataHandler process")

        # Instantiating MongoDB Cad Client
        self.mongo_db_cad_client = MongoDBCadClient()
        # Instantiating Schema Registry Cad Client
        self.schema_registry_cad_client = SchemaRegistryCadClient()
        consumer_conf = self.get_kafka_configs()
        # Instantiating Deserializing Cad Consumer
        self.deserializing_cad_consumer = DeserializingCadConsumer(consumer_conf)
        # Subscribe to the topic of PM Data
        self.deserializing_cad_consumer.subscribe([KafkaParams.TOPIC])
        # Instantiating PM Data Transformer
        self.pm_data_transformer = PMDataTransformer()

    def run(self):

        self.logger.info("Start PM Data Handler Collection process")

        while self.RUNNING:
            try:
                message = self.handle_poll_deserialize_and_filter_kafka_msg()
            except JustContinueTheMainLoop:
                continue
            else:
                if message is None:
                    continue

                try:
                    self.handle_kafka_message_value(message)
                except JustContinueTheMainLoop:
                    continue

    def handle_poll_deserialize_and_filter_kafka_msg(self):

        try:
            message = self.deserializing_cad_consumer.poll_and_deserialize_and_filter(
                timeout=KafkaParams.TIME_OUT
            )
        except RuntimeError:
            self.logger.warning(
                "PMDataHandler, An error occurred While polling a message using the consumer.",
                exc_info=True,
            )
            raise JustContinueTheMainLoop
        except ValueDeserializationError as exp:
            self.logger.warning(
                "PMDataHandler, An error occurred while secondly deserializing KafkaMsg:{}, exception={}",
                exp.kafka_message,
                exc_info=True,
            )
            raise JustContinueTheMainLoop
        except KeyDeserializationError as exp:
            self.logger.warning(
                "PMDataHandler, An error occurred while deserializing kafka msg key KafkaMsgKey:{}, "
                "exception:{}",
                exp.kafka_message,
                exc_info=True,
            )
            raise JustContinueTheMainLoop

        except ConsumeError as exp:
            self.logger.warning(
                "PMDataHandler, An error occurred while reading KafkaMsg:{}, exception:{}",
                exp.kafka_message,
                exc_info=True,
            )
            raise JustContinueTheMainLoop
        except Exception as exp:
            self.logger.warning(
                "PMDataHandler, a critical exception happened while running the service, exception:",
                exc_info=True,
            )
            self.mongo_db_cad_client.close()
            self.deserializing_cad_consumer.close()
            raise exp
        else:
            return message

    def handle_insert_cad_needed_pm_data(self, cad_needed_pm_data_record):
        try:
            self.mongo_db_cad_client.insert(cad_needed_pm_data_record)
        except WriteError:
            self.logger.warning(
                "PMDataHandler, the passed record to MongoDB has a  wrong type, exception:",
                exc_info=True,
            )
            raise JustContinueTheMainLoop

        except PyMongoError:
            self.logger.warning(
                "PMDataHandler, An error occurred while inserting CAD pm data to mongoDB, "
                "exception:",
                exc_info=True,
            )
            raise JustContinueTheMainLoop

        except Exception as exp:
            self.logger.warning(
                "PMDataHandler, a critical exception happened while running the service, exception:",
                exc_info=True,
            )
            self.mongo_db_cad_client.close()
            self.deserializing_cad_consumer.close()
            raise exp

    def handle_pm_data_transformation(self, full_pm_data_record):
        try:
            cad_needed_pm_data_record = (
                self.pm_data_transformer.extract_cad_pm_data_from_full_pm_data(
                    self.pm_data_transformer, full_pm_data_record
                )
            )
        except KeyError:
            self.logger.warning(
                "PMDataHandler, An error occurred while extracting CAD pm data from the full pm data "
                "record, exception:",
                exc_info=True,
            )
            raise JustContinueTheMainLoop
        else:
            return cad_needed_pm_data_record

    def handle_kafka_message_value(self, message):

        full_pm_data_record = message.value()

        if full_pm_data_record is not None:
            try:
                cad_needed_pm_data_record = self.handle_pm_data_transformation(
                    full_pm_data_record
                )
            except JustContinueTheMainLoop:
                raise JustContinueTheMainLoop
            else:
                try:
                    self.handle_insert_cad_needed_pm_data(cad_needed_pm_data_record)
                except JustContinueTheMainLoop:
                    raise JustContinueTheMainLoop

    def get_kafka_configs(self):
        # Prepare configuration dict for the Deserializing Cad Consumer
        kafka_key_deserializer = StringDeserializer("utf_8")
        kafka_value_deserializer = AvroDeserializer(
            self.schema_registry_cad_client,
            self.schema_registry_cad_client.latest_registered_schema.schema_str,
        )
        return {
            "bootstrap.servers": KafkaParams.BOOTSTRAP_SERVERS,
            "key.deserializer": kafka_key_deserializer,
            "value.deserializer": kafka_value_deserializer,
            "group.id": KafkaParams.GROUP_ID,
            "auto.offset.reset": KafkaParams.AUTO_OFFSET_RESET,
        }


class JustContinueTheMainLoop(Exception):
    pass
