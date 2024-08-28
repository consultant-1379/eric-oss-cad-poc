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

class GetTriggeredKPITest extends FlowAutomationScriptBaseTest {

  private static final String GET_KPI_URL = "http://localhost:5002/kpis/"
  private static final String GET_KPI_PATH = "/kpis/"

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(5002)

  @Rule
  public ExpectedException expectedEx = ExpectedException.none()

  @Before
  void setUp() throws IOException {
    System.setProperty("kpisUrl", GET_KPI_URL)
  }

  @Test
  void testGetKpiDataForSuccessHttpRequest() {
    List <?> nodesToOptimize = []
    nodesToOptimize.add( "Name": "tc11-vdu/1", "gNBId": 3, "gNBCmHandle": "3F0EA5DD12A97B6F72ED2ED1ADD1D449")
    nodesToOptimize.add( "Name": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1", "gNBId": 17, "gNBCmHandle": "2F546CEBA2B9B70D1400545EF042214A")
    nodesToOptimize.add( "Name": "Europe/Ireland/NR03gNodeBRadio00002/NR03gNodeBRadio00002/1", "gNBId": 26, "gNBCmHandle": "92F1CB35798FD7D13BCC6FF825D89CD6")
    String index = "5:3:0"
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                            "{\"status\":\"success\"," + "\"result\" : \"KPIs has been retrieved successfully.\"," + "\"KpiData\" : {" + "\"KPI1\": 5," + "\"KPI2\": 3," +
                                    "\"KPI3\": 0 }}")))
    ArrayList kpisData = []
    delegateExecution.setVariable("kpisData", kpisData)
    delegateExecution.setVariable("gnbListToOptimize", nodesToOptimize)
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
    assertEquals("5", getExecutionReportVariableContent(flowExecution, index + ".kpi1"))
    assertEquals("3", getExecutionReportVariableContent(flowExecution, index + ".kpi2"))
    assertEquals("0", getExecutionReportVariableContent(flowExecution, index + ".kpi3"))
    assertEquals(index, getExecutionReportVariableContent(flowExecution, "kpisValuesList"))
  }

  @Test
  void testGetKpiDataForFailedHttpRequestTimeout() {
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
        .willReturn(aResponse()
        .withFixedDelay(20000)
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"result\" : {" + "\"KPI1\": 5," + "\"KPI2\": 3," +
                    "\"KPI3\": 0 }}")))
    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest"))
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
  }

  @Test
  void testGetKpiDataForInvalidHttpResponseContainsNullValue() {
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"status\":\"success\"," + "\"result\" : {" + "\"KPI1\": 5," + "\"KPI2\": null," +
                    "\"KPI3\": 0 }}")))
    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest"))
    expectedEx.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
  }

  @Test
  void testGetKpiDataForInvalidResponseEmptyOrNull() {
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")))

    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest") + ": " +
            ArcFlowUtils.getMessage(delegateExecution, "kpi.error.emptyKpiData"))
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
  }

  @Test
  void testGetKpiDataForInvalidHttpResponse() {
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"status\":\"success\"}")))

    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest"))
    expectedEx.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "request.error.schemaValidation"))
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
  }

  @Test
  void testGetKpiDataForFailedHttpRequest() {
    stubFor(post(urlPathEqualTo(GET_KPI_PATH))
        .willReturn(aResponse()
            .withStatus(500)
            .withHeader("Content-Type", "application/json")
            .withBody("Error")))
    expectedEx.expect(BpmnError.class)
    expectedEx.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "kpi.error.failedRetrieveRequest") + ": 500")
    runFlowScript(delegateExecution, "groovy/GetTriggeredKPI.groovy")
  }

}
