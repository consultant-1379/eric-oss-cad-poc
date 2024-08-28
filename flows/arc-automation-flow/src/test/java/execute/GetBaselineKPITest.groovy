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
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import com.github.tomakehurst.wiremock.junit.WireMockRule

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static org.junit.Assert.assertEquals

class GetBaselineKPITest extends FlowAutomationScriptBaseTest {

  private static final String GET_BASELINE_KPI_PATH = "/kpis/"
  private static final String GET_BASELINE_KPI_URL = "http://localhost:5003/kpis/"

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(5003)
  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Before
  void setUp() throws IOException {
    System.setProperty("kpisUrl", GET_BASELINE_KPI_URL)
  }

  @Test
  void testBaselineKPIForSuccessHttp() {
    String index = "5:3:0"
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\","+ "\"result\" : \"KPIs has been retrieved successfully.\"," + "\"KpiData\" : {" + "\"KPI1\": 5," + "\"KPI2\": 3," +
                        "\"KPI3\": 0 }}")))
    ArrayList kpisData = []
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    delegateExecution.setVariable("kpisData", kpisData)
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
    assertEquals("5", getExecutionReportVariableContent(flowExecution, index + ".kpi1"))
    assertEquals("3", getExecutionReportVariableContent(flowExecution, index + ".kpi2"))
    assertEquals("0", getExecutionReportVariableContent(flowExecution, index + ".kpi3"))
    assertEquals(index, getExecutionReportVariableContent(flowExecution, "kpisValuesList"))
  }

  @Test
  void testBaselineKPIWithServerError() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody("Error")))
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest") + ": 404")
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  @Test
  void testBaselineKPIWithBodyThatContainsNullValues() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"result\" : \"KPIs has been retrieved successfully.\"," + "\"KpiData\" : {" + "\"KPI1\": 5," + "\"KPI2\": null," +
                    "\"KPI3\": 0 }}")))
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  @Test
  void testBaselineKPIWithWrongResponseBody() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"wrongKey\" : {" + "\"KPI1\": 5," + "\"KPI2\": 3," +
                    "\"KPI3\": 0 }}")))
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  @Test
  void testBaselineKPIWithEmptyKPIDataBody() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"status\":\"success\"," + "\"result\" : \"KPIs has been retrieved successfully.\"," + "\"KpiData\" : {}}")))
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(delegateExecution, "kpi.error.emptyKpiData"))
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  @Test
  void testBaselineKPIWithCorruptedFloatKPIDataBody() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"result\" : {" + "\"KPI1\": 5," + "\"KPI2\": 3," +
                    "\"KPI3\": 0," + "\"KPI5\": 1.03 }}")))
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  @Test
  void testBaselineKPIRetrieveRequestWithFixedServerDelay() {
    stubFor(post(urlPathEqualTo(GET_BASELINE_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withFixedDelay(10000)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"result\" : \"KPIs has been retrieved successfully.\"," + "\"KpiData\" : {" + "\"KPI1\": 5 ," + "\"KPI2\": 3 ," +
                    "\"KPI3\": 0 }}")))
    delegateExecution.setVariable("gnbListToOptimize", provideNodeToOptimizeList())
    thrown.expect(BpmnError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest") +
            ": Service connection is not possible")
    runFlowScript(delegateExecution, "groovy/GetBaselineKPI.groovy")
  }

  private List provideNodeToOptimizeList() {
    List <?> nodesToOptimize = []
    nodesToOptimize.add( "Name": "tc11-vdu/1", "gNBId": 3, "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449")
    nodesToOptimize.add( "Name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBId": 17, "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A")
    nodesToOptimize.add( "Name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBId": 26, "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6")
    return nodesToOptimize
  }
}
