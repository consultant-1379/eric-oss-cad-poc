/*
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

package com.ericsson.oss.flow.util;

import com.ericsson.oss.services.flowautomation.test.fwk.rest.WireMockServerSimulator;
import java.io.IOException;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;

public class  ServiceMockUtils {

  private static final String ctsGnbdus;

  static {
    try {
      ctsGnbdus = FlowTestUtils.readFile(Paths.get("responses", "get-cts-gnbdus.json"));
    } catch (IOException e) {
      throw new RuntimeException("CTS mocked response reading failure", e);
    }
  }

  public void mockUeMeasurements(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        put(urlPathMatching("/v1/CM/CoverageDataMeasurementgNB/"))
            .withRequestBody(equalToJson("{\n"
                + "    \"gnbIdList\": [\n"
                + "        {\"gnbId\": 270, \"cmHandle\": \"DC7B963A177405864810A2F5E640762D\"}, \n"
                + "        {\"gnbId\": 279, \"cmHandle\": \"18A620FDE38287A0B4095A9A28AD5045\"}\n"
                + "    ]\n"
                + "}", NON_EXTENSIBLE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"ListofEnabledgNB\": [\n"
                    + "        270,\n"
                    + "        279\n"
                    + "    ],\n"
                    + "    \"ListofNotFoundGNB\": []\n"
                    + "}")
            )
    );
  }

  public void mockUeMeasurementsWithUnavailableGnbdu(
      WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        put(urlPathMatching("/v1/CM/CoverageDataMeasurementgNB/"))
            .withRequestBody(equalToJson("{\n"
                + "    \"gnbIdList\": [\n"
                + "        {\"gnbId\": 270, \"cmHandle\": \"DC7B963A177405864810A2F5E640762D\"}, \n"
                + "        {\"gnbId\": 279, \"cmHandle\": \"18A620FDE38287A0B4095A9A28AD5045\"},\n"
                + "        {\"gnbId\": 1503, \"cmHandle\": \"64D53146816ABFD5768AEFF2996BE074\"}\n"
                + "    ]\n"
                + "}", NON_EXTENSIBLE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"ListofEnabledgNB\": [\n"
                    + "        270,\n"
                    + "        279\n"
                    + "    ],\n"
                    + "    \"ListofNotFoundGNB\": [\n"
                    + "        1503\n"
                    + "    ]\n"
                    + "}")
            )
    );
  }

  public void mockTriggerKpi(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/kpis/"))
             .withRequestBody(equalToJson("{\n"
                        + "    \"gnbList\": [\n"
                        + "            {\"gnbId\": 270, \"cmHandle\": \"DC7B963A177405864810A2F5E640762D\"},\n"
                        + "            {\"gnbId\": 279, \"cmHandle\": \"18A620FDE38287A0B4095A9A28AD5045\"}\n"
                        + "        ]\n"
                        + "}"))
            .inScenario("kpiLoop")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Success\",\n"
                    + "    \"result\": \"get kpi data.\",\n"
                    + "    \"KpiData\": {\n"
                    + "        \"KPI1\": 4,\n"
                    + "        \"KPI2\": 2,\n"
                    + "        \"KPI3\": 4\n"
                    + "    }\n"
                    + "}")
            )
            .willSetStateTo("state1")
    );
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/kpis/"))
            .withRequestBody(equalToJson("{\n"
                        + "    \"gnbList\": [\n"
                        + "            {\"gnbId\": 270, \"cmHandle\": \"DC7B963A177405864810A2F5E640762D\"},\n"
                        + "            {\"gnbId\": 279, \"cmHandle\": \"18A620FDE38287A0B4095A9A28AD5045\"}\n"
                        + "        ]\n"
                        + "}", LENIENT))
            .inScenario("kpiLoop")
            .whenScenarioStateIs("state1")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                    .withBody("{\n"
                            + "    \"status\": \"Success\",\n"
                            + "    \"result\": \"get kpi data.\",\n"
                            + "    \"KpiData\": {\n"
                            + "        \"KPI1\": 5,\n"
                            + "        \"KPI2\": 3,\n"
                            + "        \"KPI3\": 6\n"
                            + "    }\n"
                            + "}")
            )
            .willSetStateTo("state2")
    );
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/kpis/"))
            .inScenario("kpiLoop")
            .whenScenarioStateIs("state2")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Success\",\n"
                    + "    \"result\": \"get kpi data.\",\n"
                    + "    \"KpiData\": {\n"
                    + "        \"KPI1\": 3,\n"
                    + "        \"KPI2\": 2,\n"
                    + "        \"KPI3\": 4\n"
                    + "    }\n"
                    + "}")
            )
            .willSetStateTo(STARTED)
    );
  }

  public void mockOptimizationCreate(WireMockServerSimulator wireMockServerSimulator) {
      wireMockServerSimulator.stubFor(
              post(urlPathMatching("/optimizations/"))
              .withRequestBody(equalToJson("{\n"
                      + "    \"selectedNodes\": {\n"
                      + "        \"gnbIdList\": [\n"
                      + "            {\"gnbId\": 270, \"cmHandle\": \"DC7B963A177405864810A2F5E640762D\"},\n"
                      + "            {\"gnbId\": 279, \"cmHandle\": \"18A620FDE38287A0B4095A9A28AD5045\"}\n"
                      + "        ]\n"
                      + "    },\n"
                      + "    \"unwantedNodePairs\": {\n"
                      + "        \"gnbPairsList\": []\n"
                      + "    },\n"
                      + "    \"mandatoryNodePairs\": {\n"
                      + "        \"gnbPairsList\": []\n"
                      + "    }\n"
                      + "}", LENIENT))
              .willReturn(aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{\"status\": \"Success\", \"optimizationId\": \"434234\", \"result\": \"Optimization instance created.\"}")));
  }

  public void mockOptimizationStart(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/optimizations/434234/start"))
            .withRequestBody(equalToJson("{}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Success\",\n"
                    + "    \"result\": \"Optimization started.\"\n"
                    + "}")
            )
    );
  }

  public void mockOptimizationStatus(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        get(urlPathMatching("/optimizations/434234/status"))
            .inScenario("optimizing")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Optimization in progress\",\n"
                    + "    \"result\": []\n"
                    + "}")
            )
            .willSetStateTo("inProgress")
    );
    wireMockServerSimulator.stubFor(
        get(urlPathMatching("/optimizations/434234/status"))
            .inScenario("optimizing")
            .whenScenarioStateIs("inProgress")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Optimization finished\",\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"pGnbId\": 279,\n"
                    + "            \"sGnbId\": 270,\n"
                    + "            \"usability\": 0.3809468211368755\n"
                    + "        },\n"
                    + "        {\n"
                    + "            \"pGnbId\": 270,\n"
                    + "            \"sGnbId\": 279,\n"
                    + "            \"usability\": 0.12698227371229184\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")
            )
            .willSetStateTo(STARTED)
    );
  }

  public void mockOptimizationFinish(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/optimizations/434234/stop"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"status\": \"Success\",\n"
                    + "    \"result\": \"Optimization stopped successfully.\"\n"
                    + "}")
            )
    );
  }

  public void mockPartnerConfiguration(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        post(urlPathMatching("/configurations/"))
            .withRequestBody(equalToJson("{\n"
                + "    \"gnbIdList\": [\n"
                + "        {\n"
                + "            \"pGnbId\": 279,\n"
                + "            \"sGnbId\": 270,\n"
                + "        },\n"
                + "        {\n"
                + "            \"pGnbId\": 270,\n"
                + "            \"sGnbId\": 279,\n"
                + "        }\n"
                + "    ]\n"
                + "}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n"
                    + "    \"enabledGnb\": {\n"
                    + "        \"gnbIdList\": [\n"
                    + "            {\n"
                    + "                \"pGnbId\": 279,\n"
                    + "                \"sGnbId\": 270,\n"
                    + "            },\n"
                    + "            {\n"
                    + "                \"pGnbId\": 270,\n"
                    + "                \"sGnbId\": 279,\n"
                    + "            }\n"
                    + "        ]\n"
                    + "    }\n"
                    + "}")
            )
    );
  }

  public void mockCtsAvailableGnbdus(WireMockServerSimulator wireMockServerSimulator) {
    wireMockServerSimulator.stubFor(
        get(urlPathMatching("/ctw/gnbdu/all"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(ctsGnbdus)
            )
    );
  }
}
