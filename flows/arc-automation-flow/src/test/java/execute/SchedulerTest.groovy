// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class SchedulerTest extends FlowAutomationScriptBaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Test
  void testSchedulerStartNowAndNoRecurrence() {
    Map<String, Object> flowInput = [ startTime: Instant.parse("2022-10-15T08:21:23.899530600Z"),
                                      showBaselineKPI: false, kpiObservationTime: "PTOS",
                                      selectScheduleUserTask: [recurrence: [noRecurrence: [applyResultsAutomatically: [no: true]]]]]

    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
    assertEquals("2022-10-15T08:21:23.899530600Z", delegateExecution.getVariable("startDate"))
    assertEquals(false, delegateExecution.getVariable("hasRecurrence"))
    assertEquals(null, delegateExecution.getVariable("recurrence"))
    assertEquals(null, delegateExecution.getVariable("afterOccurrences"))
    assertEquals(null, delegateExecution.getVariable("endDate"))
    assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
    assertEquals(false, delegateExecution.getVariable("showBaselineKPI"))
    assertEquals("PTOS", delegateExecution.getVariable("observationTime"))
    assertEquals("no", delegateExecution.getVariable("showConfiguration"))
    assertEquals("Optimization number: 1; ", delegateExecution.getVariable("occurrenceInfo"))
  }

  @Test
  void testStartInDateAndNoRecurrence() {
    Map<String, Object> flowInput = [selectScheduleUserTask: [startTime : [specifyDate: [specifyDate: "2021-12-23T13:33:42.165Z"]],
                                                              recurrence: [noRecurrence: [applyResultsAutomatically: [yes: true]]]]]

    delegateExecution.setVariable("flowInput", flowInput)
    runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
    assertNotNull(delegateExecution.getVariable("startDate"))
    assertEquals(false, delegateExecution.getVariable("hasRecurrence"))
    assertEquals(null, delegateExecution.getVariable("recurrence"))
    assertEquals(null, delegateExecution.getVariable("afterOccurrences"))
    assertEquals(null, delegateExecution.getVariable("endDate"))
    assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
    assertEquals("yes", delegateExecution.getVariable("showConfiguration"))
  }

 @Test
void testStartNowAndRecurrencePatternHourlyTwice() {
  Map<String, Object> flowInput = [startTime: Instant.now(), recurrenceValue: "PT1H", occurrences: 2, selectScheduleUserTask: [startTime : [startTime: true],
                                                            recurrence: [withRecurrence: [selectRecurrencePreference: [recurrencePattern: [hourly: true],
                                                                                                                       recurrenceStop   : [afterNumberOfOccurrences: [afterOccurrences: 2]]]]]]]

  delegateExecution.setVariable("flowInput", flowInput)
  runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
  assertNotNull(delegateExecution.getVariable("startDate"))
  assertEquals(true, delegateExecution.getVariable("hasRecurrence"))
  assertEquals("PT1H", delegateExecution.getVariable("recurrence"))
  assertEquals(2, delegateExecution.getVariable("afterOccurrences"))
  assertEquals(null, delegateExecution.getVariable("endDate"))
  assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
}

@Test
void testStartNowAndRecurrencePatternDailyTwice() {
  Map<String, Object> flowInput = [startTime: Instant.now(), recurrenceValue: "P1D", occurrences: 2, selectScheduleUserTask: [startTime : [startTime: true],
                                                            recurrence: [withRecurrence: [selectRecurrencePreference: [recurrencePattern: [daily: true],
                                                                                                                       recurrenceStop   : [afterNumberOfOccurrences: [afterOccurrences: 2]]]]]]]

  delegateExecution.setVariable("flowInput", flowInput)
  runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
  assertNotNull(delegateExecution.getVariable("startDate"))
  assertEquals(true, delegateExecution.getVariable("hasRecurrence"))
  assertEquals("P1D", delegateExecution.getVariable("recurrence"))
  assertEquals(2, delegateExecution.getVariable("afterOccurrences"))
  assertEquals(null, delegateExecution.getVariable("endDate"))
  assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
}

