// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import com.github.tomakehurst.wiremock.junit.WireMockRule
import groovy.utils.ArcFlowUtils
import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class LinksDeploymentTest extends FlowAutomationScriptBaseTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8004)

  @Rule
  public ExpectedException expectedEx = ExpectedException.none()

  private static final String CONFIGURE_OPTIMIZATION_URL = "http://localhost:8004/configurations/"
  private static final String CONFIGURE_OPTIMIZATION_PATH = "/configurations/"

  final String BODY = """{"gnbIdList":[{"pGnbId":208731,"pCmHandler":"56B00962C8E55623C2F3A5A29BD9D795","sGnbId":208727,"sCmHandler":"3F0EA5DD12A97B6F72ED2ED1ADD1D449"},{"pGnbId":208729,"pCmHandler":"92F1CB35798FD7D13BCC6FF825D89CD6","sGnbId":208728,"sCmHandler":"2F546CEBA2B9B70D1400545EF042214A"},{"pGnbId":208729,"pCmHandler":"92F1CB35798FD7D13BCC6FF825D89CD6","sGnbId":208730,"sCmHandler":"D51D713178E554378D1EAC87B1740BC8"},{"pGnbId":208731,"pCmHandler":"56B00962C8E55623C2F3A5A29BD9D795","sGnbId":208730,"sCmHandler":"D51D713178E554378D1EAC87B1740BC8"},{"pGnbId":208728,"pCmHandler":"2F546CEBA2B9B70D1400545EF042214A","sGnbId":208729,"sCmHandler":"92F1CB35798FD7D13BCC6FF825D89CD6"},{"pGnbId":208730,"pCmHandler":"D51D713178E554378D1EAC87B1740BC8","sGnbId":208727,"sCmHandler":"3F0EA5DD12A97B6F72ED2ED1ADD1D449"},{"pGnbId":208730,"pCmHandler":"D51D713178E554378D1EAC87B1740BC8","sGnbId":208729,"sCmHandler":"92F1CB35798FD7D13BCC6FF825D89CD6"}]}"""

  final List OPTIMIZATION_RESULT = []
  @Before
  void setUp() throws IOException {
    System.setProperty("optimizationConfigurationUrl", CONFIGURE_OPTIMIZATION_URL)
    OPTIMIZATION_RESULT.add("Id": 1,
        "pGnbId": 208729,
        "pCmHandle": "1pstuffcmhandle",
        "sGnbId": 208728,
        "sCmHandle": "1sstuffcmhandle",
        "usability": "0.3809468211368755")
    OPTIMIZATION_RESULT.add("Id": 2,
        "pGnbId": 208719,
        "pCmHandle": "2pstuffcmhandle",
        "sGnbId": 208718,
        "sCmHandle": "2sstuffcmhandle",
        "usability": "0.3809468211368715")
  }

  @Test
  void testStartLinksDeploymentNoConfigureBBpairsForSuccessResponse() {
    stubFor(post(urlPathEqualTo(CONFIGURE_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(BODY)))
    delegateExecution.setVariable("optimizationResultTable", OPTIMIZATION_RESULT)
    delegateExecution.setVariable("ConfigureBBpairs.YesRadioButton", false)
    runFlowScript(delegateExecution, "groovy/LinksDeployment.groovy")

    final String FLOW_STEP         = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.deployment")
    final String OCCURRENCE_INFO   = delegateExecution.getVariable("occurrenceInfo")
    final String MESSAGE_SENT      = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.sentConfiguration")
    final String MESSAGE_APPLIED   = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.receivedConfiguration")


    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + MESSAGE_SENT as String,
            FLOW_STEP)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + MESSAGE_APPLIED as String,
            FLOW_STEP)

  }

  @Test
  void testStartLinksDeploymentYesConfigureBBpairsForSuccessResponse() {
    stubFor(post(urlPathEqualTo(CONFIGURE_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(BODY)))
    delegateExecution.setVariable("ConfigureBBpairs.YesRadioButton", true)
    delegateExecution.setVariable("optimizationResultTable", OPTIMIZATION_RESULT)
    runFlowScript(delegateExecution, "groovy/LinksDeployment.groovy")

    final String FLOW_STEP         = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.deployment")
    final String OCCURRENCE_INFO   = delegateExecution.getVariable("occurrenceInfo")
    final String MESSAGE_SENT      = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.sentConfiguration")
    final String MESSAGE_APPLIED   = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.receivedConfiguration")

    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + MESSAGE_SENT as String,
            FLOW_STEP)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + MESSAGE_APPLIED as String,
            FLOW_STEP)
  }

  @Test
  void testStartLinksDeploymentForFailureResponse() {
    stubFor(post(urlPathEqualTo(CONFIGURE_OPTIMIZATION_PATH))
            .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody(BODY)))
    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
            ArcFlowUtils.getMessage(delegateExecution, "applyConf.error.unsuccessfulRequest"))
    delegateExecution.setVariable("optimizationResultTable", OPTIMIZATION_RESULT)
    runFlowScript(delegateExecution, "groovy/LinksDeployment.groovy")

    final String FLOW_STEP         = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.deployment")
    final String OCCURRENCE_INFO   = delegateExecution.getVariable("occurrenceInfo")
    final String MESSAGE_SENT      = ArcFlowUtils.getMessage(delegateExecution, "wf.execute.sentConfiguration")
    final String ERROR_MESSAGE     = ArcFlowUtils.getMessage(delegateExecution, "applyConf.error.unsuccessfulRequest")

    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + MESSAGE_SENT as String,
            FLOW_STEP)
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.INFO,
            OCCURRENCE_INFO + ERROR_MESSAGE as String,
            FLOW_STEP)
  }
}