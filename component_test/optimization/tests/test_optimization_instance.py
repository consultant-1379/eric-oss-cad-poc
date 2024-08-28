#!/usr/bin/env python
#
# Copyright Ericsson (c) 2022


from apis.flow_automation.flow_automation_api import FlowAutomationApi


class TestOptimizationInstance:
    fa = FlowAutomationApi()

    def test_create_optimization_instance(self):
        request_data = {"selectedNodes": {
                            "gnbIdList": [
                                {"gnbId": 208728, "cmHandle": "2F546CEBA2B9B70D1400545EF042214A"},
                                {"gnbId": 208729, "cmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6"}
                            ]
                        },
                        "unwantedNodePairs": {
                            "gnbPairsList": []
                        },
                        "mandatoryNodePairs": {
                            "gnbPairsList": []
                        }}
        response = self.fa.create_optimization_instance(request_data)
        assert response.status_code == 200

    def test_create_optimization_instance_invalid_schema(self):
        request_data = {"selectedNodes": {
                            "gnbIdList": [
                                {"gnbId": 208728, "cmHandle": "2F546CEBA2B9B70D1400545EF042214A"},
                                {"gnbId": 208729, "cmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6"}
                            ]
                        }}
        response = self.fa.create_optimization_instance(request_data)
        assert response.status_code == 422
