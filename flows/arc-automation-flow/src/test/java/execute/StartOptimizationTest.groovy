// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import groovy.utils.ArcFlowUtils

import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class StartOptimizationTest extends FlowAutomationScriptBaseTest {

  private static final String START_OPTIMIZATION_PATH = "/optimizations/123/start"
  private static final String START_OPTIMIZATION_URL = "http://localhost:8003/optimizations/"
  private static final String OPTIMIZATION_ID = "123"
  private final String FLOW_STEP = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.startOptimization")

  String message = ArcFlowUtils.getMessage(delegateExecution, "startOptimization.status.successfulStart")

  @Before
  void init() {
    System.setProperty("optimizationUrl", START_OPTIMIZATION_URL)
  }

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8003)

  @Rule
  public ExpectedException thrown = ExpectedException.none()


  @Test
  void testStartOptimizationForFailedHttpRequest() {
    stubFor(post(urlPathEqualTo(START_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(499)
                    .withHeader("Content-Type", "application/json")
                    .withBody('''{"status": "error", "result": "Optimization already started."}''')))

    final String occurrenceInfo =
            ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
                    String.valueOf(1) + "; "
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)

    thrown.expect(BpmnError.class)
    thrown.expectMessage(occurrenceInfo +
            ArcFlowUtils
                    .getMessage(delegateExecution, "startOptimization.error.optimizationNotStarted") +
            ": 499")
    runFlowScript(delegateExecution, "groovy/StartOptimization.groovy")
  }

  @Test
  void testStartOptimizationForSuccessHttpRequest() {
    stubFor(post(urlPathEqualTo(START_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody('{"status": "success", "result": "Optimization started"}'.toString())))
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    final String occurrenceInfo =
              ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
                      String.valueOf(1) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)

    runFlowScript(delegateExecution, "groovy/StartOptimization.groovy")

    assertEquals("Optimization number: 1; In progress", getExecutionReportVariableContent(flowExecution, "statusOfOptimization"))
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO, message, FLOW_STEP)
  }

  @Test
  void testStartOptimizationWithInvalidResponseBody() {
    stubFor(post(urlPathEqualTo(START_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody('{"status": "success", "resul": "Optimization started"}')))

    final String occurrenceInfo =
            ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
                    String.valueOf(1) + "; "
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    thrown.expect(BpmnError.class)
    thrown
            .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/StartOptimization.groovy")
  }

  @Test
  void testStartOptimizationWithEmptyOrNullBody() {
    stubFor(post(urlPathEqualTo(START_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")))

    final String occurrenceInfo =
            ArcFlowUtils.getMessage(delegateExecution, "optimization.occurrenceNumber") + ": " +
                    String.valueOf(1) + "; "
    delegateExecution.setVariable("occurrenceInfo", occurrenceInfo)
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)

    thrown.expect(BpmnError.class)
    thrown
            .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/StartOptimization.groovy")
  }

}