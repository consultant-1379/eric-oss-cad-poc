#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022
import unittest
from unittest import TestCase, mock
from unittest.mock import Mock, patch

from pymongo.collection import Collection
from pymongo.database import Database
from pymongo.errors import InvalidName, PyMongoError

from pm_data_handler.entities import mongo_db_cad_client
from pm_data_handler.entities.cad_pm_data import PMData
from pm_data_handler.entities.mongo_db_cad_client import MongoDBCadClient


class TestMongoDBCadClient(TestCase):
    def tearDown(self):
        """
        As MongoDBCadClient follows the singleton pattern, deleting class instance after each test will prevent
        working on the same  instance
        """
        MongoDBCadClient._instances = {}

    """

      GIVEN right collection list

      WHEN creating mongoDB cad client

      THEN pm_collection is set correctly

      """

    @patch.object(MongoDBCadClient, "_check_mongo_server")
    @patch.object(MongoDBCadClient, "retrieve_db")
    @patch("pm_data_handler.entities.mongo_db_cad_client.MongoClient.__init__")
    def test_pm_collection_name_exists_in_db_list_of_collection_names(
        self, mock_init, mock_retrieve_db, mock_check_mongo_server
    ):
        # GIVEN
        mocked_db = self.mock_mongo_db()
        mock_retrieve_db.return_value = mocked_db
        mocked_db.list_collection_names.return_value = ["pm_data", "arc"]
        mocked_db.get_collection.return_value = self.mock_collection()

        # WHEN
        MongoDBCadClient()
        # THEN
        mock_init.assert_called_once()
        mock_retrieve_db.assert_called_once()

        mocked_db.list_collection_names.assert_called_once()
        mocked_db.get_collection.assert_called_once()

    """

      GIVEN wrong collection list

      WHEN creating mongoDB cad client

      THEN create new collection with the right pm collection name

    """

    @patch.object(MongoDBCadClient, "_check_mongo_server")
    @patch.object(MongoDBCadClient, "retrieve_db")
    def test_pm_collection_name_does_not_exist_in_db_list_of_collection_names(
        self, mock_retrieve_db, mock_check_mongo_server
    ):
        # GIVEN
        mocked_db = self.mock_mongo_db()
        mock_retrieve_db.return_value = mocked_db
        mocked_db.list_collection_names.return_value = ["log_data", "arc_data"]
        mocked_db.get_collection.return_value = self.mock_collection()

        # WHEN
        MongoDBCadClient()

        # THEN
        mocked_db.create_collection.assert_called_once()
        mocked_db.get_collection.assert_called_once()
        expected_calls = [
            mock.call([("metadata.gnb_id", 1)]),
            mock.call([("timestamp", -1)]),
            mock.call([("metadata.gnb_id", 1), ("timestamp", -1)]),
        ]

        actual_calls = mocked_db.get_collection().create_index.mock_calls

        self.assertEqual(actual_calls, expected_calls)

    """

          WHEN insert pm collection data correctly

          THEN insert one will be called one successfully

    """

    @patch.object(MongoDBCadClient, "_check_mongo_server")
    @patch.object(MongoDBCadClient, "retrieve_db")
    @patch("pm_data_handler.entities.mongo_db_cad_client.MongoClient.__init__")
    def test_pm_collection_insert_success(
        self, mock_init, mock_retrieve_db, mock_check_mongo_server
    ):
        # GIVEN
        mongo_client = MongoDBCadClient()
        pm_data = PMData(1, 1, 1, 1, 1, 1, 1, 1)

        # WHEN
        mongo_client.insert(pm_data)

        # THEN
        mongo_client.pm_collection.insert_one.assert_called_once()

    """

          WHEN insert pm collection data raise an exception

          THEN log warning will be called and exception will reraised

    """

    @patch.object(mongo_db_cad_client.MongoDBCadClient.logger, "warning")
    @patch.object(MongoDBCadClient, "_check_mongo_server")
    @patch.object(MongoDBCadClient, "retrieve_db")
    @patch("pm_data_handler.entities.mongo_db_cad_client.MongoClient.__init__")
    def test_raise_exception_pm_collection_insert(
        self, mock_init, mock_retrieve_db, mock_check_mongo_server, mocked_logger
    ):
        # GIVEN
        mongo_client = MongoDBCadClient()
        mongo_client.pm_collection = self.mock_collection()
        mongo_client.pm_collection.insert_one.side_effect = PyMongoError()
        pm_data = PMData(1, 1, 1, 1, 1, 1, 1, 1)

        # WHEN
        try:
            mongo_client.insert(pm_data)
            assert False
        except PyMongoError:
            # THEN
            mocked_logger.assert_called_once()
            assert True

    """

          WHEN get collection raise an exception

          THEN log error will be called and exception will reraised

    """

    @patch.object(mongo_db_cad_client.MongoDBCadClient.logger, "warning")
    @patch.object(MongoDBCadClient, "_check_mongo_server")
    @patch.object(MongoDBCadClient, "retrieve_db")
    @patch("pm_data_handler.entities.mongo_db_cad_client.MongoClient.__init__")
    def test_raise_exception_get_pm_collection(
        self, mock_init, mock_retrieve_db, mock_check_mongo_server, mocked_logger
    ):
        # GIVEN
        mongo_client = MongoDBCadClient()
        mongo_client.db = self.mock_mongo_db()
        mongo_client.db.list_collection_names.return_value = ["log_data", "arc_data"]
        mongo_client.db.get_collection.side_effect = InvalidName(
            "error while fetching collection"
        )
        # WHEN
        try:
            mongo_client.get_pm_collection("pm_data", True)
            assert False
        except InvalidName:
            # THEN
            mocked_logger.assert_called_once()
            assert True

    def mock_mongo_db(self):
        return Mock(Database)

    def mock_collection(self):
        return Mock(Collection)

    if __name__ == "__main__":
        unittest.main()
