# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import json
import requests_mock

from unittest import TestCase
from clients.constants import ContentType, HttpHeader

from services import topology_service
from . import TEST_DIR


@requests_mock.Mocker()
class TestTopologyService(TestCase):
    JSON_HEADER = {HttpHeader.CONTENT_TYPE: ContentType.JSON}
    COUNT = 2
    HOSTNAME = "localhost"
    COUNT_URL = f'https://{HOSTNAME}/oss-core-ws/rest/ctw/gnbduTask/count'
    NODE_URL = f'https://{HOSTNAME}/oss-core-ws/rest/ctw/gnbdu'

    def setUp(self):
        self.service = topology_service

    def test_count_nodes(self, m):
        m.get(self.COUNT_URL, json=self.COUNT, headers=self.JSON_HEADER)
        self.assertEqual(self.COUNT, self.service.count_nodes())

    def test_get_node(self, m):
        with open(f'{TEST_DIR}/resources/sample_gnbdu.json', 'r') as f:
            response_json = json.load(f)
        m.get(f'{self.NODE_URL}/647', json=response_json, headers=self.JSON_HEADER)
        self.assertEqual(response_json, self.service.get_node(647))
        self.assertEqual(1, m.call_count)

    def test_list_nodes(self, m):
        with open(f'{TEST_DIR}/resources/sample_gnbdu.json', 'r') as f:
            response_json = json.load(f)
        count_adapter = m.get(self.COUNT_URL, json=self.COUNT, headers=self.JSON_HEADER)
        list_adapter = m.get(self.NODE_URL, json=[response_json], headers=self.JSON_HEADER)
        self.assertEqual(self.COUNT, len(self.service.list_nodes()))
        self.assertEqual(1, count_adapter.call_count)
        self.assertEqual(2, list_adapter.call_count)
