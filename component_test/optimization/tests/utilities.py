#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022

import pytest


class TestRunner:
    pytest_arguments = [
        "-p", "no:cacheprovider" # disabling cache plugin
        ]

    def run_tests(self):
        print("Running tests...")
        return_code = pytest.main(self.pytest_arguments)
        print("Pytest return code: {}".format(return_code))
