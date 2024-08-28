// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Start Optimization"
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
final String occurrenceInfo = execution.getVariable("occurrenceInfo")
final String ARC_OPTIMIZER_HOST = "arcOptimizer.host"
final String ARC_OPTIMIZER_PATH = "arcOptimizer.path"
final String STATUS_REPORT_VARIABLE = "statusOfOptimization"
final String START_OPTIMIZATION_PATH = "/start"
String optimizationId = execution.getVariable("optimizationId")
Reporter.updateReportSummary(execution,
    occurrenceInfo + arcFlowUtils.getMessage(execution, "startOptimization.title"))
String optimizationUrl = System.getProperty("optimizationUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, ARC_OPTIMIZER_HOST, ARC_OPTIMIZER_PATH))

// Create optimization start request
Object apacheHttpClient = apacheHttpClientAgent
    .initialize()
    .url(optimizationUrl + optimizationId + START_OPTIMIZATION_PATH)
try {
  apacheHttpClient.processPostRequest("{}")
  int responseCode = apacheHttpClient.getResponseCode()
  if (responseCode == HttpURLConnection.HTTP_OK) {
    String responseBody = apacheHttpClient.getResponseBody()
    arcFlowUtils.validateSchema(execution, responseBody,
        jsonSchemaLocations.START_OPTIMIZATION_POST)
    Reporter.updateReportSummary(execution,
        occurrenceInfo + arcFlowUtils.getMessage(execution, "optimization.title"))
    Reporter.updateReportVariable(execution, STATUS_REPORT_VARIABLE,
        occurrenceInfo + arcFlowUtils.getMessage(execution, "status.inProgress"))
    String statusMessage =
        occurrenceInfo +
            arcFlowUtils.getMessage(execution, "startOptimization.status.successfulStart")
    LOGGER.debug(statusMessage)
    EventRecorder.info(execution, statusMessage, FLOW_STEP)
  } else {
    String errorMessage =
        occurrenceInfo +
            arcFlowUtils.getMessage(execution, "startOptimization.error.optimizationNotStarted")
    Reporter.updateReportSummary(execution, errorMessage)
    Reporter.updateReportVariable(execution, STATUS_REPORT_VARIABLE, errorMessage)
    LOGGER.error("$errorMessage: $responseCode")
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "$responseCode"])
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
        "$errorMessage: $responseCode, ${apacheHttpClient.getResponseBody()}")
  }
} catch (BpmnError bpmnError) {
  String errorMessage =
      occurrenceInfo +
          arcFlowUtils.getMessage(execution, "startOptimization.error.optimizationNotStarted")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "${bpmnError.getMessage()}"])
  throw bpmnError
}

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

