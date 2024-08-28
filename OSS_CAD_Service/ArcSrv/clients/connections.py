# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging
import requests
from requests.adapters import HTTPAdapter
from urllib3 import Retry, exceptions, disable_warnings
from .utils import validate_content_type
from .constants import HttpHeader, HttpMethod, ContentType

logging.getLogger("urllib3.connectionpool").setLevel(logging.ERROR)
disable_warnings(exceptions.InsecureRequestWarning)
RETRY_AFTER_STATUS_CODES = frozenset({413, 429, 500, 501, 502, 503, 504})


class RestConnection(object):
    def __init__(self, hostname: str, username: str, password: str, **kwargs):
        self.__init_session()
        self._url = f"{kwargs.get('schema', 'https')}://{hostname}"
        self.__username = username
        self.__password = password
        self.__tenant = kwargs.get('tenant', 'master')

    def __del__(self):
        self._session.close()

    def __init_session(self, **kwargs):
        self._session = requests.Session()
        self._session.verify = False
        self._session.mount(f"{kwargs.get('schema', 'https')}://", HTTPAdapter(
            max_retries=Retry(
                total=kwargs.get('max_retries', 5),
                backoff_factor=1.1,
                status_forcelist=RETRY_AFTER_STATUS_CODES
            )))

    def __auth(self):
        auth_headers = {'X-Tenant': self.__tenant, 'X-Login': self.__username, 'X-Password': self.__password}
        resp = self._session.request(str(HttpMethod.POST.value), f"{self._url}/auth/v1/login", headers=auth_headers)
        resp.raise_for_status()

    def request(self, method: HttpMethod, path: str, **kwargs) -> requests.Response:
        method, url = str(method.value), f"{self._url}{path}"
        response = self._session.request(method, url, **kwargs)
        if (response.headers.get(HttpHeader.CONTENT_TYPE) or "").startswith(ContentType.HTML) or \
                response.status_code == 401:
            self.__auth()
            response = self._session.request(method, url, **kwargs)
        response.raise_for_status()
        return response

    def request_json(self, method: HttpMethod, path: str, **kwargs):
        return validate_content_type(ContentType.JSON)(self.request)(method, path, **kwargs).json()
