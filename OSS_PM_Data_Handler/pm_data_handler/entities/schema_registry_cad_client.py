# coding: utf-8
#
# Copyright Ericsson (c) 2022

from confluent_kafka.schema_registry import (
    Schema,
    SchemaRegistryClient,
    SchemaRegistryError,
)

from pm_data_handler.configs.schema_registry_config import SchemaRegistryParams
from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger
from pm_data_handler.utils.meta import SingletonMeta


# TODO: Logging activities and Exception Handling
class SchemaRegistryCadClient(SchemaRegistryClient, metaclass=SingletonMeta):
    latest_registered_schema: Schema
    subject_name: str
    """
        Schema Registry CAD Client.

        Args:
            sr_conf (dict): Schema Registry client configuration
            subject_name (str): Subject name the schema is registered under

        Notes:
            sr_conf -> configuration properties (* indicates a required field):
            +------------------------------+------+-------------------------------------------------+
            | Property name                | type | Description                                     |
            +==============================+======+=================================================+
            | ``url`` *                    | str  | Schema Registry URL.                            |
            +------------------------------+------+-------------------------------------------------+
            |                              |      | Path to CA certificate file used                |
            | ``ssl.ca.location``          | str  | to verify the Schema Registry's                 |
            |                              |      | private key.                                    |
            +------------------------------+------+-------------------------------------------------+
            |                              |      | Path to client's private key                    |
            |                              |      | (PEM) used for authentication.                  |
            | ``ssl.key.location``         | str  |                                                 |
            |                              |      | ``ssl.certificate.location`` must also be set.  |
            +------------------------------+------+-------------------------------------------------+
            |                              |      | Path to client's public key (PEM) used for      |
            |                              |      | authentication.                                 |
            | ``ssl.certificate.location`` | str  |                                                 |
            |                              |      | May be set without ssl.key.location if the      |
            |                              |      | private key is stored within the PEM as well.   |
            +------------------------------+------+-------------------------------------------------+
            |                              |      | Client HTTP credentials in the form of          |
            |                              |      | ``username:password``.                          |
            | ``basic.auth.user.info``     | str  |                                                 |
            |                              |      | By default userinfo is extracted from           |
            |                              |      | the URL if present.                             |
            +------------------------------+------+-------------------------------------------------+

    """

    def __init__(
        self,
        sr_conf=SchemaRegistryParams.get_conf(),
        subject_name=SchemaRegistryParams.SUBJECT,
    ):
        super().__init__(conf=sr_conf)
        self.subject_name = subject_name
        self.update_schema()

    """
        Schema Registry CAD Client - upgrade_schema() -> method

        Desc:
            Fetches the latest registered schema associated with ``subject_name`` from the Schema Registry
            The result is cached (saved) in ``latest_schema`` attr

        Raises:
            SchemaRegistryError: If schema can't be found

    """

    def update_schema(self):
        logger = PMDataHandlerLogger().get_logger("dev")
        try:
            # Use ``self.subject_name`` attr as an identifier to look up the latest schema associated to it
            registered_schema = self.get_latest_version(self.subject_name)
        except SchemaRegistryError as exp:
            logger.warning(
                "SchemaRegistryCadClient, the schema version can't be found or is invalid. exception:",
                exc_info=True,
            )
            raise exp
        else:
            self.latest_registered_schema = registered_schema.schema
