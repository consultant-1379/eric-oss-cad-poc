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

final String FLOW_STEP = "Optimization"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)
OPTIMIZATION_STATUS_REPORTS = "CAD_Optimization_Report.csv"
OPTIMIZATION_STATUS_COLUMNS = 'pGnbId,sGnbId,usability'
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
final String optimizationId = execution.getVariable("optimizationId")
final String ARC_OPTIMIZER_HOST = "arcOptimizer.host"
final String ARC_OPTIMIZER_PATH = "arcOptimizer.path"
final String OPTIMIZATION_REQUEST_PATH = "/status"
String optimizationUrl = System.getProperty("optimizationUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, ARC_OPTIMIZER_HOST, ARC_OPTIMIZER_PATH))
String optimizationWithAssignedIdUrl = optimizationUrl + optimizationId + OPTIMIZATION_REQUEST_PATH
Object apacheHttpClient = apacheHttpClientAgent.initialize().url(optimizationWithAssignedIdUrl)
try {
  apacheHttpClient.processGetRequest()
} catch (BpmnError e) {
  String errorMessage =
      occurrenceInfo + arcFlowUtils.getMessage(execution, "optimization.error.connectionFailed")
  LOGGER.error(errorMessage, e)
  EventRecorder.error(execution, errorMessage, FLOW_STEP)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage, e)
}

int responseCode = apacheHttpClient.getResponseCode()
String responseBody = apacheHttpClient.getResponseBody()
try {
  arcFlowUtils.validateSchema(execution, responseBody, jsonSchemaLocations.OPTIMIZATION_DATA_GET)
} catch (BpmnError e) {
  String errorMessage =
      occurrenceInfo + arcFlowUtils.getMessage(execution, "optimization.error.connectionFailed")
  EventRecorder.error(execution, errorMessage, FLOW_STEP)
  throw e
}
Map<String, Object> response = new JsonSlurper().parseText(responseBody) as Map
if (responseCode == HttpStatus.SC_OK) {
  String statusMessage =
      occurrenceInfo +
          arcFlowUtils.getMessage(execution, "optimization.status.connectionEstablished")
  LOGGER.debug("$statusMessage: ${response.status}.")
  EventRecorder.info(execution, statusMessage, FLOW_STEP)
  processOptimizationResult(FLOW_STEP, response, occurrenceInfo)
} else {
  String errorMessage =
      occurrenceInfo + arcFlowUtils.getMessage(execution, "optimization.error.connectionFailed")
  LOGGER.error("$errorMessage ($responseCode): ${response.status}.")
  EventRecorder
      .error(execution, errorMessage, FLOW_STEP, ["Reason": "HTTP code was ${responseCode}"])
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
      "$errorMessage ($responseCode)")
}

private List processOptimizationResult(String target, Map<String, Object> response,
    String occurrenceInfo) {
  List optimizationResult = []
  if (response.status == "Optimization finished") {
    createBBPairsListWithPairsId(response.result as List, optimizationResult)
    execution.setVariable("optimizationResultTableIsEmpty",optimizationResult.isEmpty())
    execution.setVariable("status", "finished")
    execution.setVariable("optimizationResultTable", optimizationResult)
    List<?> lstgnb = (flowInput?.reportGNBDUs as List)
    reportResult = parseStatusListInCsvFormat(lstgnb)
    Reporter.updateReportVariable(execution, OPTIMIZATION_STATUS_REPORTS, reportResult)
    if (optimizationResult.isEmpty()) {
      EventRecorder.warn(execution, "No target optimization partners found. Moving onto next available optimization.", target)
    }
    EventRecorder.info(execution, occurrenceInfo + response.status as String, target,
        ["Optimal BB pairs": optimizationResult])
    Reporter.updateReportVariable(execution, "statusOfOptimization", response.status)
    Reporter.updateReportSummary(execution, "Optimization finished")
  } else {
    Reporter.updateReportVariable(execution, "statusOfOptimization", response.status)
    execution.setVariable("status", "ongoing")
  }
  return optimizationResult
}

private String parseStatusListInCsvFormat(final List<String> result) {
  StringBuilder reportStrBuilder = new StringBuilder(OPTIMIZATION_STATUS_COLUMNS)
  reportStrBuilder.append("\n")
  List<?> unwantedLinks = (flowInput?.unwantedGNBDUs as List)
  for (unwantedlink in unwantedLinks){
    def index1 = result.findIndexOf{unwantedLinks.SecondarygNBId}
    result.remove(index1)
  }
  result.each { value ->
    reportStrBuilder.append(value.gNBId)
    reportStrBuilder.append(",")
    reportStrBuilder.append(value.SecondarygNBId)
    reportStrBuilder.append(",")
    reportStrBuilder.append(0.1)
    reportStrBuilder.append("\n")
  }

  return reportStrBuilder.toString()
}

static private createBBPairsListWithPairsId(List<Map<String, Object>> result,
    List optimizationResult) {
  int count = 0
  result.each { value ->
    count++
    optimizationResult.add(
        "Id": count,
        "pGnbId": value.pGnbId,
        "sGnbId": value.sGnbId,
        "usability": value.usability == 0.0 ? "Nan" : value.usability.toString())
  }
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