@Test
void testStartNowAndRecurrencePatternEveryMinuteTwice() {
  Map<String, Object> flowInput = [startTime: Instant.now(), recurrenceValue: "PT30S", occurrences: 2, selectScheduleUserTask: [startTime : [startTime: true],
                                                            recurrence: [withRecurrence: [selectRecurrencePreference: [recurrencePattern: [everyMinute: true],
                                                                                                                       recurrenceStop   : [afterNumberOfOccurrences: [afterOccurrences: 2]]]]]]]

  delegateExecution.setVariable("flowInput", flowInput)
  runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
  assertNotNull(delegateExecution.getVariable("startDate"))
  assertEquals(true, delegateExecution.getVariable("hasRecurrence"))
  assertEquals("PT30S", delegateExecution.getVariable("recurrence"))
  assertEquals(2, delegateExecution.getVariable("afterOccurrences"))
  assertEquals(null, delegateExecution.getVariable("endDate"))
  assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
}

@Test
void testStartAtDateAndRecurrencePatternEveryMinuteWithEndDate() {
  Map<String, Object> flowInput = [startTime: Instant.now(), recurrenceValue: "PT30S", endDate: Instant.parse("2021-12-23T14:33:42.165Z"), selectScheduleUserTask: [startTime : [specifyDate: [specifyDate: "2021-12-23T13:33:42.165Z"]],
                                                            recurrence: [withRecurrence: [selectRecurrencePreference: [recurrencePattern: [everyMinute: true],
                                                                                                                       recurrenceStop   : [endDate: [specifyDate: "2021-12-23T14:33:42.165Z"]]]]]]]

  delegateExecution.setVariable("flowInput", flowInput)
  runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
  assertNotNull(delegateExecution.getVariable("startDate"))
  assertEquals(true, delegateExecution.getVariable("hasRecurrence"))
  assertEquals("PT30S", delegateExecution.getVariable("recurrence"))
  assertEquals(null, delegateExecution.getVariable("afterOccurrences"))
  assertEquals( Instant.parse("2021-12-23T14:33:42.165Z"), delegateExecution.getVariable("endDate"))
  assertEquals(true, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
}

 @Test
void testStartAtDateAndRecurrencePatternEveryMinuteWithEndDateInFuture() {
   Instant futureInstant = Instant.now().plus(1, ChronoUnit.HOURS)
  Map<String, Object> flowInput =
      [startTime: Instant.parse("2021-12-23T13:33:42.165Z"),
       recurrenceValue: "PT30S", endDate: futureInstant,
       showBaselineKPI: false,
       kpiObservationTime: "PTOS",
       selectScheduleUserTask: [startTime : [specifyDate: [specifyDate: "2021-12-23T13:33:42.165Z"]],
       recurrence: [withRecurrence: [selectRecurrencePreference: [recurrencePattern: [everyMinute: true],
       recurrenceStop: [endDate: [specifyDate:futureInstant]]]]]]]

  delegateExecution.setVariable("flowInput", flowInput)
  runFlowScript(delegateExecution, "groovy/Scheduler.groovy")
  assertNotNull(delegateExecution.getVariable("startDate"))
  assertEquals(true, delegateExecution.getVariable("hasRecurrence"))
  assertEquals("PT30S", delegateExecution.getVariable("recurrence"))
  assertEquals(null, delegateExecution.getVariable("afterOccurrences"))
  assertEquals(futureInstant, delegateExecution.getVariable("endDate"))
  assertEquals(false, Boolean.valueOf(delegateExecution.getVariable("endDateOccurred") as String))
  assertEquals("PTOS", delegateExecution.getVariable("observationTime"))
  assertEquals(false, delegateExecution.getVariable("showBaselineKPI"))
  assertEquals("Optimization number: 1; ", delegateExecution.getVariable("occurrenceInfo"))
}

}
