// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest

import static groovy.util.GroovyTestCase.assertEquals

class RecurrenceTest extends FlowAutomationScriptBaseTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none()

    @Test
    void testAfterOccurrencesSuccess() {
        int afterOccurrences = 5
        delegateExecution.setVariable("hasRecurrence", true)
        delegateExecution.setVariable("occurrenceNumber", 5)
        delegateExecution.setVariable("afterOccurrences", afterOccurrences)
        String recurrence = "P1D"
        delegateExecution.setVariable("recurrence", recurrence)
        delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
        runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
        assertEquals(4, delegateExecution.getVariable("afterOccurrences"))
        assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
    }

    @Test
    void testFutureEndDateSuccess() {
        String recurrence = "P1D"
        delegateExecution.setVariable("recurrence", recurrence)
        delegateExecution.setVariable("hasRecurrence", true)
        delegateExecution.setVariable("occurrenceNumber", 5)
        String endDate = Instant.now().plus(2, ChronoUnit.DAYS).toString()
        delegateExecution.setVariable("endDate", endDate)
        delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
        runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
        assertEquals(false, delegateExecution.getVariable("endDateOccurred"))
        assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
    }

    @Test
    void testPastEndDateSuccess() {
        String recurrence = "P1D"
        delegateExecution.setVariable("recurrence", recurrence)
        delegateExecution.setVariable("hasRecurrence", true)
        delegateExecution.setVariable("occurrenceNumber", 5)
        String endDate = "2020-12-23T13:33:42.165Z"
        delegateExecution.setVariable("endDate", endDate)
        delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
        runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
        assertEquals(true, delegateExecution.getVariable("endDateOccurred"))
        assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
    }

  @Test
  void testEndDateNotFarEnoughDaily() {
    String recurrence = "P1D"
    delegateExecution.setVariable("recurrence", recurrence)
    delegateExecution.setVariable("hasRecurrence", true)
    delegateExecution.setVariable("occurrenceNumber", 5)
    delegateExecution.setVariable("endDate", Instant.now().plus(4, ChronoUnit.HOURS).toString())
    delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
    runFlowScript(delegateExecution, "groovy/Recurrence.groovy")

    assertEquals(true, delegateExecution.getVariable("endDateOccurred"))
    assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
  }

  @Test
  void testEndDateFarEnoughDaily() {
    String recurrence = "P1D"
    delegateExecution.setVariable("recurrence", recurrence)
    delegateExecution.setVariable("hasRecurrence", true)
    delegateExecution.setVariable("occurrenceNumber", 5)
    delegateExecution.setVariable("endDate", Instant.now().plus(28, ChronoUnit.HOURS).toString())
    delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
    runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
    assertEquals(false, delegateExecution.getVariable("endDateOccurred"))
    assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
  }

  @Test
  void testEndDateNotFarEnoughHourly() {
    String recurrence = "PT1H"
    delegateExecution.setVariable("recurrence", recurrence)
    delegateExecution.setVariable("hasRecurrence", true)
    delegateExecution.setVariable("occurrenceNumber", 5)
    delegateExecution.setVariable("endDate", Instant.now().plus(10, ChronoUnit.MINUTES).toString())
    delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
    runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
    assertEquals(true, delegateExecution.getVariable("endDateOccurred"))
    assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
  }

  @Test
  void testEndDateFarEnoughHourly() {
    String recurrence = "PT1H"
    delegateExecution.setVariable("recurrence", recurrence)
    delegateExecution.setVariable("hasRecurrence", true)
    delegateExecution.setVariable("occurrenceNumber", 5)
    delegateExecution.setVariable("endDate", Instant.now().plus(70, ChronoUnit.MINUTES).toString())
    delegateExecution.setVariable("nextOptimizationDate", Instant.now().plus(Duration.parse(recurrence)).toString())
    runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
    assertEquals(false, delegateExecution.getVariable("endDateOccurred"))
    assertEquals(6, delegateExecution.getVariable("occurrenceNumber"))
  }

  @Test
  void testNoRecurrence() {
    delegateExecution.setVariable("hasRecurrence", false)
    Instant nextOptimizationDate = Instant.now()
    delegateExecution.setVariable("nextOptimizationDate", nextOptimizationDate.toString())
    runFlowScript(delegateExecution, "groovy/Recurrence.groovy")
  }
}