// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

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

final String FLOW_STEP = "Trigger NCMP UE measurement"
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

final String NCMP_HOST = "ncmp.host"
final String COVERAGE_DATA_MEASUREMENT_PATH = "coverageDataMeasurement.path"

List<?> selectedGNodeB = (flowInput?.selectGNBDUs as Map)?.table as List
List<?> mandatoryLinks = (flowInput?.mandatoryGNodeBPairsSelection as Map)?.readOnlyTable as List
List<?> unwantedLinks = (flowInput?.unwantedGNodeBPairsSelection as Map)?.readOnlyTable as List
String ncmpUrl = System.getProperty("ncmpUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, NCMP_HOST, COVERAGE_DATA_MEASUREMENT_PATH))
String gnbIdList = arcFlowUtils.buildJsonWithKey(selectedGNodeB)
List<String> enabledGnbList = []
List<String> notFoundGnbList = []
execution.setVariable("gnbIdList", enabledGnbList)
execution.setVariable("gnbNotFoundList", notFoundGnbList)

Object apacheHttpClient = apacheHttpClientAgent.initialize().url(ncmpUrl)
try {
  /*apacheHttpClient.processPutRequest(gnbIdList)

  if (apacheHttpClient.getResponseCode() == HttpStatus.SC_OK) {
    EventRecorder
        .info(execution,
            arcFlowUtils.getMessage(execution, "ncmp.status.connectionEstablished"),
            FLOW_STEP)

    String responseBody = apacheHttpClient.getResponseBody()
    arcFlowUtils.validateSchema(execution, responseBody, jsonSchemaLocations.COVERAGE_MEASUREMENT_PUT)

    Map<String, Object> parsedResponse = new JsonSlurper().parseText(responseBody) as Map

    enabledGnbList = Arrays.asList(parsedResponse.ListofEnabledgNB as String[])
    notFoundGnbList = Arrays.asList(parsedResponse?.ListofNotFoundGNB as String[])
  } else {
    String errorMessage = arcFlowUtils.getMessage(execution, "ncmp.error.connectionFailed")
    EventRecorder
        .error(execution, errorMessage, FLOW_STEP, ["Reason": apacheHttpClient.getResponseCode()])
    LOGGER.error(errorMessage)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
        "${arcFlowUtils.getMessage(execution, "request.error.responseCode")}: ${apacheHttpClient.responseCode}")
  }
  execution.setVariable("gnbIdList", enabledGnbList)
  execution.setVariable("gnbNotFoundList", notFoundGnbList)*/
  List<?> nodesToOptimize = selectedGNodeB.stream().filter(
      { Map node -> nodeHasUEMeasurements(node.gNBId as Integer, enabledGnbList) })
      .collect(Collectors.toList()) as List
  List<?> mandatoryNodePairs = mandatoryLinks.stream().filter(
      { Map nodesLink -> nodeHasUEMeasurements(nodesLink.pGnbduId as Integer, enabledGnbList) && nodeHasUEMeasurements(nodesLink.sGnbduId as Integer, enabledGnbList) })
      .collect(Collectors.toList()) as List
  List<?> unwantedNodePairs = unwantedLinks.stream().filter(
      { Map nodesLink -> nodeHasUEMeasurements(nodesLink.pGnbduId as Integer, enabledGnbList) && nodeHasUEMeasurements(nodesLink.sGnbduId as Integer, enabledGnbList) })
      .collect(Collectors.toList()) as List
  execution.setVariable("gnbListToOptimize", nodesToOptimize)
  execution.setVariable("mandatoryNodePairs", mandatoryNodePairs)
  execution.setVariable("unwantedNodePairs", unwantedNodePairs)
} catch (UnknownHostException unknownHostException) {
  String errorMessage = arcFlowUtils.getMessage(execution, "ncmp.error.connectionFailed")
  String exceptionMessage = arcFlowUtils.getMessage(execution, "ncmp.error.unknownHost")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": exceptionMessage])
  LOGGER.error(errorMessage, unknownHostException)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
      "$errorMessage: ${unknownHostException.getMessage()}", unknownHostException)
} catch (BpmnError bpmnError) {
  String errorMessage = arcFlowUtils.getMessage(execution, "ncmp.error.connectionFailed")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": bpmnError.getMessage()])
  LOGGER.error(errorMessage, bpmnError)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, "$errorMessage: ${bpmnError.getMessage()}",
      bpmnError)
} catch (Exception otherException) {
  String errorMessage = arcFlowUtils.getMessage(execution, "ncmp.error.connectionFailed")
  EventRecorder.error(execution, errorMessage, FLOW_STEP)
  LOGGER.error(errorMessage, otherException)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
      "$errorMessage: ${otherException.getMessage()}", otherException)
}

static boolean nodeHasUEMeasurements(Integer gNBId, List<String> enabledGnbList) {
  return true
//  return enabledGnbList.contains(gNBId.toString())
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
