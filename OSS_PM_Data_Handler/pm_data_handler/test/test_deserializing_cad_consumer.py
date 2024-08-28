#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import unittest
from unittest import TestCase
from unittest.mock import patch

from confluent_kafka.error import ConsumeError, KeyDeserializationError

from pm_data_handler.entities import deserializing_cad_consumer
from pm_data_handler.entities.deserializing_cad_consumer import DeserializingCadConsumer


class TestDeserializingCadConsumer(TestCase):
    def tearDown(self):
        """
        As DeserializingCadConsumer follows the singleton pattern, deleting class instance after each test will prevent
        working on the same instance
        """
        DeserializingCadConsumer._instances = {}

    consumer_conf = {
        "bootstrap.servers": "localhost:8080",
        "group.id": "arc_client",
        "auto.offset.reset": "earliest",
    }

    """
    WHEN deserializing_cad_consumer init is called
    THEN parent init is called
    """

    @patch(
        "pm_data_handler.entities.deserializing_cad_consumer.SchemaRegistryCadClient"
    )
    @patch.object(DeserializingCadConsumer, "parent_init")
    def test_init_method(self, mock_parent_init, mock_cad_client):
        # WHEN
        DeserializingCadConsumer(self.consumer_conf)

        # THEN
        mock_parent_init.assert_called_once_with(self.consumer_conf)

    """
    GIVEN kafka none msg
    WHEN deserializing_cad_consumer is called
    THEN none returned
    """

    @patch.object(DeserializingCadConsumer, "poll_msg")
    def test_polled_msg_with_none(self, mocked_msg):
        # GIVEN
        mocked_msg.return_value = None

        # WHEN
        actual_value = self.mock_init().poll_and_deserialize_and_filter()

        # THEN
        self.assertIsNone(actual_value)

    """
    GIVEN kafka msg with error
    WHEN deserializing_cad_consumer is called
    THEN exception is raised
    """

    @patch.object(DeserializingCadConsumer, "poll_msg")
    @patch.object(DeserializingCadConsumer, "get_msg_error")
    def test_polled_msg_with_error(self, mock_error, mocked_msg):
        # GIVEN
        mock_error.return_value = True

        # WHEN
        try:
            self.mock_init().poll_and_deserialize_and_filter()

        # THEN
        except ConsumeError:
            self.assertRaises(ConsumeError)

    """
    GIVEN non needed kafka msg headers
    WHEN deserializing_cad_consumer is called
    THEN None is returned
    """

    @patch.object(DeserializingCadConsumer, "poll_msg")
    @patch.object(DeserializingCadConsumer, "get_msg_error")
    @patch.object(DeserializingCadConsumer, "get_msg_headers")
    @patch.object(DeserializingCadConsumer, "is_needed_message_from_headers")
    def test_given_non_needed_msg_headers_when_deserializing_then_none_returned(
        self, mock_is_needed, mock_headers, mocked_error, mocked_msg
    ):
        # GIVEN
        mocked_error.return_value = None
        mock_headers.return_value = "unneeded header"
        mock_is_needed.return_value = False

        # WHEN
        actual_return = self.mock_init().poll_and_deserialize_and_filter()

        # THEN
        self.assertIsNone(actual_return)

    """
    GIVEN non needed kafka msg headers
    WHEN deserializing_cad_consumer is called
    THEN None is returned
    """

    @patch("pm_data_handler.entities.deserializing_cad_consumer.SerializationContext")
    @patch.object(DeserializingCadConsumer, "get_msg_value")
    @patch.object(DeserializingCadConsumer, "poll_msg")
    @patch.object(DeserializingCadConsumer, "get_msg_error")
    @patch.object(DeserializingCadConsumer, "get_msg_headers")
    @patch.object(DeserializingCadConsumer, "is_needed_message_from_headers")
    def test_given_needed_msg_headers_when_deserializing_then_msg_returned(
        self,
        mock_is_needed,
        mock_headers,
        mocked_error,
        mocked_msg,
        mock_msg_value,
        mock_ser_ctx,
    ):

        # GIVEN
        mocked_error.return_value = None
        mock_headers.return_value = "needed header"
        mock_is_needed.return_value = True

        # WHEN
        self.mock_init().poll_and_deserialize_and_filter()
        # THEN
        mock_msg_value.assert_called_once()
        mock_ser_ctx.assert_called_once()

    """
     WHEN poll method raises an exception
     THEN log error will be called and error will raised
    """

    @patch.object(deserializing_cad_consumer.DeserializingCadConsumer.logger, "warning")
    @patch.object(DeserializingCadConsumer, "poll_msg")
    def test_given_poll_msg_raise_an_error_when_deserializing_then_log_error_is_called(
        self, mocked_poll_msg, mocked_logger
    ):
        # GIVEN
        mocked_poll_msg.side_effect = RuntimeError()

        # WHEN
        try:
            self.mock_init().poll_and_deserialize_and_filter()
            assert False
        except RuntimeError:
            # THEN
            mocked_logger.assert_called_once()
            assert True

    """
     WHEN value_deserializer method raises an exception
     THEN logger is called and an exception is thrown
    """

    @patch(
        "pm_data_handler.entities.deserializing_cad_consumer.DeserializingCadConsumer.logger"
    )
    @patch.object(DeserializingCadConsumer, "get_msg_value")
    def test_deserializing_kafka_msg_value_when_exception_thrown_then_log_msg_and_raise_exception(
        self, mock_msg_value, mock_logger
    ):
        # GIVEN
        deserializer_cad_consumer = self.mock_init()
        deserializer_cad_consumer.value_deserializer = "mocked value"
        # WHEN
        try:
            deserializer_cad_consumer.handle_deserialize_kafka_msg_value(
                mock_msg_value, None
            )
        # THEN
        except TypeError:
            mock_logger.warning.assert_called_once()
            mock_logger.info.assert_called_once()

    """
     WHEN key_deserializer method raises an exception
     THEN logger is called and exception is thrown
    """

    @patch.object(DeserializingCadConsumer, "is_needed_message_from_deserialized_key")
    @patch(
        "pm_data_handler.entities.deserializing_cad_consumer.DeserializingCadConsumer.logger"
    )
    @patch.object(DeserializingCadConsumer, "get_msg_value")
    @patch.object(DeserializingCadConsumer, "get_serialization_context")
    def test_deserializing_kafka_msg_key_when_exception_thrown_then_logging_and_raise_exception(
        self, mock_serialization_context, mock_msg_value, mock_logger, mock_key_filter
    ):
        # GIVEN
        deserializer_cad_consumer = self.mock_init()
        deserializer_cad_consumer.key_deserializer = "mocked key"
        # WHEN
        try:
            deserializer_cad_consumer.handle_deserialize_kafka_msg_key(
                mock_msg_value, mock_serialization_context
            )
        # THEN
        except KeyDeserializationError:
            mock_key_filter.assert_not_called()
            mock_logger.warning.assert_called_once()

    """
     WHEN calling mocked filtering methods
     THEN true is always returned
    """

    @patch.object(DeserializingCadConsumer, "get_msg_value")
    def test_when_calling_mocked_filtered_methods_then_always_true_returned(
        self, mock_msg_value
    ):
        # WHEN
        actual_value_filter = (
            DeserializingCadConsumer.is_needed_message_from_deserialized_value(
                mock_msg_value
            )
        )
        actual_key_filter = (
            DeserializingCadConsumer.is_needed_message_from_deserialized_key(
                mock_msg_value
            )
        )
        actual_headers_filter = DeserializingCadConsumer.is_needed_message_from_headers(
            mock_msg_value
        )
        # THEN
        self.assertTrue(actual_value_filter)
        self.assertTrue(actual_key_filter)
        self.assertTrue(actual_headers_filter)

    @patch.object(DeserializingCadConsumer, "parent_init")
    @patch(
        "pm_data_handler.entities.deserializing_cad_consumer.SchemaRegistryCadClient"
    )
    def mock_init(self, mock_cad_client, mock_parent_init):
        return DeserializingCadConsumer(self.consumer_conf)

    if __name__ == "__main__":
        unittest.main()
