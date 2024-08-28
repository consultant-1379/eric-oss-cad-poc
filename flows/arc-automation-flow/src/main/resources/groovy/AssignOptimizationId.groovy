// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Assigning Optimization ID"
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

final String ARC_OPTIMIZER_HOST = "arcOptimizer.host"
final String ARC_OPTIMIZER_PATH = "arcOptimizer.path"
String createOptimizationUrl = System.getProperty("optimizationUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, ARC_OPTIMIZER_HOST, ARC_OPTIMIZER_PATH))
List<?> nodesToOptimize = execution.getVariable("gnbListToOptimize") as List
List<?> mandatoryNodePairs = execution.getVariable("mandatoryNodePairs") as List
List<?> unwantedNodePairs = execution.getVariable("unwantedNodePairs") as List
String createOptimizationRequestBody =
        buildCreateOptimizationRequestBody(nodesToOptimize as List<Map<String, Object>>, unwantedNodePairs, mandatoryNodePairs)
Object apacheHttpClient = apacheHttpClientAgent.initialize().url(createOptimizationUrl)
try {
  apacheHttpClient.processPostRequest(createOptimizationRequestBody)
} catch (BpmnError bpmnError) {
  String errorMessage = arcFlowUtils.getMessage(execution, "optimization.id.request.failure")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "${bpmnError.getMessage()}"])
  throw bpmnError
}
  int responseCode = apacheHttpClient.getResponseCode()
  if (responseCode == HttpURLConnection.HTTP_OK) {
    Reporter.updateReportSummary(execution, arcFlowUtils.getMessage(execution, "optimization.id.title"))
    String responseBody = apacheHttpClient.getResponseBody()
    try {
      arcFlowUtils.validateSchema(execution, responseBody, jsonSchemaLocations.ASSIGN_OPTIMIZATION_ID_POST)
    } catch (BpmnError bpmnErr) {
      String errorMessage = arcFlowUtils.getMessage(execution, "optimization.schema.validation.failure")
      EventRecorder.error(execution, errorMessage, FLOW_STEP)
      throw bpmnErr
    }
    Map<String, Object> response = new JsonSlurper().parseText(responseBody) as Map
    if (response.status == "Success") {
      String optimizationId = response.optimizationId as String
      execution.setVariable("optimizationId", optimizationId)
      String statusMessage = arcFlowUtils.getMessage(execution, "optimization.id.successful")
      LOGGER.debug(statusMessage)
      EventRecorder.info(execution, statusMessage, FLOW_STEP)
    }
  } else {
    String errorMessage = arcFlowUtils.getMessage(execution, "optimization.id.failure")
    Reporter.updateReportSummary(execution, errorMessage)
    LOGGER.error("$errorMessage: $responseCode")
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "$responseCode"])
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
        "$errorMessage: $responseCode")
  }

static String buildCreateOptimizationRequestBody(List<Map<String, Object>> gnbs, List<Map<String, Object>> unwantedNodePairsList, List<Map<String, Object>> mandatoryNodePairsList) {
  JsonBuilder json = new JsonBuilder()
  json {
    selectedNodes {
      gnbIdList(gnbs.collect { ["gnbId": it.gNBId, "cmHandle": it.gNBCmHandle] })
    }
    unwantedNodePairs {
      gnbPairsList(unwantedNodePairsList.collect { ["pGnbduId": it.pGnbduId, "sGnbduId": it.sGnbduId] })
    }
    mandatoryNodePairs {
      gnbPairsList(mandatoryNodePairsList.collect { ["pGnbduId": it.pGnbduId, "sGnbduId": it.sGnbduId] })
    }
  }
  return json.toString()
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








