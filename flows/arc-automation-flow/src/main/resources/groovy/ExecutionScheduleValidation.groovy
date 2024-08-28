// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.transform.Field
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.usertask.UsertaskInputProcessingError

@Field
final String FLOW_STEP = "Validate execution schedule"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

@Field
final String validationType = execution.getVariable("setupMode") as String

// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
@Field
String arcFlowUtilsClassContent
@Field
String flowAutomationErrorCodesClassContent
@Field
String jsonSchemaExceptionClassContent
@Field
String jsonSchemaValidatorClassContent

try {
  arcFlowUtilsClassContent =
      getStringFromResourceFile(execution, "groovy/utils/ArcFlowUtils.groovy")
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
@Field
Class flowAutomationErrorCodes
@Field
static Class arcFlowUtils

try {
  flowAutomationErrorCodes = loader.parseClass(flowAutomationErrorCodesClassContent)
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

//*****************************************************
// Validating StartTime block of selectScheduleUserTask
//*****************************************************
Instant startTime
startTime = getStartTimeDate(execution, selectScheduleUserTask?.startTime as Map)
flowInput.startTime = startTime

//*************************************************************
// Validating Show baseline KPI block of selectScheduleUserTask
//*************************************************************
Duration  kpiObservationTime
boolean showBaselineKPI
Map<String, Object> showKPIMap = selectScheduleUserTask?.baselineKPI as Map
kpiObservationTime = getShowBaselineKPIDuration(showKPIMap)
showBaselineKPI = getShowBaselineKpi(showKPIMap)
flowInput.showBaselineKPI = showBaselineKPI
flowInput.kpiObservationTime = kpiObservationTime

//******************************************************
// Validating recurrence block of selectScheduleUserTask
//******************************************************
Map<String, Object> withRecurrence = selectScheduleUserTask?.recurrence?.withRecurrence
if (withRecurrence) {
  Map<String, Object> recurrencePattern = (withRecurrence.selectRecurrencePreference as Map).recurrencePattern as Map
  String recurrenceValue
  recurrenceValue = getRecurrenceValue(recurrencePattern)
  flowInput.recurrenceValue = recurrenceValue
  Map<String, Object> recurrenceStopSettings =  (withRecurrence.selectRecurrencePreference as Map).recurrenceStop as Map
  if (recurrenceStopSettings.endDate?.specifyDate) {
    Instant endDate = Instant.parse(recurrenceStopSettings.endDate?.specifyDate)
    validateRecurrenceEndDate(execution, endDate, startTime, kpiObservationTime)
    flowInput.endDate = endDate
  } else if (recurrenceStopSettings.noEndDate) {
    errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.unsupportedNoEndDate"))
  } else if (recurrenceStopSettings.afterNumberOfOccurrences?.afterOccurrences) {
    long occurrences = (recurrenceStopSettings.afterNumberOfOccurrences as Map).afterOccurrences as long
    validateAfterNumberOfOccurrences(execution, occurrences, recurrenceValue, recurrencePattern)
    flowInput.occurrences = occurrences
  } else {
    errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.recurrenceStopNotSupported"))
  }
}

flowInput?.selectScheduleUserTask = selectScheduleUserTask

private Instant getStartTimeDate(DelegateExecution execution, Map < String, Object > startTimeMap) {
  Instant startTime
  Instant currentTime = Instant.now()
  if (startTimeMap.startTime) {
    startTime = currentTime
  } else if (startTimeMap?.specifyDate?.specifyDate) {
    startTime = Instant.parse(startTimeMap.specifyDate?.specifyDate)
    validateStartTimeIsAfterCurrentTimeAndBeforeAmonth(startTime, currentTime, execution)
  } else if (startTimeMap.hoursLater) {
    Long hoursLater = Long.valueOf(startTimeMap.hoursLater?.hoursLater)
    startTime = currentTime.plus(hoursLater, ChronoUnit.HOURS)
  } else {
    errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.startDateInvalid"))
  }
  return startTime
}

private void validateStartTimeIsAfterCurrentTimeAndBeforeAmonth(Instant startTime, Instant currentTime, DelegateExecution execution) {
  Instant monthAfterCurrentTime =
          currentTime.atZone(ZoneId.systemDefault()).plusMonths(1).toInstant()
  if (startTime.isBefore(currentTime)) {
    errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.startDateInPast"))
  }
  if (monthAfterCurrentTime.isBefore(startTime)) {
    errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.startDateMaxDelayExceeded"))
  }
}

private boolean getShowBaselineKpi(Map<String, Object> showKPIMap) {
  if (showKPIMap.no) {
    return false
  }
  return true
}

private Duration getShowBaselineKPIDuration(Map < String, Object > showKPIMap) {
  Duration KPIObservationTime
  if (showKPIMap.no) {
    KPIObservationTime = Duration.ZERO
  }
  else if (showKPIMap.yes) {
    Map<String, Object> observationTimeMap = showKPIMap.yes.time as Map
    if (observationTimeMap.hour) {
      KPIObservationTime = Duration.ofHours(1)
    }
    else if (observationTimeMap.day) {
      KPIObservationTime = Duration.ofDays(1)
    }
    else if (observationTimeMap.week) {
      KPIObservationTime = Duration.ofDays(7)
    }
  }
  return KPIObservationTime
}

private String getRecurrenceValue(Map<String, Object> recurrencePattern) {
  if (recurrencePattern.hourly != null) {
    return "PT1H"
  } else if (recurrencePattern.daily != null) {
    return "P1D"
  } else if (recurrencePattern.everyMinute != null) {
    return "PT30S"
  }
}

private void validateRecurrenceEndDate(DelegateExecution execution, Instant endDate, Instant startTime, Duration KPIObservationTime) {
  if (endDate.isBefore(startTime)) {
     errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.endDateBeforeStartDate"))
  }
  if (endDate.isBefore(startTime.plus(KPIObservationTime))) {
     errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.endDateBeforeEndOfObservation"))
  }
  if (endDate.minus(14, ChronoUnit.DAYS).minus(KPIObservationTime).isAfter(startTime)) {
     errorMessageValidation(execution, arcFlowUtils.getMessage(execution, "scheduler.error.endDateAfterTwoWeeksFromStartDate"))
  }
}

private String validateAfterNumberOfOccurrences(DelegateExecution execution, long occurrences, String recurrenceValue, Map<String, Object> recurrencePattern) {
  if (Duration.parse(recurrenceValue).multipliedBy(occurrences) > Duration.of(14, ChronoUnit.DAYS)) {
    String message = new StringBuilder()
            .append( arcFlowUtils.getMessage(execution, "scheduler.error.endDateAfterTwoWeeksFromStartDate"))
            .append(": ")
            .append(getEndDateErrorMessage(execution, recurrencePattern))
            .toString()
    errorMessageValidation(execution, message)
  }
}

private static String getEndDateErrorMessage(DelegateExecution execution, Map <String, Object> recurrencePattern) {
  if (recurrencePattern.hourly != null) {
    return arcFlowUtils.getMessage(execution, "scheduler.error.endDateAfterTwoWeeksFromStartDateHourlyExplication")
  } else if (recurrencePattern.daily != null) {
    return arcFlowUtils.getMessage(execution, "scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication")
  } else if (recurrencePattern.everyMinute != null) {
    return arcFlowUtils.getMessage(execution, "scheduler.error.endDateAfterTwoWeeksFromStartDateEMExplication")
  }
}

private errorMessageValidation(DelegateExecution execution, String message) {
  if ("InteractiveModeSchedulerValidation" == validationType) {
    throw new UsertaskInputProcessingError(flowAutomationErrorCodes.ERROR_USER_TASK_INVALID_INPUT, message)
  }
  if ("fileInputModeSchedulerValidation" == validationType) {
    EventRecorder.error(execution, message, FLOW_STEP)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,  message)
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