// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.json.JsonSlurper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

final String FLOW_STEP = "Get triggered KPI data"
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
final String KPI_DATA_PATH = "kpiData.path"
String kpiGetRequestBody =
        arcFlowUtils.buildGetKpisRequestBody(execution.getVariable("gnbListToOptimize") as List<Map<String, Object>>)
ArrayList kpisData = execution.getVariable("kpisData") as ArrayList ?: []
String kpisUrl = System.getProperty("kpisUrl",
    arcFlowUtils.getUrlFromPropertiesFile(execution, ARC_OPTIMIZER_HOST, KPI_DATA_PATH))

Object apacheHttpClient = apacheHttpClientAgent
    .initialize()
    .url(kpisUrl)
try {
  apacheHttpClient.processPostRequest(kpiGetRequestBody)
  if (apacheHttpClient.getResponseCode() == HttpStatus.SC_OK) {
    String responseBody = apacheHttpClient.getResponseBody()
    if (!responseBody) {
      throw new IllegalArgumentException(
          arcFlowUtils.getMessage(execution, "kpi.error.emptyKpiData"))
    }

    arcFlowUtils.validateSchema(execution, responseBody, jsonSchemaLocations.KPI_DATA_POST)
    Map parsedResponse = new JsonSlurper().parseText(responseBody) as Map
    Map kpisDataMap = parsedResponse.KpiData as Map
    String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())

    String index2 = String.format("%s:%s:%s:%s", now, getKpiData(kpisDataMap?.KPI1 as int),
        getKpiData(kpisDataMap?.KPI2 as int), getKpiData(kpisDataMap?.KPI3 as int))
    kpisData = kpisData + [index2]
    Reporter.updateReportVariable(execution, index2 + ".timeStamp", now)
    Reporter.updateReportVariable(execution, index2 + ".kpi1History", getKpiData(kpisDataMap?.KPI1 as int))
    Reporter.updateReportVariable(execution, index2 + ".kpi2History", getKpiData(kpisDataMap?.KPI2 as int))
    Reporter.updateReportVariable(execution, index2 + ".kpi3History", getKpiData(kpisDataMap?.KPI3 as int))
    Reporter.updateReportVariable(execution, "kpisHistoryValuesList", kpisData.join(','))
    execution.setVariable("kpisData",kpisData)

    ArrayList kpis = []
    String index = String.format("%s:%s:%s", getKpiData(kpisDataMap?.KPI1 as int),
        getKpiData(kpisDataMap?.KPI2 as int), getKpiData(kpisDataMap?.KPI3 as int))
    kpis = kpis + [index]
    Reporter.updateReportVariable(execution, index + ".kpi1", getKpiData(kpisDataMap?.KPI1 as int))
    Reporter.updateReportVariable(execution, index + ".kpi2", getKpiData(kpisDataMap?.KPI2 as int))
    Reporter.updateReportVariable(execution, index + ".kpi3", getKpiData(kpisDataMap?.KPI3 as int))
    Reporter.updateReportVariable(execution, "kpisValuesList", kpis.join(','))

  } else {
    String errorMessage =  arcFlowUtils.getMessage(execution, "kpi.error.failedRetrieveRequest")
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    LOGGER.error(errorMessage)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
        "${arcFlowUtils.getMessage(execution, "kpi.error.failedRetrieveRequest")}: ${apacheHttpClient.getResponseCode()}")
  }
}
 catch (IllegalArgumentException illegalArgumentException) {
  String errorMessage = arcFlowUtils.getMessage(execution, "kpi.error.failedRetrieveRequest")
  String errorCause = arcFlowUtils.getMessage(execution, "kpi.error.emptyKpiData")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, [reason: errorCause])
  LOGGER.error(errorMessage, illegalArgumentException)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
      errorMessage + ": " + illegalArgumentException.getMessage(), illegalArgumentException)
} catch (BpmnError bpmnError) {
  String errorMessage = arcFlowUtils.getMessage(execution, "kpi.error.failedRetrieveRequest")
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": bpmnError.getMessage()])
  LOGGER.error(errorMessage, bpmnError)
  throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage + ": " + bpmnError.getMessage(),
      bpmnError)
}

private static String getKpiData(Integer kpi) {
  return kpi.toString()
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