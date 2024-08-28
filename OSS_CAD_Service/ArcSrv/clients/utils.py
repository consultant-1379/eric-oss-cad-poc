# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import json
import os.path
from jsonschema import validate, RefResolver

from .constants import HttpHeader

path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def validate_content_type(accept: str):
    def decorator(rest_call):
        def wrapper(*args, **kwargs):
            response = rest_call(*args, **kwargs)
            content_type = response.headers[HttpHeader.CONTENT_TYPE]
            if content_type != accept:
                raise ValueError(f'Unexpected content type: {content_type},'
                                 f' for call {response.request.method} {response.request.path_url}')
            return response
        return wrapper
    return decorator


def validate_json(schema_path):
    def decorator(rest_call):
        def wrapper(*args, **kwargs):
            response = rest_call(*args, **kwargs)
            schema_file = os.path.join(path, 'resources', 'schemas', schema_path)

            with open(schema_file, 'r') as file:
                schema_dir = os.path.dirname(schema_file)
                schema = json.load(file)
                resolver = RefResolver(base_uri=f'file://{schema_dir}/', referrer=schema)
                validate(instance=response, schema=schema, resolver=resolver)
            return response
        return wrapper
    return decorator
