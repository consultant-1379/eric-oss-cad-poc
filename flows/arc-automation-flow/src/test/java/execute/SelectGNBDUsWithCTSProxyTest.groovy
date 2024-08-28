// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import groovy.utils.ArcFlowUtils

import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.assertEquals

class SelectGNBDUsWithCTSProxyTest extends FlowAutomationScriptBaseTest {
    List<?> gnbList = []
    List<?> selectedGnbs = []

    String gnbdusUrl = "http://localhost:8003/ctw/gnbdu/all/"
    String gnbdusListPath = "/ctw/gnbdu/all/"

    String body = """[
  {
    "type":"ctw/gnbdu",
    "id":3,
    "href":"ctw/gnbdu/3",
    "externalId":"3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name":"tc11-vdu/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:01.361Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:01.361Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":3
    },
    "versionNumber":1,
    "gnbduId":1508,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":3
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":17,
    "href":"ctw/gnbdu/17",
    "externalId":"2F546CEBA2B9B70D1400545EF042214A/ericsson-enm-ComTop:ManagedElement=NR02gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:14.159Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:14.159Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":17
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":17
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":26,
    "href":"ctw/gnbdu/26",
    "externalId":"92F1CB35798FD7D13BCC6FF825D89CD6/ericsson-enm-ComTop:ManagedElement=NR03gNodeBRadio00002/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:39.707Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:39.707Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":26
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":26
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":59,
    "href":"ctw/gnbdu/59",
    "externalId":"D51D713178E554378D1EAC87B1740BC8/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00003/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:20:05.771Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:20:05.771Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":59
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":59
    }
  }
]"""
    String bodyNegID = """[
  {
    "type":"ctw/gnbdu",
    "id":3,
    "href":"ctw/gnbdu/3",
    "externalId":"3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name":"tc11-vdu/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:01.361Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:01.361Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":3
    },
    "versionNumber":1,
    "gnbduId":1508,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":3
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":-17,
    "href":"ctw/gnbdu/17",
    "externalId":"2F546CEBA2B9B70D1400545EF042214A/ericsson-enm-ComTop:ManagedElement=NR02gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:14.159Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:14.159Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":17
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":17
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":26,
    "href":"ctw/gnbdu/26",
    "externalId":"92F1CB35798FD7D13BCC6FF825D89CD6/ericsson-enm-ComTop:ManagedElement=NR03gNodeBRadio00002/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:39.707Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:39.707Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":26
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":26
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":59,
    "href":"ctw/gnbdu/59",
    "externalId":"D51D713178E554378D1EAC87B1740BC8/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00003/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:20:05.771Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:20:05.771Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":59
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":59
    }
  }
]"""
    String bodyEmptyName = """[
  {
    "type":"ctw/gnbdu",
    "id":3,
    "href":"ctw/gnbdu/3",
    "externalId":"3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name":"tc11-vdu/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:01.361Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:01.361Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":3
    },
    "versionNumber":1,
    "gnbduId":1508,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":3
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":17,
    "href":"ctw/gnbdu/17",
    "externalId":"2F546CEBA2B9B70D1400545EF042214A/ericsson-enm-ComTop:ManagedElement=NR02gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:14.159Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:14.159Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":17
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":17
    }
  },
  {
    "type":"ctw/gnbdu",
    "id":26,
    "href":"ctw/gnbdu/26",
    "externalId":"92F1CB35798FD7D13BCC6FF825D89CD6/ericsson-enm-ComTop:ManagedElement=NR03gNodeBRadio00002/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name":"Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:39.707Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:39.707Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":26
    },
    "versionNumber":1,
    "gnbduId":1,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":26
    }
  }
]"""
    String bodyWithOneGNBDU = """[
  {
    "type":"ctw/gnbdu",
    "id":3,
    "href":"ctw/gnbdu/3",
    "externalId":"3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name":"tc11-vdu/1",
    "revisionNumber":1,
    "createdOn":"2022-04-04T13:15:01.361Z",
    "createdBy":"sysadm",
    "status":"operating",
    "lastModifiedOn":"2022-04-04T13:15:01.361Z",
    "lastModifiedBy":"sysadm",
    "revisionGroupKey":{
      "type":"revisionGroupKey",
      "keyValue":3
    },
    "versionNumber":1,
    "gnbduId":1508,
    "key":{
      "type":"ctw/gnbduKey",
      "keyValue":3
    }
  }
]"""
    String bodyWithDuplicate = """[ {
    "type": "ctw/gnbdu",
    "id": 3,
    "href": "ctw/gnbdu/3",
    "externalId": "3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name": "tc11-vdu/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:15:01.361Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:15:01.361Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 3
    },
    "versionNumber": 1,
    "gnbduId": 1508,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 3
    }
}, {
    "type": "ctw/gnbdu",
    "id": 81,
    "href": "ctw/gnbdu/81",
    "externalId": "56B00962C8E55623C2F3A5A29BD9D795/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:24:30.250Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:24:30.250Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 81
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 81
    }
}, {
    "type": "ctw/gnbdu",
    "id": 17,
    "href": "ctw/gnbdu/17",
    "externalId": "2F546CEBA2B9B70D1400545EF042214A/ericsson-enm-ComTop:ManagedElement=NR02gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:15:14.159Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:15:14.159Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 17
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 17
    }
}, {
    "type": "ctw/gnbdu",
    "id": 26,
    "href": "ctw/gnbdu/26",
    "externalId": "92F1CB35798FD7D13BCC6FF825D89CD6/ericsson-enm-ComTop:ManagedElement=NR03gNodeBRadio00002/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:15:39.707Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:15:39.707Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 26
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 26
    }
}, {
    "type": "ctw/gnbdu",
    "id": 2478,
    "href": "ctw/gnbdu/2478",
    "externalId": "188DCD1EA7EFC9652E31BAF8BF62A2B8/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00004/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR01gNodeBRadio00004/NR01gNodeBRadio00004/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-05T11:36:30.421Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-06T07:38:00.067Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 2482
    },
    "versionNumber": 2,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 2478
    }
}, {
    "type": "ctw/gnbdu",
    "id": 59,
    "href": "ctw/gnbdu/59",
    "externalId": "D51D713178E554378D1EAC87B1740BC8/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00003/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:20:05.771Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:20:05.771Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 59
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 59
    }
}, {
    "type": "ctw/gnbdu",
    "id": 81,
    "href": "ctw/gnbdu/81",
    "externalId": "56B00962C8E55623C2F3A5A29BD9D795/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00005/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:24:30.250Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:24:30.250Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 81
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 81
    }
}, {
    "type": "ctw/gnbdu",
    "id": 26,
    "href": "ctw/gnbdu/26",
    "externalId": "92F1CB35798FD7D13BCC6FF825D89CD6/ericsson-enm-ComTop:ManagedElement=NR03gNodeBRadio00002/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-04T13:15:39.707Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-04T13:15:39.707Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 26
    },
    "versionNumber": 1,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 26
    }
}, {
    "type": "ctw/gnbdu",
    "id": 111111,
    "href": "ctw/gnbdu/2478",
    "externalId": "188DCD1EA7EFC9652E31BAF8BF62A2B8/ericsson-enm-ComTop:ManagedElement=NR01gNodeBRadio00004/ericsson-enm-GNBDU:GNBDUFunction=1",
    "name": "Europe/Ireland/NR01gNodeBRadio00004/NR01gNodeBRadio00004/1",
    "revisionNumber": 1,
    "createdOn": "2022-04-05T11:36:30.421Z",
    "createdBy": "sysadm",
    "status": "operating",
    "lastModifiedOn": "2022-04-06T07:38:00.067Z",
    "lastModifiedBy": "sysadm",
    "revisionGroupKey": {
        "type": "revisionGroupKey",
        "keyValue": 2482
    },
    "versionNumber": 2,
    "gnbduId": 1,
    "key": {
        "type": "ctw/gnbduKey",
        "keyValue": 2478
    }
}]
"""


