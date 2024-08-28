// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import groovy.json.JsonSlurper
import groovy.utils.ArcFlowUtils
import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.put
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class TriggerUeMeasurementTest extends FlowAutomationScriptBaseTest {

  final String ncmpUrlProperties = 'http://localhost:8002/v1/CM/CoverageDataMeasurementgNB/'
  final String wrongNcmpUrlProperties = 'http://wrongUrl:8002/v1/CM/CoverageDataMeasurementgNB/'
  final String urlPath = "/v1/CM/CoverageDataMeasurementgNB/"
  List<String> gnbIdList = ["208727", "208728", "208729"]
  List<String> gnbIdNotFoundList = ["222222"]


  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8002)

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Before
  void setUp() throws IOException {
    System.setProperty("ncmpUrl", ncmpUrlProperties)

  }

  @Test
  @Ignore
  void givenGnbListWhenTriggerMeasurementsThenSuccessHttpWithEnabledGnbListAndNotFoundGnbList() {
    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('{' + '"ListofEnabledgNB":[208727,208728,208729]' +
                ',"ListofNotFoundGNB":[222222]' + '}')))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")

    assertEquals(gnbIdList, delegateExecution.getVariable("gnbIdList"))
    assertEquals(gnbIdNotFoundList, delegateExecution.getVariable("gnbNotFoundList"))
    assertEquals(expectedNodeToOptimizeList(), delegateExecution.getVariable("gnbListToOptimize"))
    assertEquals(expectedMandatoryLinkList(), delegateExecution.getVariable("mandatoryNodePairs"))
    assertEquals(expectedUnwantedLinkList(), delegateExecution.getVariable("unwantedNodePairs"))
  }

  @Test
  @Ignore
  void givenResponseCode422WhenTriggerMeasurementsThenBpmnError() {

    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(422)
            .withHeader("Content-Type", "application/text")
            .withBody("Unprocessable entity")))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "request.error.responseCode") + ": 422")
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }


  @Test
  @Ignore
  void givenBadResponseBodyWhenTriggerMeasurementsThenBpmnError() {

    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"A\":\"123\"}")))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown
        .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }

  @Test
  @Ignore
  void givenWrongUrlWhenTriggerMeasurementsThenBpmnError() {

    System.setProperty("ncmpUrl", wrongNcmpUrlProperties)
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown.expectMessage("Service connection is not possible.")
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }

  @Test
  @Ignore
  void givenResponseBodyIsNullOrEmptyWhenTriggerMeasurementsThenSchemaException() {

    System.setProperty("ncmpUrl", ncmpUrlProperties)

    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown
        .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }


  @Test
  @Ignore
  void givenGnbIdNotFoundIsNullWhenTriggerMeasurementsThenSchemaException() {

    System.setProperty("ncmpUrl", ncmpUrlProperties)

    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ListofEnabledgNB\":[" + "\"208727\"," + "\"208728\"," + "\"208729\"]," +
                "\"ListofNotFoundGNB\":null}")))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown
        .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }

  @Test
  @Ignore
  void givenWrongJsonStructureWhenTriggerMeasurementsThenSchemaException() {

    System.setProperty("ncmpUrl", ncmpUrlProperties)

    stubFor(put(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ListofEnabledgNB\":[" + "\"ListofNotFoundGNB\":[" + "\"222222\"]" +
                "\"208727\"," + "\"208728\"," + "\"208729\"]}")))
    delegateExecution.setVariable("flowInput", getFlowInputStub())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "ncmp.error.connectionFailed"))
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.jsonParsing"))
    runFlowScript(delegateExecution, "groovy/TriggerUeMeasurement.groovy")
  }

  private static List expectedNodeToOptimizeList() {
    List <?> nodesToOptimize = []
    nodesToOptimize.add( "CmHandle": "gSZGcmHalndle1", "Name": "tc11-vdu/1", "gNBId": 208727)
    nodesToOptimize.add( "CmHandle": "gSZGcmHalndle1", "Name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBId": 208728)
    nodesToOptimize.add( "CmHandle": "gSZGcmHalndle1", "Name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBId": 208729)
    return nodesToOptimize
  }

  private static List expectedMandatoryLinkList() {
    List <?> nodesToOptimize = []
    nodesToOptimize.add( "pGnbduId": 208727, "sGnbduId": 208728)
    nodesToOptimize.add( "pGnbduId": 208728, "sGnbduId": 208729)
    nodesToOptimize.add( "pGnbduId": 208727, "sGnbduId": 208729)
    return nodesToOptimize
  }

  private static List expectedUnwantedLinkList() {
    List <?> unwantedLinkList = []
    return unwantedLinkList
  }

  static Object getFlowInputStub() {

    return new JsonSlurper().parseText('''
      {
          "selectGNBDUs": {
              "table": [
                  {
                      "Name": "tc11-vdu/1",
                      "CmHandle": "gSZGcmHalndle1",
                      "gNBId": 208727
                  },
                  {
                      "Name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
                      "CmHandle": "gSZGcmHalndle1",
                      "gNBId": 208728
                  },
                  {
                      "Name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1",
                      "CmHandle": "gSZGcmHalndle1",
                      "gNBId": 208729
                  }
              ]
          },"mandatoryGNodeBPairsSelection" : {
              "primaryGNBDUMandatory" : [ ],
              "secondaryGNBDUMandatory" : [ ],
              "readOnlyTable" : [
                  {
                      "pGnbduId": 208727,
                      "sGnbduId": 208728
                  },
                  {
                      "pGnbduId": 208728,
                      "sGnbduId": 208729
                  },
                  {
                      "pGnbduId": 208727,
                      "sGnbduId": 208729
                  }
              ]
          },"unwantedGNodeBPairsSelection" : {
              "primaryGNBDUExclusion" : [ ],
              "secondaryGNBDUExclusion" : [ ],
              "readOnlyTable" : [ ]
          }
      }
''') as Map
  }
}
