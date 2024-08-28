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
import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class OptimizationTest extends FlowAutomationScriptBaseTest {

  private static final String OPTIMIZATION_PATH = "/optimizations/123/status"
  private static final String OPTIMIZATION_URL = "http://localhost:5001/optimizations/"
  private static final String FLOW_STEP = "Assigning Optimization ID"
  private static final String OPTIMIZATION_ID = "123"


  @Rule
  public WireMockRule wireMockRule = new WireMockRule(5001)
  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Before
  void setUp() throws IOException {
    System.setProperty("optimizationUrl", OPTIMIZATION_URL)
  }

  @Test
  void testOptimizationForFailedHttpRequestTimeout() {
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withFixedDelay(30000)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
                "status": "finished",
                "result":[]
    }''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    final String occurrenceInfo =
        ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
            String.valueOf(1) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(occurrenceInfo +
        ArcFlowUtils.getMessage(delegateExecution, "optimization.error.connectionFailed"))
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
  }

  @Test
  @Ignore
  void testOptimizationForSuccessHttp() {
    List optimizationResult = []
    optimizationResult.add("Id": 1,
        "pGnbId": 208729,
        "sGnbId": 208728,
        "usability": "0.3809468211368755")
    optimizationResult.add("Id": 2,
        "pGnbId": 208719,
        "sGnbId": 208718,
        "usability": "0.3809468211368715")
    optimizationResult.add("Id": 3,
            "pGnbId": 208716,
            "sGnbId": 208717,
            "usability": "Nan")
    stubFor(get(urlPathEqualTo("/optimizations/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
          "status": "Optimization finished",
          "result":[
            {"pGnbId": 208729,"sGnbId": 208728,"usability": 0.3809468211368755},
            {"pGnbId": 208719,"sGnbId": 208718,"usability": 0.3809468211368715},
            {"pGnbId": 208716,"sGnbId": 208717,"usability": 0.0}
      ]
    }
    ''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
    assertEquals("Optimization finished",
        getExecutionReportVariableContent(flowExecution, "statusOfOptimization"))
    assertEquals("finished", delegateExecution.getVariable("status"))
    assertEquals(optimizationResult, delegateExecution.getVariable("optimizationResultTable"))
  }

  @Test
  @Ignore
  void testOptimizationForSuccessHttpWithEmptyResult() {
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "Optimization finished",
  "result":[]
}''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
    assertEquals([], delegateExecution.getVariable("optimizationResultTable"))
    final String FLOW_STEP = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.optimization")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.WARNING,
        "No target optimization partners found. Moving onto next available optimization.",
        FLOW_STEP)
  }

  @Test
  void testOptimizationForFailedHttp() {
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(500)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "error",
  "result": []
}''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    final String occurrenceInfo =
        ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
            String.valueOf(3) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(occurrenceInfo +
        ArcFlowUtils.getMessage(delegateExecution, "optimization.error.connectionFailed") +
        " (500)")
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
  }

  @Test
  void testOptimizationForEmptyHttpResponse() {
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("")))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    final String occurrenceInfo =
        ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
            String.valueOf(6) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
  }

  @Test
  void testOptimizationForWrongPairsAttributeInHttpResponse() {
    List optimizationResult = []
    optimizationResult.add("Id": 1,
        "pGnbId": 204719,
        "sGnbId": 208718,
        "usability": 0.3809468211368755)
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "finished",
  "result": [
    {"pGnbId": null, "sGnbId": 208720, "usability": 0.3809468211368750},
    {"pGnbId": 204719, "sGnbId": 208718, "usability": 0.3809468211368755},
    {"pGnbId": 208719, "usability": 0.3809468211368715}
  ]
}''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    final String occurrenceInfo =
        ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
            String.valueOf(1) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
  }

  @Test
  void testOptimizationWhenOptimizationOngoing() {
    stubFor(get(urlPathEqualTo(OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "ongoing",
  "result": []
}''')))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    runFlowScript(delegateExecution, "groovy/Optimization.groovy")
    assertEquals("ongoing",
        getExecutionReportVariableContent(flowExecution, "statusOfOptimization"))
    assertEquals(null, delegateExecution.getVariable("optimizationResultTable"))
  }
}
