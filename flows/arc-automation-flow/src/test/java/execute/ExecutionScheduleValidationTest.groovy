// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import groovy.utils.ArcFlowUtils
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

import org.camunda.bpm.engine.delegate.BpmnError
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.flowapi.usertask.UsertaskInputProcessingError
import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest

import static org.junit.Assert.assertEquals

class ExecutionScheduleValidationTest extends FlowAutomationScriptBaseTest {

  static String daily = "daily"
  static String hourly = "hourly"
  static String hour = "hour"
  static String day = "day"
  static String week = "week"
  static String time = "time"
  static String EM = "everyMinute"
  static String yes = "yes"
  static String no = "no"

  String FLOW_STEP = ArcFlowUtils.getMessage(delegateExecution, "wf.setup.validateExecutionSchedule")
  def flowInput = [selectGNBDUs: [:], selectScheduleUserTask: [:]]
  def selectScheduleUserTask = [startTime: [:], baselineKPI: [:], recurrence: [:]]
  def selectRecurrencePreference = [recurrencePattern: [:], recurrenceStop: [:]]

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Test
  void specifyDateInThePast() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild("2022-03-14T16:25:50.000Z")
    checkOptimizationBuild(no, true)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "scheduler.error.startDateInPast"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void specifyDateAfterOneMonth() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusMonths(2).toInstant().toString())
    baselineKPIBuild(no, true)
    recurrencePatternBuild(EM, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusMonths(3).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "scheduler.error.startDateMaxDelayExceeded"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void endDateBeforeStartDate() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(2).toInstant().toString())
    baselineKPIBuild(yes, day, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(1).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "scheduler.error.endDateBeforeStartDate"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void endDateBeforeEndOfObservation() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusDays(3).toInstant().toString())
    baselineKPIBuild(yes, week, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(1).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(ArcFlowUtils
        .getMessage(delegateExecution, "scheduler.error.endDateBeforeEndOfObservation"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void plannedOptimizationMoreThanTwoWeeks() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(2).toInstant().toString())
    baselineKPIBuild(no, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusDays(29).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(
        delegateExecution, "scheduler.error.endDateAfterTwoWeeksFromStartDate"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void plannedOptimizationMoreThanTwoWeeksWithObservation() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(2).toInstant().toString())
    baselineKPIBuild(yes, day, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusDays(30).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(ArcFlowUtils.getMessage(
        delegateExecution, "scheduler.error.endDateAfterTwoWeeksFromStartDate"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void noEndDateNotSupportedYet() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(true)
    baselineKPIBuild(yes, week, true)
    recurrencePatternBuild(EM, true)
    recurrenceStopBuild(true)
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "scheduler.error.unsupportedNoEndDate"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void hoursLaterEndDate() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(15)
    baselineKPIBuild(no, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(
        Instant.now().atZone(ZoneId.systemDefault()).plusDays(3).toInstant().toString())
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    def expectedFlowInput = buildFlowInput("2022-08-15T04:29:53.075Z", "PT0S", "no", "P1D", "2022-08-17T13:29:52.403Z", 0)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    assertEquals(expectedFlowInput, delegateExecution.getVariable("flowInput"))
  }

  @Test
  void afterOccurrencesMoreThanTwoWeeksInteractiveMode() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(true)
    baselineKPIBuild(yes, day, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(15)
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(
            delegateExecution,"scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " +
        ArcFlowUtils.getMessage(
            delegateExecution,"scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void afterOccurrencesMoreThanTwoWeeksFileInputMode() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    startTimeBuild(true)
    baselineKPIBuild(yes, day, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(15)
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    thrown.expect(BpmnError.class)
    thrown.expectMessage(
            ArcFlowUtils.getMessage(
                    delegateExecution,"scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " +
                    ArcFlowUtils.getMessage(
                            delegateExecution,"scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication"))
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
  }

  @Test
  void hoursLaterNumberOfOccurrence() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(15)
    baselineKPIBuild(yes, hour, true)
    recurrencePatternBuild(hourly, true)
    recurrenceStopBuild(15)
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    def expectedFlowInput = buildFlowInput("2022-08-15T04:58:37.092Z", "PT1H", "yes", "PT1H", "", 15)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    assertEquals(expectedFlowInput, delegateExecution.getVariable("flowInput"))
  }

  @Test
  void hoursLaterNoRecurrence() {
    delegateExecution.setVariable("setupMode", "InteractiveModeSchedulerValidation")
    startTimeBuild(15)
    baselineKPIBuild(no, true)
    checkOptimizationBuild(yes, true)
    def expectedFlowInput = buildFlowInput("2022-08-15T05:15:01.999Z", "PT0S", "no", "", "", 0)
    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    assertEquals(expectedFlowInput, delegateExecution.getVariable("flowInput"))
  }

  @Test
  void specifyDateInThePastFileInputMode() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    startTimeBuild("2022-03-14T16:25:50.000Z")
    baselineKPIBuild(no, true)
    checkOptimizationBuild(no, true)
    String message = ArcFlowUtils.getMessage(delegateExecution, "scheduler.error.startDateInPast")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR, message, FLOW_STEP)
  }

  @Test
  void specifyDateAfterOneMonthFileInputMode() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusMonths(2).toInstant().toString())
    baselineKPIBuild(no, true)
    checkOptimizationBuild(no, true)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    String message = ArcFlowUtils
            .getMessage(delegateExecution, "scheduler.error.startDateMaxDelayExceeded")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR, message, FLOW_STEP)
  }

  @Test
  void endDateBeforeStartDateFileInputMode() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(2).toInstant().toString())
    baselineKPIBuild(no, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(1).toInstant().toString())
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    delegateExecution.setVariable("flowInput", flowInput)
    String message = ArcFlowUtils
            .getMessage(delegateExecution, "scheduler.error.endDateBeforeStartDate")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR, message, FLOW_STEP)
  }

  @Test
  void plannedOptimizationWithNumberOccurrencesMoreThanTwoWeeksFileInputMode() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    startTimeBuild(Instant.now().atZone(ZoneId.systemDefault()).plusWeeks(2).toInstant().toString())
    baselineKPIBuild(no, true)
    recurrencePatternBuild(daily, true)
    recurrenceStopBuild(15)
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    delegateExecution.setVariable("flowInput", flowInput)
    String message = ArcFlowUtils.getMessage(delegateExecution, "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": "
    ArcFlowUtils.getMessage(delegateExecution,"scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR,
        message, FLOW_STEP)
  }

  @Test
  void notSupportedRecurrenceStop() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    selectScheduleUserTask.startTime.startTime = true
    selectScheduleUserTask.baselineKPI = [no: true]
    selectScheduleUserTask.recurrence.withRecurrence = [:]
    flowInput.selectScheduleUserTask = selectScheduleUserTask
    selectRecurrencePreference.recurrencePattern.everyMinute = true
    selectRecurrencePreference.recurrenceStop.thisIsNotSupported = [:]
    selectScheduleUserTask.recurrence.withRecurrence =
        [selectRecurrencePreference: selectRecurrencePreference]
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    delegateExecution.setVariable("flowInput", flowInput)
    String message = ArcFlowUtils
        .getMessage(delegateExecution, "scheduler.error.recurrenceStopNotSupported")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR,
        message, FLOW_STEP)
  }

  @Test
  void notSupportedStartTime() {
    delegateExecution.setVariable("setupMode", "fileInputModeSchedulerValidation")
    selectScheduleUserTask.startTime.thisIsNotSupported = [:]
    selectScheduleUserTask.baselineKPI = [no: true]
    selectScheduleUserTask.recurrence.withRecurrence = [:]
    selectRecurrencePreference.recurrencePattern.everyMinute = true
    selectRecurrencePreference.recurrenceStop.endDate = [specifyDate: Instant.now().atZone(
        ZoneId.systemDefault()).plusDays(9).toInstant().toString()]
    selectScheduleUserTask.recurrence.withRecurrence =
        [selectRecurrencePreference: selectRecurrencePreference]
    delegateExecution.setVariable("selectScheduleUserTask", selectScheduleUserTask)
    delegateExecution.setVariable("flowInput", flowInput)
    String message = ArcFlowUtils.getMessage(delegateExecution, "scheduler.error.startDateInvalid")
    thrown.expect(BpmnError.class)
    thrown.expectMessage(message)
    runFlowScript(delegateExecution, "groovy/ExecutionScheduleValidation.groovy")
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.ERROR, message,
        FLOW_STEP)
  }

  void startTimeBuild(boolean b) {
    selectScheduleUserTask.startTime.startTime = b
  }

  void startTimeBuild(String specifyDate) {
    selectScheduleUserTask.startTime.specifyDate = [specifyDate: specifyDate]
  }

  void startTimeBuild(int hours) {
    selectScheduleUserTask.startTime.hoursLater = [hoursLater: hours]
  }

  void baselineKPIBuild(String decision, boolean b) {
    selectScheduleUserTask.baselineKPI = [(decision): b]
  }

  void baselineKPIBuild(String decision, String period, boolean b) {
    selectScheduleUserTask.baselineKPI = [(decision): [(time): [(period): b]]]
  }

  void checkOptimizationBuild(String decision, boolean b) {
    selectScheduleUserTask.recurrence.noRecurrence = [applyResultsAutomatically: [(decision): b]]
  }

  void recurrencePatternBuild(String pattern, boolean b) {
    selectScheduleUserTask.recurrence.withRecurrence = [:]
    selectRecurrencePreference.recurrencePattern = [(pattern): b]
  }

  void recurrenceStopBuild(int occurrences) {
    selectRecurrencePreference.recurrenceStop.afterNumberOfOccurrences =
        [afterOccurrences: occurrences]
    selectScheduleUserTask.recurrence.withRecurrence =
        [selectRecurrencePreference: selectRecurrencePreference]
  }

  void recurrenceStopBuild(String date) {
    selectRecurrencePreference.recurrenceStop.endDate = [specifyDate: date]
    selectScheduleUserTask.recurrence.withRecurrence =
        [selectRecurrencePreference: selectRecurrencePreference]
  }

  void recurrenceStopBuild(boolean b) {
    selectRecurrencePreference.recurrenceStop.noEndDate = b
    selectScheduleUserTask.recurrence.withRecurrence =
        [selectRecurrencePreference: selectRecurrencePreference]
  }

  LinkedHashMap<String, LinkedHashMap> buildFlowInput(String startTime, String kpiObservationTime, String showConfiguration, String recurrenceValue, String endDate, long occurrences) {
    flowInput.selectScheduleUserTask = selectScheduleUserTask
    flowInput.selectGNBDUs = [:]
    flowInput["startTime"] = Instant.parse(startTime)
    flowInput.kpiObservationTime = Duration.parse(kpiObservationTime)
    flowInput.showConfiguration = showConfiguration
    if (recurrenceValue?.trim()) {
      flowInput.recurrenceValue = recurrenceValue
    }
    if (endDate?.trim()) {
      flowInput.endDate = Instant.parse(endDate)
    }
    if (occurrences != 0) {
      flowInput.occurrences = occurrences
    }
    return flowInput
  }
}