    @Before
    void init() throws IOException {
        System.setProperty("commonTopologyServiceUrl", gnbdusUrl)
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8003)

    @Rule
    public ExpectedException thrown = ExpectedException.none()


    @Test
    @Ignore
    void TestSelectGnbsForSuccessHttp() {
        gnbList.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        gnbList.add("gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A", "gNBId": 17)
        gnbList.add("gNBName": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6", "gNBId": 26)
        gnbList.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1", "gNBCmHandle": "D51D713178E554378D1EAC87B1740BC8", "gNBId": 59)
        selectedGnbs.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        selectedGnbs.add("gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A", "gNBId": 17)
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)))

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
        assertEquals(gnbList, delegateExecution.getVariable("gNBList"))
        assertEquals(selectedGnbs, delegateExecution.getVariable("selectedGnbs"))
    }

    @Test
    void givenRequestTimeOutWhenSelectGnbsThenBpmnError() {
        stubFor(get(urlPathEqualTo(gnbdusListPath))
            .willReturn(aResponse()
            .withFixedDelay(20000)
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body)))
        thrown.expect(BpmnError.class)
        thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "cts.error.connectionFailed"))

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
    }

    @Test
    void TestSelectGnbsForErrorHttp() {
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")))
        thrown.expect(BpmnError.class)
        thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "cts.error.connectionFailed") +
                ": 404")

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
    }


    @Test
    void TestSelectGnbsForEmptyResponseBody() {
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")))
        thrown.expect(BpmnError.class)
        thrown.expectMessage(ArcFlowUtils
                .getMessage(delegateExecution, "request.error.schemaValidation"))

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
    }

    @Test
    @Ignore
    void TestSelectGnbsWithNegativeId() {
        gnbList.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        gnbList.add("gNBName": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6", "gNBId": 26)
        gnbList.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1", "gNBCmHandle": "D51D713178E554378D1EAC87B1740BC8", "gNBId": 59)

        selectedGnbs.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        selectedGnbs.add("gNBName": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6", "gNBId": 26)
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyNegID)))
        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
        assertEquals(gnbList, delegateExecution.getVariable("gNBList"))
        assertEquals(selectedGnbs, delegateExecution.getVariable("selectedGnbs"))
    }

    @Test
    @Ignore
    void TestSelectGnbsWithEmptygNBName() {
        gnbList.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        gnbList.add("gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A", "gNBId": 17)
        gnbList.add("gNBName": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6", "gNBId": 26)

        selectedGnbs.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        selectedGnbs.add("gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A", "gNBId": 17)
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyEmptyName)))
        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
        assertEquals(gnbList, delegateExecution.getVariable("gNBList"))
        assertEquals(selectedGnbs, delegateExecution.getVariable("selectedGnbs"))
    }

    @Test
    @Ignore
    void TestSelectGnbsWithLessThan2Gnbdus() {
        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyWithOneGNBDU)))

        thrown.expect(BpmnError.class)
        thrown.expectMessage(
                ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.gnbListSizeLessThan2"))

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")
    }

    @Test
    @Ignore
    void checkIsUniqueGnbdu() {
        gnbList.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        gnbList.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1","gNBCmHandle": "56B00962C8E55623C2F3A5A29BD9D795", "gNBId": 81)
        gnbList.add("gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A", "gNBId": 17)
        gnbList.add("gNBName": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6", "gNBId": 26)
        gnbList.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00004/NR01gNodeBRadio00004/1", "gNBCmHandle": "188DCD1EA7EFC9652E31BAF8BF62A2B8", "gNBId": 2478)
        gnbList.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1", "gNBCmHandle": "D51D713178E554378D1EAC87B1740BC8", "gNBId": 59)

        selectedGnbs.add("gNBName": "tc11-vdu/1", "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449", "gNBId": 3)
        selectedGnbs.add("gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1","gNBCmHandle": "56B00962C8E55623C2F3A5A29BD9D795", "gNBId": 81)

        stubFor(get(urlPathEqualTo(gnbdusListPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyWithDuplicate)))

        runFlowScript(delegateExecution, "groovy/SelectGNBDUsWithCTSProxy.groovy")

        assertEquals(gnbList, delegateExecution.getVariable("gNBList"))
        assertEquals(selectedGnbs, delegateExecution.getVariable("selectedGnbs"))
    }
}