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

final String FLOW_STEP = "Recurrence"
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

Integer afterOccurrences = execution.getVariable("afterOccurrences") as Integer
boolean endDateOccurred = execution.getVariable("endDateOccurred")
boolean hasRecurrence = execution.getVariable("hasRecurrence")
Instant nextOptimizationDate =
    Instant.parse(execution.getVariable("nextOptimizationDate") as String)

if (hasRecurrence) {
  String recurrenceValue = execution.getVariable("recurrence")
  if (afterOccurrences) {
    afterOccurrences = Integer.valueOf(afterOccurrences) - 1
    execution.setVariable("afterOccurrences", afterOccurrences)
  } else {
    String endDate = execution.getVariable('endDate')
    Instant endDateTime = Instant.parse(endDate)
    endDateOccurred = (Instant.now() + Duration.parse(recurrenceValue)).isAfter(endDateTime)
    execution.setVariable("endDateOccurred", endDateOccurred)
  }
  while ((nextOptimizationDate + Duration.parse(recurrenceValue)).isBefore(Instant.now())) {
    nextOptimizationDate = nextOptimizationDate + Duration.parse(recurrenceValue)
  }

  int occurrenceNumber = execution.getVariable("occurrenceNumber") + 1
  execution.setVariable("occurrenceNumber", occurrenceNumber)
  execution.setVariable("occurrenceInfo",
      arcFlowUtils.getMessage(execution, "optimization.occurrenceNumber") + ": " +
          String.valueOf(occurrenceNumber) + "; ")
}
boolean recurrenceContinue = !(afterOccurrences == 0 || endDateOccurred || !hasRecurrence)
execution.setVariable("nextOptimizationDate", nextOptimizationDate.toString())
execution.setVariable("recurrenceContinue", recurrenceContinue)

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