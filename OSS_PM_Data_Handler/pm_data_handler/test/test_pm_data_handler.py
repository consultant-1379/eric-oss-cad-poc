#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import unittest
from unittest import TestCase
from unittest.mock import Mock, PropertyMock, patch

from confluent_kafka import Message
from confluent_kafka.error import ConsumeError
from pymongo.errors import PyMongoError

from pm_data_handler.entities import pm_data_handler
from pm_data_handler.entities.pm_data_handler import PMDataHandler


class TestPMDataHandler(TestCase):
    @classmethod
    def setUpClass(cls):
        PMDataHandler._instances = {}

    def tearDown(self):
        PMDataHandler._instances = {}

    """
    WHEN pm-data-handler init is called
    THEN all internal calls should be checked
    """

    @patch.object(pm_data_handler.PMDataHandler.logger, "info")
    @patch("pm_data_handler.entities.pm_data_handler.PMDataTransformer")
    @patch("pm_data_handler.entities.pm_data_handler.DeserializingCadConsumer")
    @patch("pm_data_handler.entities.pm_data_handler.SchemaRegistryCadClient")
    @patch("pm_data_handler.entities.pm_data_handler.MongoDBCadClient")
    @patch.object(PMDataHandler, "get_kafka_configs")
    def test_pm_data_handler_init_method(
        self,
        mock_kafka_meth,
        mock_mongo,
        mock_schema,
        mock_des_cad_consumer,
        mock_transformer,
        mock_logger,
    ):
        # GIVEN
        mocked_configs = self.mock_kafka_config()
        mock_kafka_meth.return_value = mocked_configs
        # WHEN
        pm_data_instance = PMDataHandler()
        # THEN

        mock_kafka_meth.assert_called_once()
        mock_logger.assert_called_once()
        mock_schema.assert_called_once()
        mock_mongo.assert_called_once()
        mock_des_cad_consumer.assert_called_once()
        mock_transformer.assert_called_once()
        self.assertEqual(pm_data_instance.get_kafka_configs(), mocked_configs)

    """
    GIVEN Singleton pattern
    WHEN creating two different instances
    THEN both instances should be equal
    """

    def test_pm_data_handler_singleton_pattern(self):
        # WHEN
        pm_instance_1 = self.mock_init()
        pm_instance_2 = self.mock_init()
        # THEN
        self.assertEqual(pm_instance_1, pm_instance_2)

    """
    GIVEN None message returned from kafka topic
    WHEN running PMDataHandler
    THEN check for another new message
    """

    def test_run_method_given_none_msg_retrieved_from_kafka_topic(self):
        # GIVEN
        self.mock_running_property()
        pm_data_instance = self.mock_init()
        pm_data_instance.deserializing_cad_consumer.poll_and_deserialize_and_filter.side_effect = (
            None
        )
        # WHEN
        pm_data_instance.run()

    """
        GIVEN RuntimeError thrown when polling for a message
        WHEN poll_deserialize_and_filter_kafka_msg
        THEN deserializing_cad_consumer should not be closed
        """

    @patch.object(pm_data_handler.PMDataHandler.logger, "warning")
    def test_run_method_given_runtime_exception_raised_when_polling_for_message_with_consumer(
        self, mocked_logger
    ):
        # GIVEN
        self.mock_running_property()
        pm_data_instance = self.mock_init()
        pm_data_instance.deserializing_cad_consumer.poll_and_deserialize_and_filter.side_effect = (
            RuntimeError
        )
        # WHEN
        pm_data_instance.run()

        # THEN
        mocked_logger.assert_called_once()
        pm_data_instance.deserializing_cad_consumer.close.assert_not_called()
        pm_data_instance.mongo_db_cad_client.close.assert_not_called()

    """
    GIVEN ConsumeError thrown when extracting pm data from full data
    WHEN poll_deserialize_and_filter_kafka_msg
    THEN deserializing_cad_consumer should not be closed
    """

    def test_run_method_given_consumeerror_exception_raised_when_poll_deserialize_and_filter_kafka_msg(
        self,
    ):
        # GIVEN
        self.mock_running_property()
        pm_data_instance = self.mock_init()
        pm_data_instance.deserializing_cad_consumer.poll_and_deserialize_and_filter.side_effect = ConsumeError(
            kafka_error=None, kafka_message="test_msg"
        )

        # WHEN
        pm_data_instance.run()

        # THEN
        pm_data_instance.deserializing_cad_consumer.close.assert_not_called()

    """
    GIVEN ConsumeError thrown when extracting pm data from full data
    WHEN poll_deserialize_and_filter_kafka_msg
    THEN deserializing_cad_consumer should not be closed
    """

    def test_run_method_given_keyerror_exception_raised_when_extract_cad_pm_data_from_the_full_pm_data(
        self,
    ):
        # GIVEN
        self.mock_running_property()
        pm_data_instance = self.mock_init()
        mocked_msg = self.mock_kafka_msg()
        pm_data_instance.pm_data_transformer.extract_cad_pm_data_from_full_pm_data.side_effect = (
            KeyError
        )
        mocked_msg.value.return_value = "{mocked_value}"
        # WHEN
        pm_data_instance.run()

        # THEN
        pm_data_instance.deserializing_cad_consumer.close.assert_not_called()

    """
    GIVEN PyMongoError thrown when inserting data into MongoDB
    WHEN poll_deserialize_and_filter_kafka_msg
    THEN deserializing_cad_consumer should not be closed
    """

    def test_run_method_given_pymongoerror_exception_raised_when_insert_kafka_msg(
        self,
    ):
        # GIVEN
        self.mock_running_property()
        pm_data_instance = self.mock_init()
        self.mock_kafka_msg()
        pm_data_instance.mongo_db_cad_client.insert.side_effect = PyMongoError
        # WHEN
        pm_data_instance.run()

        # THEN
        pm_data_instance.deserializing_cad_consumer.close.assert_not_called()

    def test_run_method_given_critical_exception_raised_when_run_then_shut_down_service(
        self,
    ):
        # GIVEN
        PMDataHandler.RUNNING = True
        pm_data_instance = self.mock_init()
        pm_data_instance.deserializing_cad_consumer.poll_and_deserialize_and_filter.side_effect = (
            MemoryError
        )
        # WHEN
        try:
            pm_data_instance.run()
            self.fail()
        except MemoryError:
            # THEN
            pm_data_instance.mongo_db_cad_client.close.assert_called()
            pm_data_instance.deserializing_cad_consumer.close.assert_called()

    """
    WHEN retrieving kafka configs
    THEN return correct configs values
    """

    @patch("pm_data_handler.entities.pm_data_handler.StringDeserializer")
    @patch("pm_data_handler.entities.pm_data_handler.AvroDeserializer")
    def test_retrieve_kafka_configs_method(self, mock_avro_des, mock_string_des):
        # GIVEN
        expected_length = 5
        pm_handler = self.mock_init()
        # WHEN
        actual_value = pm_handler.get_kafka_configs()

        # THEN
        self.assertEqual(len(actual_value), expected_length)
        mock_string_des.assert_called_once_with("utf_8")
        mock_avro_des.assert_called_once_with(
            pm_handler.schema_registry_cad_client,
            pm_handler.schema_registry_cad_client.latest_registered_schema.schema_str,
        )

    @patch("pm_data_handler.entities.pm_data_handler.PMDataHandlerLogger")
    @patch("pm_data_handler.entities.pm_data_handler.PMDataTransformer")
    @patch("pm_data_handler.entities.pm_data_handler.DeserializingCadConsumer")
    @patch("pm_data_handler.entities.pm_data_handler.SchemaRegistryCadClient")
    @patch("pm_data_handler.entities.pm_data_handler.MongoDBCadClient")
    @patch.object(PMDataHandler, "get_kafka_configs")
    def mock_init(
        self,
        mock_kafka_meth,
        mock_mongo,
        mock_schema,
        mock_des_cad_consumer,
        mock_transformer,
        mock_logger,
    ):

        return PMDataHandler()

    def mock_kafka_config(self):
        return {
            "bootstrap.servers": "localhost:8888",
            "key.deserializer": "kafka_key_deserializer",
            "value.deserializer": "kafka_value_deserializer",
            "group.id": "cad_client",
            "auto.offset.reset": "earliest",
        }

    def mock_running_property(self):
        PMDataHandler.RUNNING = PropertyMock(side_effect=[True, False])

    def mock_kafka_msg(self):
        return Mock(Message)

    if __name__ == "__main__":
        unittest.main()
