#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022


import json
import requests


class FlowAutomationApi:
    CREATE_OPTIMIZATION_INSTANCE_URL = "http://arcoptimization:5000/optimizations/"

    def create_optimization_instance(self, data):
        return requests.post(url=self.CREATE_OPTIMIZATION_INSTANCE_URL, data=json.dumps(data))
