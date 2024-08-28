// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import groovy.AssignOptimizationId
import groovy.json.JsonSlurper
import groovy.utils.ArcFlowUtils

import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class AssignOptimizationIdTest extends FlowAutomationScriptBaseTest {

  private static final String CREATE_OPTIMIZATION_PATH = "/optimizations/"
  private static final String CREATE_OPTIMIZATION_URL = "http://localhost:5001/optimizations/"
  private static final String FLOW_STEP = "Assigning Optimization ID"
  String expectedRequestBody = "{\"selectedNodes\":{\"gnbIdList\":[{\"gnbId\":3,\"cmHandle\":\"3F0EA5DD12A97B6F72ED2ED1ADD1D449\"}," +
          "{\"gnbId\":17,\"cmHandle\":\"2F546CEBA2B9B70D1400545EF042214A\"},{\"gnbId\":26,\"cmHandle\":\"92F1CB35798FD7D13BCC6FF825D89CD6\"}]}," +
          "\"unwantedNodePairs\":{\"gnbPairsList\":[{\"pGnbduId\":17," +
          "\"sGnbduId\":26}," +
          "{\"pGnbduId\":17,\"sGnbduId\":3}]}," +
          "\"mandatoryNodePairs\":{\"gnbPairsList\":[" +
          "{\"pGnbduId\":26,\"sGnbduId\":3}," +
          "{\"pGnbduId\":26,\"sGnbduId\":17}]}}"
  String message = ArcFlowUtils.getMessage(delegateExecution, "optimization.id.successful")
  List<Map<String, Object>> mandatoryNodePairs = provideMandatoryLinkList()
  List<Map<String, Object>> unwantedNodePairs = provideUnwantedLinkList()
  List<Map<String, Object>> gnbListToOptimize = provideNodeToOptimizeList()



  @Rule
  public WireMockRule wireMockRule = new WireMockRule(5001)

  @Before
  void init() {
    System.setProperty("optimizationUrl", CREATE_OPTIMIZATION_URL)
    delegateExecution.setVariable("gnbListToOptimize", gnbListToOptimize)
    delegateExecution.setVariable("mandatoryNodePairs", mandatoryNodePairs)
    delegateExecution.setVariable("unwantedNodePairs", unwantedNodePairs)
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Test
  void testCreateOptimizationSuccessHttpRequest() {
    stubFor(post(urlPathEqualTo(CREATE_OPTIMIZATION_PATH))
        .withRequestBody(equalToJson(expectedRequestBody))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('{"status": "Success", "optimizationId": "123", "result": "Optimization instance created."}')))
    runFlowScript(delegateExecution, "groovy/AssignOptimizationId.groovy")
    assertEquals("123", delegateExecution.getVariable("optimizationId"))
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO, message, FLOW_STEP)
  }

  @Test
  void testCreateOptimizationForFailedHttpRequest() {
    stubFor(post(urlPathEqualTo(CREATE_OPTIMIZATION_PATH))
         .withRequestBody(equalToJson(expectedRequestBody))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody('{"status": "failure", "optimizationId": "None" }')))
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "optimization.id.failure") + ": 400")
    runFlowScript(delegateExecution, "groovy/AssignOptimizationId.groovy")
  }

  @Test
  void testCreateOptimizationSuccessResponseWithInvalidResponseBody() {
    stubFor(post(urlPathEqualTo(CREATE_OPTIMIZATION_PATH))
            .withRequestBody(equalToJson(expectedRequestBody))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody('{"status": "success", "resul": "Optimization started", "optimizationId": 123}')))
    thrown.expect(BpmnError.class)
    thrown
            .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/AssignOptimizationId.groovy")
  }

  @Test
  void testBuildRequestBody() {
    assertEquals(expectedRequestBody, AssignOptimizationId.buildCreateOptimizationRequestBody(gnbListToOptimize,
        unwantedNodePairs, mandatoryNodePairs))
  }

  private static List<Map<String, Object>> provideNodeToOptimizeList() {
    List <Map<String, Object>> nodesToOptimize = []
    nodesToOptimize.add( "Name": "tc11-vdu/1", "gNBId": 3, "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449")
    nodesToOptimize.add( "Name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBId": 17, "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A")
    nodesToOptimize.add( "Name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBId": 26, "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6")
    return nodesToOptimize
  }

  private static List<Map<String, Object>> provideMandatoryLinkList() {
    List <Map<String, Object>> mandatoryLinkList = []
    mandatoryLinkList.add( "pGnbduId": 26, "sGnbduId": 3)
    mandatoryLinkList.add( "pGnbduId": 26, "sGnbduId": 17)
    return mandatoryLinkList
  }

  private static List<Map<String, Object>> provideUnwantedLinkList() {
    List <Map<String, Object>> mandatoryLinkList = []
    mandatoryLinkList.add( "pGnbduId": 17, "sGnbduId": 26)
    mandatoryLinkList.add( "pGnbduId": 17, "sGnbduId": 3)
    return mandatoryLinkList
  }
}