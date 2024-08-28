/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package groovy

import groovy.json.JsonSlurper
import java.util.stream.Collectors

import org.apache.http.HttpStatus
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Prepare gNBDUs"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
String apacheHttpClientAgentClassContent, arcFlowUtilsClassContent, jsonSchemaLocationsClassContent,
       flowAutomationErrorCodesClassContent, jsonSchemaExceptionClassContent, jsonSchemaValidatorClassContent
try {
  apacheHttpClientAgentClassContent =
      getStringFromResourceFile(execution, "groovy/utils/ApacheHttpClientAgent.groovy")
  arcFlowUtilsClassContent =
      getStringFromResourceFile(execution, "groovy/utils/ArcFlowUtils.groovy")
  jsonSchemaLocationsClassContent =
      getStringFromResourceFile(execution, "groovy/constants/JsonSchemaLocations.groovy")
  flowAutomationErrorCodesClassContent =
      getStringFromResourceFile(execution, "groovy/constants/FlowAutomationErrorCodes.groovy")
  jsonSchemaExceptionClassContent =
      getStringFromResourceFile(execution, "groovy/exceptions/JsonSchemaException.groovy")
  jsonSchemaValidatorClassContent =
      getStringFromResourceFile(execution, "groovy/utils/JsonSchemaValidator.groovy")
} catch (BpmnError bpmnError) {
  String errorMessage = "Failed to retrieve class content"
  LOGGER.error(errorMessage, bpmnError)
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Retrieval error"])
  throw new BpmnError("error.get_string_from_resource_file.exception",
      "${bpmnError.getMessage()}", bpmnError)
} catch (IOException ioException) {
  String errorMessage = "Failed to retrieve class content"
  LOGGER.error(errorMessage, ioException)
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Retrieval error"])
  throw new BpmnError("error.get_string_from_resource_file.exception",
      "${ioException.getMessage()}", ioException)
}

//Defining the local class loader
GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())

// Loading the explicit classes with the locally created class loader
Class flowAutomationErrorCodes, jsonSchemaLocations, arcFlowUtils, apacheHttpClientAgent
try {
  flowAutomationErrorCodes = loader.parseClass(flowAutomationErrorCodesClassContent)
  loader.parseClass(jsonSchemaExceptionClassContent)
  loader.parseClass(jsonSchemaValidatorClassContent)
  jsonSchemaLocations = loader.parseClass(jsonSchemaLocationsClassContent)
  arcFlowUtils = loader.parseClass(arcFlowUtilsClassContent)
  apacheHttpClientAgent = loader.parseClass(apacheHttpClientAgentClassContent)
} catch (CompilationFailedException compilationFailedException) {
  String errorMessage = "Failed to load class"
  LOGGER.error(errorMessage, compilationFailedException)
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Loading error"])
  throw new BpmnError("error.class_loader.exception", "${compilationFailedException.getMessage()}",
      compilationFailedException)
}

Reporter.updateReportSummary(execution, arcFlowUtils.getMessage(execution, "selectGnbdus.title"))

final String TOPOLOGY_HOST = "arcOptimizer.host"
final String GNBDU_LIST_PATH = "gnbduList.path"

String commonTopologyServiceUrl = System.getProperty("commonTopologyServiceUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, TOPOLOGY_HOST, GNBDU_LIST_PATH))
List<?> gnbListCts = []

Object apacheHttpClient =
    apacheHttpClientAgent.initialize().url(commonTopologyServiceUrl)
try {
  apacheHttpClient.processGetRequest()
} catch (BpmnError connectionException) {
  String errorMessage = arcFlowUtils.getMessage(execution, "cts.error.connectionFailed")
  LOGGER.error(errorMessage, connectionException)
  EventRecorder.error(execution, errorMessage, FLOW_STEP)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage, connectionException)
}
int ctsResponseCode = apacheHttpClient.getResponseCode()
if (ctsResponseCode == HttpStatus.SC_OK) {
  EventRecorder
      .info(execution,
          arcFlowUtils.getMessage(execution, "cts.status.connectionEstablished"),
          FLOW_STEP)
  try {
    String responseBody = apacheHttpClient.getResponseBody()
    arcFlowUtils.validateSchema(execution, responseBody, jsonSchemaLocations.GNBDU_LIST_GET)
    List<Object> parsedResponse = new JsonSlurper().parseText(responseBody) as List

    LOGGER.info "Reading response data from Topology Service ..\n"
    parsedResponse.each { it ->
      int id = it?.gNBId
      String name = it?.name
      String cmHandle = it?.cmHandle
      if ((id && name && cmHandle) && (id > 0) &&
              (arcFlowUtils.isUniqueGnbdu(id, name, cmHandle, gnbListCts))) {
        gnbListCts.add("gNBName": name, "gNBId": id, "gNBCmHandle": cmHandle)
      }
    }

    if (gnbListCts.size() < 2) {
      String errorMessage =
          arcFlowUtils.getMessage(execution, "selectGnbdus.error.gnbListSizeLessThan2")
      EventRecorder.error(execution, errorMessage, FLOW_STEP)
      Reporter.updateReportSummary(execution, errorMessage)
      throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage)
    }
  } catch (IllegalArgumentException ex) {
    String errorMessage =
        arcFlowUtils.getMessage(execution, "request.error.responseParsing")
    LOGGER.error("$errorMessage: ${ex.getMessage()}", ex)
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage, ex)
  } catch (BpmnError e) {
    String errorMessage =
        arcFlowUtils.getMessage(execution, "request.error.responseParsing")
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    throw e
  }
} else {
  String errorMessage = arcFlowUtils.getMessage(execution, "cts.error.connectionFailed")
  LOGGER.error(errorMessage + ": " + ctsResponseCode)
  EventRecorder.error(execution, errorMessage, FLOW_STEP)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, "$errorMessage: $ctsResponseCode")
}

execution.setVariable("gNBList", gnbListCts)
execution.setVariable("selectedGnbs", gnbListCts)

// Method for retrieving the contents of the explicit classes
String getStringFromResourceFile(DelegateExecution execution,
    String resourceFilePath) throws IOException, BpmnError {
  String classContent
  final String ERROR_MESSAGE = "Null encountered while reading file"
  if (execution.getClass().getSimpleName().contains("DelegateExecutionFake")) {
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceFilePath)
    if (inputStream != null) {
      classContent =
          inputStream
              .withCloseable {
                new InputStreamReader(it)
                    .withCloseable {
                      new BufferedReader(it).lines()
                          .collect(Collectors.joining('\n'))
                    }
              }
    } else {
      throw new BpmnError(ERROR_MESSAGE)
    }
  } else {
    classContent = FlowPackageResources.get(execution, resourceFilePath)
    if (classContent == null) {
      throw new BpmnError(ERROR_MESSAGE)
    }
  }
  return classContent
}

