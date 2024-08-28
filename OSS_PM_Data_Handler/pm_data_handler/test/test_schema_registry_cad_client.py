#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
from http import HTTPStatus
from unittest import TestCase
from unittest.mock import patch

from confluent_kafka.schema_registry import (
    RegisteredSchema,
    SchemaRegistryClient,
    SchemaRegistryError,
)

from pm_data_handler.configs.schema_registry_config import SchemaRegistryParams
from pm_data_handler.entities.schema_registry_cad_client import SchemaRegistryCadClient


class TestSchemaRegistryCadClient(TestCase):
    """
    GIVEN schema  registry config and subject
    WHEN constructing SchemaRegistryCadClient
    THEN schema is updated
    """

    @patch.object(SchemaRegistryCadClient, "get_latest_version")
    @patch(
        "pm_data_handler.entities.schema_registry_cad_client.SchemaRegistryClient.__init__"
    )
    def test_schema_registry_client_init(self, mock_init, mock_get_version):
        # GIVEN
        mocked_schema = self.mock_registered_schema()
        mock_get_version.return_value = mocked_schema

        # WHEN
        cad_client = SchemaRegistryCadClient()

        # THEN
        self.assertTrue(
            mock_init.called_with(conf=SchemaRegistryParams.get_conf()),
            mock_get_version.called,
        )
        self.assertEqual(cad_client.latest_registered_schema, mocked_schema.schema)
        self.assertEqual(cad_client.subject_name, SchemaRegistryParams.SUBJECT)

    """
    GIVEN Singleton pattern
    WHEN creating two different instances
    THEN both instances should be  equal
    """

    def test_singleton_pattern(self):
        # WHEN
        schema_reg_instance_1 = self.mock_init()
        schema_reg_instance_2 = self.mock_init()
        # THEN
        self.assertEqual(schema_reg_instance_1, schema_reg_instance_2)

    """
    GIVEN SchemaRegistryError
    WHEN  update_schema
    THEN  SchemaRegistryError reraised
    """

    @patch(
        "pm_data_handler.entities.schema_registry_cad_client.SchemaRegistryClient.__init__"
    )
    @patch("pm_data_handler.entities.pm_data_handler.PMDataHandlerLogger")
    @patch.object(SchemaRegistryClient, "get_latest_version")
    def test_given_schema_registry_error_raised_when_update_schema_then_error_reraised(
        self, mock_schema, mock_logger, mock_init
    ):
        # GIVEN

        mock_schema.side_effect = SchemaRegistryError(
            HTTPStatus.BAD_REQUEST, -1, "error_msg"
        )
        try:
            SchemaRegistryCadClient()
            self.fail()
        except SchemaRegistryError:
            pass

    @patch.object(SchemaRegistryCadClient, "get_latest_version")
    @patch(
        "pm_data_handler.entities.schema_registry_cad_client.SchemaRegistryClient.__init__"
    )
    def mock_init(self, mock_init, mock_version):
        return SchemaRegistryCadClient()

    def mock_registered_schema(self):
        return RegisteredSchema("id", "schema_str", "type", "_hash")
