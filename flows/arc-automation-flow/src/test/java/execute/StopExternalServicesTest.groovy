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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.delete
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class StopExternalServicesTest extends FlowAutomationScriptBaseTest {

  private static final String STOP_OPTIMIZATION_PATH = "/optimizations/" + OPTIMIZATION_ID.toString() + "/stop"
  private static final String OPTIMIZATION_URL = "http://localhost:5001/optimizations/"
  private static final String OPTIMIZATION_ID = "96567"

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(5001)

  @Before
  void init() {
    System.setProperty("optimizationUrl", OPTIMIZATION_URL)
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Test
  void testStopOptimizationForFailedHttpConnection() {
    stubFor(post(urlPathEqualTo(STOP_OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(500)
            .withFixedDelay(30000)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "error",
  "result": "Optimization ID not found"
}''')))
    delegateExecution.setVariable("PMDataStarted", true)
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.arcOptimizationResponse.failure"))
    runFlowScript(delegateExecution, "groovy/StopExternalServices.groovy")
  }


  @Test
  void testStopOptimizationForFailedSuccessResponse() {
    stubFor(post(urlPathEqualTo(STOP_OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(500)
            .withHeader("Content-Type", "application/json")
            .withBody('''{
  "status": "error",
  "result": "Optimization ID not found"
}''')))
    delegateExecution.setVariable("PMDataStarted", true)
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.arcOptimizationResponse.failure"))
    runFlowScript(delegateExecution, "groovy/StopExternalServices.groovy")
  }



  @Test
  void testStopOptimizationForSuccessResponse() {
    stubFor(post(urlPathEqualTo(STOP_OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody('{"status": "Success", "result": "Optimization stopped"}')))
    delegateExecution.setVariable("PMDataStarted", true)
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    runFlowScript(delegateExecution, "groovy/StopExternalServices.groovy")
    String flowStep = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.stopExternalServices")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
        ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.start"),
        flowStep)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
        ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.arcOptimizationResponse.successful"),
        flowStep)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
        ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.successful"),
        flowStep)

  }

  @Test
  void testStrongOptimizationForCheckingWrongSchema() {
    stubFor(post(urlPathEqualTo(STOP_OPTIMIZATION_PATH))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody()))
    delegateExecution.setVariable("PMDataStarted", true)
    delegateExecution.setVariable("optimizationId", OPTIMIZATION_ID)
    thrown.expect(BpmnError.class)
    thrown
        .expectMessage(ArcFlowUtils.getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/StopExternalServices.groovy")
  }

  @Test
  void testNothingToStop() {
    delegateExecution.setVariable("PMDataStarted", false)
    delegateExecution.setVariable("optimizationId", "None")
    runFlowScript(delegateExecution, "groovy/StopExternalServices.groovy")
    String flowStep = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.stopExternalServices")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
        ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.start"),
        flowStep)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
        ArcFlowUtils.getMessage(delegateExecution, "stopExternalServices.successful"),
        flowStep)
  }
}