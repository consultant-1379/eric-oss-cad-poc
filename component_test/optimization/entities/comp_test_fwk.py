#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022


from tests.utilities import TestRunner


class ComponentTestFwk:
    test_runner = TestRunner()

    def launch(self):
        self.test_runner.run_tests()
