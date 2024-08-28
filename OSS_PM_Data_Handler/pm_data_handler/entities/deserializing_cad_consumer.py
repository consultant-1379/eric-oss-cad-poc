# coding: utf-8
#
# Copyright Ericsson (c) 2022

from confluent_kafka import Consumer
from confluent_kafka.error import (
    ConsumeError,
    KeyDeserializationError,
    ValueDeserializationError,
)
from confluent_kafka.schema_registry.avro import AvroDeserializer
from confluent_kafka.serialization import (
    MessageField,
    SerializationContext,
    StringDeserializer,
)

from pm_data_handler.configs.kafka_config import KafkaParams
from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger
from pm_data_handler.entities.schema_registry_cad_client import SchemaRegistryCadClient
from pm_data_handler.utils.meta import SingletonMeta


# TODO: GnbFiltering , From where will we get the list of the gnb_ids currently envolved in an optimization instance
class DeserializingCadConsumer(Consumer, metaclass=SingletonMeta):
    logger = PMDataHandlerLogger().get_logger("dev")

    key_deserializer: StringDeserializer
    value_deserializer: AvroDeserializer
    schema_registry_cad_client: SchemaRegistryCadClient

    """
    Deserializing Cad Consumer

    Desc:
        A customized Cad kafka client that consumes only needed pm records from a Kafka cluster with deserialization
        and filtering capabilities

    Notes:

        Configuration of this sub-module is centralized in configs.kafka_config.py

        The ``key.deserializer`` and ``value.deserializer`` classes instruct the
        DeserializingCadConsumer on how to convert the message payload bytes to objects.

        All configured callbacks are served from the application queue upon
        calling :py:func:`DeserializingCadConsumer.poll_and_deserialize_and_filter`

    Notable DeserializingConsumer configuration properties(* indicates required field)

    +-------------------------+---------------------+-----------------------------------------------------+
    | Property Name           | Type                | Description                                         |
    +=========================+=====================+=====================================================+
    | ``bootstrap.servers`` * | str                 | Comma-separated list of brokers.                    |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Client group id string.                             |
    | ``group.id`` *          | str                 | All clients sharing the same group.id belong to the |
    |                         |                     | same group.                                         |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Callable(SerializationContext, bytes) -> obj        |
    | ``key.deserializer``    | callable            |                                                     |
    |                         |                     | Deserializer used for message keys.                 |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Callable(SerializationContext, bytes) -> obj        |
    | ``value.deserializer``  | callable            |                                                     |
    |                         |                     | Deserializer used for message values.               |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Callable(KafkaError)                                |
    |                         |                     |                                                     |
    | ``error_cb``            | callable            | Callback for generic/global error events. These     |
    |                         |                     | errors are typically to be considered informational |
    |                         |                     | since the client will automatically try to recover. |
    +-------------------------+---------------------+-----------------------------------------------------+
    | ``logger``              | ``logging.Handler`` | Logging handler to forward logs                     |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Callable(str)                                       |
    |                         |                     |                                                     |
    |                         |                     | Callback for statistics. This callback is           |
    | ``stats_cb``            | callable            | added to the application queue every                |
    |                         |                     | ``statistics.interval.ms`` (configured separately). |
    |                         |                     | The function argument is a JSON formatted str       |
    |                         |                     | containing statistics data.                         |
    +-------------------------+---------------------+-----------------------------------------------------+
    |                         |                     | Callable(ThrottleEvent)                             |
    | ``throttle_cb``         | callable            |                                                     |
    |                         |                     | Callback for throttled request reporting.           |
    +-------------------------+---------------------+-----------------------------------------------------+

    See Also:
        - `CONFIGURATION.md <https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md>`
            _ for additional configuration property details

        - `STATISTICS.md <https://github.com/edenhill/librdkafka/blob/master/STATISTICS.md>`
            _ for detailed information about the statistics handled by stats_cb

    Args:
        conf (dict): DeserializingConsumer configuration.

    Raises:
        ValueError: if configuration validation fails
    """

    def __init__(self, conf):
        conf_copy = conf.copy()
        self.key_deserializer = conf_copy.pop("key.deserializer", None)
        self.value_deserializer = conf_copy.pop("value.deserializer", None)
        self.parent_init(conf_copy)
        self.schema_registry_cad_client = SchemaRegistryCadClient()

    def poll_and_deserialize_and_filter(self, timeout=KafkaParams.TIME_OUT):
        """
        Deserializing Cad Consumer poll_and_deserialize_and_filter() -> method

        Desc:
            Consume only Cad needed pm data messages and calls callbacks

        Args:
            timeout (float): Maximum time to block waiting for message(Seconds)

        Returns:
            :py:class:`Message` or None on timeout or on Ignored message

        Raises:
            KeyDeserializationError: If an error occurs during key
            deserialization.
            ValueDeserializationError: If an error occurs during value
            deserialization.
            ConsumeError if an error was encountered while polling.
        """

        # Handle the polling process of Kafka's message
        message = self.handle_poll_kafka_message(timeout)
        if message is None:
            return None
        # Create the serialization context to fetch the value
        serialization_context = self.get_serialization_context(message)

        # Handle deserialization process of Kafka's message
        value = self.handle_deserialize_kafka_msg_value(message, serialization_context)

        # Handle the polling process of Kafka's message
        key = self.handle_deserialize_kafka_msg_key(message, serialization_context)
        if key is None:
            return None

        # At that stage, received message that is still needed and well deserialized
        # Set the deserialized key of the message
        message.set_key(key)
        # Set the deserialized value of the message
        message.set_value(value)
        # Return the final needed message ( After deserialization and all checks )
        return message

    def get_serialization_context(self, message):
        return SerializationContext(
            message.topic(), MessageField.VALUE, message.headers()
        )

    def handle_deserialize_kafka_msg_key(self, message, serialization_context):

        # Get the serialized message key
        key = message.key()
        # Change the context of serialization to fetch the key
        serialization_context.field = MessageField.KEY
        # Check if we need to deserialize the message key (if the key deserializer is not None in kafka_config)
        if self.key_deserializer is not None:
            try:
                # Deserialize the key based on kafka_config configuration file
                key = self.key_deserializer(key, serialization_context)
                # If from the deserialized key we can decide to ignore the received message
                if not self.is_needed_message_from_deserialized_key(key):
                    # We ignore the message
                    return None
            except Exception as se:
                self.logger.warning(
                    "Key Deserialization failed for some reason -> raise the exception"
                )
                raise KeyDeserializationError(exception=se, kafka_message=message)

        return key

    def handle_deserialize_kafka_msg_value(self, message, serialization_context):

        # Get the serialized message value
        value = self.get_msg_value(message)

        # Check if we need to deserialize the message value (if the value deserializer is not None in kafka_config)
        if self.value_deserializer is not None:
            try:
                # Deserialize- the value with last updated value deserializer (related to latest fetched schema from SR)
                value = self.value_deserializer(value, serialization_context)
                # If from the deserialized value we can decide to ignore the received message
                if not self.is_needed_message_from_deserialized_value(value):
                    # We ignore the message
                    return None
            except Exception as se:
                # If a value deserialization error encountered we try to :
                # Extract needed info from the exception
                self.logger.warning(
                    "First try, Deserialization error encountered: " + str(se)
                )
                # Reload the latest schema again
                self.logger.info(
                    "First try, Fetch the latest schema version & load in memory"
                )
                self.schema_registry_cad_client.update_schema()
                # Re update the value deserializer
                self.value_deserializer = AvroDeserializer(  # The exception is occurring exactly here
                    self.schema_registry_cad_client,
                    self.schema_registry_cad_client.latest_registered_schema.schema_str,
                )
                try:

                    # Deserialize again the value after updating the deserializer with the latest schema
                    self.logger.info("Second try, Redo deserialization")
                    value = self.value_deserializer(value, serialization_context)
                    # If from the deserialized value we can decide to ignore the received message
                    if not self.is_needed_message_from_deserialized_value(value):
                        # We ignore the message
                        return None

                except Exception as se:
                    # Value Deserialization failed for other reason -> raise the exception
                    self.logger.warning(
                        "Second try, deserialization error encountered: " + str(se)
                    )
                    raise ValueDeserializationError(exception=se, kafka_message=message)

        return value

    def handle_poll_kafka_message(self, timeout):
        # Poll the base message ( Before deserialization )
        # Calling the poll method of the `Consumer` parent class
        try:
            message = self.poll_msg(timeout)
        except RuntimeError as exc:
            self.logger.warning(
                "Deserializing cad consumer, an error occurred while polling for a message."
            )
            raise exc

        if message is None:
            return None

        msg_error = self.get_msg_error(message)
        if msg_error is not None:
            self.logger.warning(
                "An error occurred during  message  polling, ConsumerError exception is  raised"
            )
            raise ConsumeError(msg_error, kafka_message=message)

        msg_headers = self.get_msg_headers(message)
        if (msg_headers is not None) and (
            not self.is_needed_message_from_headers(msg_headers)
        ):
            return None

        return message

    """
    Deserializing Cad Consumer is_needed_message_from_headers() -> static method

    Desc:
        From headers content of the message, this method decide whether we still
        want to persist the record or not before any deserialization

    Args:
        msg_headers ([(str, bytes),...] or None):  list of two-tuples, one (key, value) pair for each header

    Returns:
        decision (bool): True if we still want to keep the message
    """

    @staticmethod
    def is_needed_message_from_headers(msg_headers: []) -> bool:
        # TODO [Sync with producer team to know exactly what is available in the message headers ]:
        #    returns True if any info inside the headers needs to be checked
        #    to decide whether we still want to persist the record or not before any deserialization
        #    ALGO [If possible from sync with producer team]:
        #    if name of MO of the message headers is the name of the MO Holding our needed pm counters :
        #           if gnb_id of the message headers is envolved in an optimization instance :
        #               return True
        #    return False
        #
        #
        # TODO [TO BE DELETED] : currently forcing it to return true
        return True

    """
    Deserializing CAD Consumer is_needed_message_from_deserialized_value() -> static method

    Desc:
        From the deserialized message value, this method decide whether we still
        want to persist the record or not after deserialization

    Args:
        msg_value (dict) : Message deserialized value

    Returns:
        decision (bool): True if we still want to keep the message
    """

    @staticmethod
    def is_needed_message_from_deserialized_value(msg_value: dict) -> bool:
        # TODO [Sync with producer team to know exactly what is available in the message value ]:
        #    ALGO :
        #    returns True if any info inside the value needs to be checked
        #    to decide whether we still want to persist the record or not after deserializing the value
        #    Otherwise returns False
        # TODO [TO BE DELETED] : currently forcing it to return true
        return True

    """
    Deserializing Cad Consumer is_needed_message_from_deserialized_key() -> static method

    Desc:
        From the deserialized message key, this method decide whether we still
        want to persist the record or not after deserialization

    Args:
        msg_key (str): Message deserialized key

    Returns:
        decision (bool): True if we still want to keep the message
    """

    @staticmethod
    def is_needed_message_from_deserialized_key(msg_key: str) -> bool:
        # TODO [Sync with producer team to know exactly what is available in the message key ]:
        #    ALGO :
        #    returns True if any info inside the key needs to be checked
        #    to decide whether we still want to persist the record or not after deserializing the key
        #    Otherwise returns False
        # TODO [TO BE DELETED] : currently forcing it to return true
        return True

    def parent_init(self, conf_copy):
        super().__init__(conf_copy)

    def poll_msg(self, timeout):
        return super(DeserializingCadConsumer, self).poll(timeout)

    def get_msg_value(self, msg):
        return msg.value()

    def get_msg_headers(self, msg):
        return msg.headers()

    def get_msg_error(self, msg):
        return msg.error()
