// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Scheduler"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
String arcFlowUtilsClassContent, jsonSchemaExceptionClassContent, jsonSchemaValidatorClassContent
try {
  arcFlowUtilsClassContent =
      getStringFromResourceFile(execution, "groovy/utils/ArcFlowUtils.groovy")
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
Class arcFlowUtils
try {
  loader.parseClass(jsonSchemaExceptionClassContent)
  loader.parseClass(jsonSchemaValidatorClassContent)
  arcFlowUtils = loader.parseClass(arcFlowUtilsClassContent)
} catch (CompilationFailedException compilationFailedException) {
  String errorMessage = "Failed to load class"
  LOGGER.error(errorMessage, compilationFailedException)
  EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Loading error"])
  throw new BpmnError("error.class_loader.exception", "${compilationFailedException.getMessage()}",
      compilationFailedException)
}

Reporter.updateReportSummary(execution, arcFlowUtils.getMessage(execution,"scheduler.title"))

Map<String, Object> scheduleSettings = (flowInput as Map).selectScheduleUserTask as Map

boolean endDateOccurred = false
Instant nextOptimizationDate = Instant.now()
Instant startDate = (flowInput as Map).startTime
String recurrenceValue = (flowInput as Map).recurrenceValue
Integer afterOccurrences  = (flowInput as Map).occurrences as Integer
Instant endDate = (flowInput as Map).endDate as Instant
boolean showBaselineKPI = (flowInput as Map).showBaselineKPI
String kpiObservationTime = (flowInput as Map).kpiObservationTime as String
Map<String, Object> recurrenceSettings = (scheduleSettings.recurrence as Map).withRecurrence as Map
boolean hasRecurrence = recurrenceSettings != null

if (hasRecurrence) {
  if (endDate != null)
  {
      endDateOccurred = Instant.now().isAfter(endDate)
  }
  nextOptimizationDate = startDate.plus(Duration.parse(recurrenceValue))
  if (showBaselineKPI) {
    nextOptimizationDate = nextOptimizationDate.plus(Duration.parse(kpiObservationTime))
  }
}

if ((((scheduleSettings.recurrence as Map)?.noRecurrence as Map)?.applyResultsAutomatically as Map)
    ?.no) {
    execution.setVariable("showConfiguration", "no")
} else {
    execution.setVariable("showConfiguration", "yes")
}

execution.setVariable("nextOptimizationDate", nextOptimizationDate.toString())
execution.setVariable("startDate", startDate.toString())
execution.setVariable("recurrence", recurrenceValue)
execution.setVariable("afterOccurrences", afterOccurrences)
execution.setVariable("hasRecurrence", hasRecurrence)
execution.setVariable("endDate", endDate)
execution.setVariable("endDateOccurred", endDateOccurred)
execution.setVariable("occurrenceNumber", 1)
execution.setVariable("occurrenceInfo",
    arcFlowUtils.getMessage(execution, "optimization.occurrenceNumber") + ": " + String.valueOf(1) +
        "; ")
execution.setVariable("showBaselineKPI", showBaselineKPI)
execution.setVariable("observationTime", kpiObservationTime)

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