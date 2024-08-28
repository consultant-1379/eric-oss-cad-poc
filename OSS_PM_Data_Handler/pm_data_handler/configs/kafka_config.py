# coding: utf-8
#
# Copyright Ericsson (c) 2022
from abc import ABC, abstractmethod
from typing import Final


class KafkaParams(ABC):
    """
    Kafka Parameters Abstract configuration class  ([*] indicates that corresponding field is required):


    Final Static Attributes :

        TOPIC (Final str) [*]: Topic name

        BOOTSTRAP_SERVERS (Final str) [*]: Kafka broker server

        GROUP_ID (Final str) [*]: Client group id

        AUTO_OFFSET_RESET (Final str) [*]: Action to take when there is no initial offset in offset store
                                            or the desired offset is out of range

        TIME_OUT (Final float) [*]: Maximum time to block waiting for message(Seconds)

    For additional configuration property details :
        - `CONFIGURATION.md <https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md>`

    """

    TOPIC: Final = "pm_data"
    BOOTSTRAP_SERVERS: Final = "broker:29092"
    # BOOTSTRAP_SERVERS: Final = "localhost:9092"
    GROUP_ID: Final = "arc_client"
    AUTO_OFFSET_RESET: Final = "earliest"
    TIME_OUT: Final = 1.0
    """
        Kafka Parameters Abstract configuration class - abstract_method() -> abstract method

        Desc:
            Prevent creating instances of this configuration class
    """

    @abstractmethod
    def abstract_method(self):
        pass
