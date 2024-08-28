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

import static com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity.*
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class ValidateSetupFileInputTest extends FlowAutomationScriptBaseTest {


  static String gnbdusUrl = "http://localhost:8003/ctw/gnbdu/all/"
  static String gnbdusListPath = "/ctw/gnbdu/all/"
  static String body = """
[
  {
    "type": "ctw/gnbdu","id": 117,"href": "ctw/gnbdu/117", 
    "externalId":"3F0EA5DD12A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1", 
    "name": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/1",
    "revisionNumber": 1,"createdOn": "2022-02-17T10:22:57.884Z","createdBy": "sysadm",
    "status": "operating","lastModifiedOn": "2022-02-17T10:22:57.884Z","lastModifiedBy": "sysadm",
    "revisionGroupKey": {"type": "revisionGroupKey","keyValue": 117},"versionNumber": 1,
    "key": {"type": "ctw/gnbduKey","keyValue": 117}
  },
  {
    "type": "ctw/gnbdu","id": 118,"href": "ctw/gnbdu/118",
    "externalId":"3FGHJFI2222A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
    "revisionNumber": 1,"createdOn": "2022-02-17T10:23:13.740Z","createdBy": "sysadm",
    "status": "operating","lastModifiedOn": "2022-02-17T10:23:13.740Z","lastModifiedBy": "sysadm",
    "revisionGroupKey": {"type": "revisionGroupKey","keyValue": 118},"versionNumber": 1,
    "key": {"type": "ctw/gnbduKey","keyValue": 118}
  },
  {
    "type": "ctw/gnbdu","id": 119,"href": "ctw/gnbdu/119",
    "externalId":"3F03399992A97B6F72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3",
    "revisionNumber": 1,"createdOn": "2022-02-17T10:23:16.913Z","createdBy": "sysadm",
    "status": "operating","lastModifiedOn": "2022-02-17T10:23:16.913Z","lastModifiedBy": "sysadm",
    "revisionGroupKey": {"type": "revisionGroupKey","keyValue": 119},"versionNumber": 1,
    "key": {"type": "ctw/gnbduKey","keyValue": 119}
  },
  {
    "type": "ctw/gnbdu","id": 120,"href": "ctw/gnbdu/120",
    "externalId":"3F0E888888KKF72ED2ED1ADD1D449/_3gpp-common-managed-element:ManagedElement=tc11-vdu/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction=1",
    "name": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4",
    "revisionNumber": 1,"createdOn": "2022-02-17T10:22:57.884Z","createdBy": "sysadm",
    "status": "operating","lastModifiedOn": "2022-02-17T10:22:57.884Z","lastModifiedBy": "sysadm",
    "revisionGroupKey": {"type": "revisionGroupKey","keyValue": 120},"versionNumber": 1,
    "key": {"type": "ctw/gnbduKey","keyValue": 120}
  }
]"""

  def flowInput

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8003)

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  String flowStep

  @Before
  void init() throws IOException {
    System.setProperty("commonTopologyServiceUrl", gnbdusUrl)
    flowStep = ArcFlowUtils.getMessage(delegateExecution, "wf.setup.validateSetupFileInput")
    flowInput = [:]
    flowInput.unwantedGNodeBPairsSelection = ["readOnlyTable" : []]
    flowInput.mandatoryGNodeBPairsSelection = ["readOnlyTable" : []]
  }


  @Test
  @Ignore
  void successDuplicatedMixed() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]]

    List<?> expectedTable = [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBCmHandle": "3FGHJFI2222A97B6F72ED2ED1ADD1D449", "gNBId": 118],
                             ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBCmHandle": "3F03399992A97B6F72ED2ED1ADD1D449", "gNBId": 119],
                             ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]

    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    String msg = ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.duplicateGnbdus")
    checkExecutionEventIsRecorded(flowExecution, WARNING, msg, flowStep)
    assertEquals(expectedTable, delegateExecution.getVariable("gnbduList"))
  }


  @Test
  void errorEmptyResponseHttp() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119]]]
    delegateExecution.setVariable("flowInput", flowInput)
    withResponse("", 200)
    thrown.expect(BpmnError.class)
    thrown
        .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.responseParsing"))
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
  }

  @Test
  void errorAccessHttp() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119]]]
    delegateExecution.setVariable("flowInput", flowInput)
    withResponse(body, 422)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "cts.error.connectionFailed"))
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
  }

  @Test
  @Ignore
  void gNBsNotAvailable() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119],
                 ["gNBName": "Kisqsdta_2", "gNBId": 20541398729]]]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    thrown.expect(BpmnError.class)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    checkExecutionEventIsRecorded(flowExecution, ERROR,
        'Invalid GNBDUs table: {"gNBName": Kisqsdta_2, "gNBId": 20541398729} Not an available node.',
        flowStep)
  }

  @Test
  @Ignore
  void checkDuplicateGnbdus() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/1", "gNBId": 117],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBId": 119]]]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    String message =
        ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.duplicateGnbdus")
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    checkExecutionEventIsRecorded(flowExecution, WARNING, message,
        flowStep)
  }

  @Test
  @Ignore
  void checkDuplicateWith2Gnbdus() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/1", "gNBId": 117],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/1", "gNBId": 117]]]
    withResponse(body, 200)

    thrown.expect(BpmnError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.gnbListSizeLessThan2"))

    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
  }

  static void withResponse(String body, int status) {
    stubFor(get(urlPathEqualTo(gnbdusListPath))
        .willReturn(aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)))
  }

  @Test
  @Ignore
  void successCheckExcludedLinks() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBCmHandle": "3FGHJFI2222A97B6F72ED2ED1ADD1D449", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBCmHandle": "3F03399992A97B6F72ED2ED1ADD1D449", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]]
    flowInput.unwantedGNodeBPairsSelection.readOnlyTable = [[
                                                          "pGnbduId": 118, "pGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
                                                          "sGnbduId": 119, "sGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3"
                                                      ]
    ]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
  }

  @Test
  @Ignore
  void errorCheckExcludedLinksEqualIds() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBCmHandle": "3FGHJFI2222A97B6F72ED2ED1ADD1D449", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBCmHandle": "3F03399992A97B6F72ED2ED1ADD1D449", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]]
    flowInput.unwantedGNodeBPairsSelection.readOnlyTable = [[
                                                          "pGnbduId": 118, "pGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
                                                          "sGnbduId": 118, "sGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2"
                                                      ]
    ]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    thrown.expect(BpmnError.class)
    String msg =
        ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.invalidPairConstraints")
    thrown.expectMessage(msg)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    checkExecutionEventIsRecorded(flowExecution, ERROR, msg, flowStep)
  }

  @Test
  @Ignore
  void errorCheckExcludedLinksInvalidIds() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBCmHandle": "3FGHJFI2222A97B6F72ED2ED1ADD1D449", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBCmHandle": "3F03399992A97B6F72ED2ED1ADD1D449", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]]
    flowInput.unwantedGNodeBPairsSelection.readOnlyTable = [[
                                                          "pGnbduId": 322, "pGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
                                                          "sGnbduId": 118, "sGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2"
                                                      ]
    ]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    thrown.expect(BpmnError.class)
    String msg =
        ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.invalidPairConstraints")
    thrown.expectMessage(msg)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    checkExecutionEventIsRecorded(flowExecution, ERROR, msg, flowStep)
  }

  @Test
  @Ignore
  void errorCheckExcludedLinksInvalidNames() {
    flowInput.selectGNBDUs =
        [table: [["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2", "gNBCmHandle": "3FGHJFI2222A97B6F72ED2ED1ADD1D449", "gNBId": 118],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/3", "gNBCmHandle": "3F03399992A97B6F72ED2ED1ADD1D449", "gNBId": 119],
                 ["gNBName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4", "gNBCmHandle": "3F0E888888KKF72ED2ED1ADD1D449", "gNBId": 120]]]
    flowInput.unwantedGNodeBPairsSelection.readOnlyTable = [
        [
            "pGnbduId": 118, "pGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
            "sGnbduId": 120, "sGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4"
        ],
        [
            "pGnbduId": 118, "pGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/2",
            "sGnbduId": 119, "sGnbduName": "Europe/Ireland/NR45gNodeBRadio00022/NR45gNodeBRadio00022/4"
        ]
    ]
    withResponse(body, 200)
    delegateExecution.setVariable("flowInput", flowInput)
    thrown.expect(BpmnError.class)
    String msg =
        ArcFlowUtils.getMessage(delegateExecution, "selectGnbdus.error.invalidPairConstraints")
    thrown.expectMessage(msg)
    runFlowScript(delegateExecution, "groovy/ValidateSetupFileInput.groovy")
    checkExecutionEventIsRecorded(flowExecution, ERROR, msg, flowStep)
  }
}