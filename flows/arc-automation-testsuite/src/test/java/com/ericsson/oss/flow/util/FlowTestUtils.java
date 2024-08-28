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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class FlowTestUtils {
  private static final String MESSAGES_FILE_PATH;
  private static final Properties messagesProperties;

  static {
    MESSAGES_FILE_PATH = "resources/messages.properties";
    try (InputStream messageStream = FlowTestUtils.class.getClassLoader().getResourceAsStream(
        MESSAGES_FILE_PATH);
         BufferedReader messageReader = new BufferedReader(new InputStreamReader(
             Objects.requireNonNull(messageStream)))
    ) {
      String messagesString = messageReader.lines().collect(
          Collectors.joining(System.lineSeparator()));
      messagesProperties = new Properties();
      messagesProperties.load(new StringReader(messagesString));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String getMessage(String messageCode) throws UncheckedIOException {
    return messagesProperties.getProperty(messageCode);
  }

  /**
   * Reads a file located at the filePath relative to the com.ericsson.oss.flow.util package.
   *
   * @param filePath Path of the file relative to the util package.
   * @return The text contained in the file.
   * @throws IOException If an error happens while reading the file.
   */
  public static String readFile(Path filePath) throws IOException {
    try (InputStream inputStream = FlowTestUtils.class.getResourceAsStream(filePath.toString());
         BufferedReader br =
             new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))
    ) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }

  /**
   * Sets the routes for testing depending on the type of testing.
   * If isOnline is true then the routes are set to localhost,
   * otherwise the routes are set to use wireMockRule's port.
   *
   * @param isOnline If true the routes are set to localhost endpoints. Otherwise, routes are set to wiremock port.
   * @param wireMockRule The wiremock rule for the test class.
   */
  public static void setRoutes(boolean isOnline, WireMockRule wireMockRule) {
    if (isOnline) {
      System.setProperty("commonTopologyServiceUrl", "http://localhost:8001/ctw/gnbdu/all");
      System.setProperty("ncmpUrl", "http://localhost:8000/v1/CM/CoverageDataMeasurementgNB/");
      System.setProperty("kpisUrl", "http://localhost:5000/kpis/");
      System.setProperty("optimizationUrl", "http://localhost:5000/optimizations/");
      System.setProperty("optimizationConfigurationUrl", "http://localhost:5000/configurations/");
    } else {
      System.setProperty("commonTopologyServiceUrl",
          "http://localhost:" + wireMockRule.port() + "/ctw/gnbdu/all");
      System.setProperty("ncmpUrl",
          "http://localhost:" + wireMockRule.port() + "/v1/CM/CoverageDataMeasurementgNB/");
      System.setProperty("kpisUrl", "http://localhost:" + wireMockRule.port() + "/kpis/");
      System.setProperty("optimizationUrl",
          "http://localhost:" + wireMockRule.port() + "/optimizations/");
      System.setProperty("optimizationConfigurationUrl",
          "http://localhost:" + wireMockRule.port() + "/configurations/");
    }
  }
}
