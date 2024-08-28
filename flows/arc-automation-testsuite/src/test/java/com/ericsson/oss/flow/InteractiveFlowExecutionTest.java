/*
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

package com.ericsson.oss.flow;

import com.ericsson.oss.flow.util.FlowTestUtils;
import com.ericsson.oss.flow.util.ServiceMockUtils;
import com.ericsson.oss.services.flowautomation.model.FlowDefinition;
import com.ericsson.oss.services.flowautomation.model.FlowExecution;
import com.ericsson.oss.services.flowautomation.model.UserTask;
import com.ericsson.oss.services.flowautomation.model.UserTaskSchema;
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationRestBaseTest;
import com.ericsson.oss.services.flowautomation.test.fwk.UsertaskCheckBuilder;
import com.ericsson.oss.services.flowautomation.test.fwk.UsertaskInputBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity.INFO;
import static com.ericsson.oss.services.flowautomation.test.fwk.TestUtils.getFlowPackageBytes;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.equalTo;

public class InteractiveFlowExecutionTest extends FlowAutomationRestBaseTest {

  private static final boolean isOnline;
  private static final ServiceMockUtils serviceMockUtils;

  static {
    try {
      InputStream is = InteractiveFlowExecutionTest.class.getResourceAsStream("config.properties");
      Properties configProperties = new Properties();
      configProperties.load(is);
      isOnline = Boolean.parseBoolean(configProperties.getProperty("onlineTesting"));
      serviceMockUtils = isOnline ? null : new ServiceMockUtils();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private FlowExecution flowExecution;
  private final List<Map<String, Object>> defaultGNBDUs = getDefaultGNBDUs();
  private final List<Map<String, Object>> selectedGNBDUs = getSelectedGNBDUs();
  private final List<Map<String, Object>> emptyList = new ArrayList<>();
  private final String flowId = "com.ericsson.oss.flow.arc-automation";
  static final String immed = "Immediately";
  static final String E_M = "Every 30 seconds";
  static final String HOURLY = "Hourly";
  static final String DAILY = "Daily";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    String flowPackage = "arc-automation-flow";
    FlowDefinition flowDefinition = importFlow(flowId, getFlowPackageBytes(flowPackage));
    FlowTestUtils.setRoutes(isOnline, wireMockRule);
    if (!isOnline) {
      setMockedServices();
    }

    /*
     * Setup phase
     */
    // Start the flow
    flowExecution = startFlowExecution(flowDefinition, createUniqueInstanceName(flowId));

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    // Choose setup
    completeUsertaskChooseSetupInteractive(flowExecution);

    // Check license
    completeUsertaskNoInput(flowExecution, "License Check");

    // Select GNBDUs
    checkUsertaskActive(flowExecution, "Select gNBDUs");
    checkUsertask(flowExecution, "Select gNBDUs",
        new UsertaskCheckBuilder().check("Select Node > GNBDUs table", defaultGNBDUs));
    completeUsertask(flowExecution, "Select gNBDUs",
        new UsertaskInputBuilder().input("Select Node > GNBDUs table", selectedGNBDUs));
    // Exclude GNBDUs pairs
    checkUsertaskActive(flowExecution, "Unwanted gNodeB Pairs Selection");
    UsertaskInputBuilder unwantedGnodeB = new UsertaskInputBuilder();
    unwantedGnodeB.input("Select excluded links > List of excluded links", "");
    unwantedGnodeB.input("Select excluded links > Excluded Link Primary gNodeB", emptyList);
    unwantedGnodeB.input("Select excluded links > Excluded Link Secondary gNodeB", emptyList);
    completeUsertask(flowExecution, "Unwanted gNodeB Pairs Selection", unwantedGnodeB);
    // Mandatory GNBDUs pairs
    checkUsertaskActive(flowExecution, "Mandatory gNodeB Pairs Selection");
    UsertaskInputBuilder mandatoryGnodeB = new UsertaskInputBuilder();
    unwantedGnodeB.input("Select mandatory links > List of mandatory links", "");
    unwantedGnodeB.input("Select mandatory links > Mandatory Link Primary gNodeB", emptyList);
    unwantedGnodeB.input("Select mandatory links > Mandatory Link Secondary gNodeB", emptyList);

    completeUsertask(flowExecution, "Mandatory gNodeB Pairs Selection", unwantedGnodeB);
  }

  @After
  public void after() {
    removeFlow(flowId);
  }

  @Test
  public void immedNoRecurrenceApplyResultsAutomatically() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "Yes");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, "Yes", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedNoRecurrenceConfirmConfigurationYes() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "No");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, "No", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "Yes");
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedNoRecurrenceConfirmConfigurationNo() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "No");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, "No", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "No");
  }

  @Test
  public void immedAfterOccurrencesEMOverLimit() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, E_M, 40321);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateEMExplication");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"" + errorMessage + "\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(errorMessage));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void hoursLaterAfterOccurrencesHourlyOverLimit() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, 48);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, HOURLY, 337);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateHourlyExplication");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"" + errorMessage + "\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(errorMessage));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void hoursLaterEndDateDailyOverLimit() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, 72);
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusDays(14).plusHours(73)
        .toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, DAILY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"" + errorMessage + "\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(errorMessage));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void startDateAfterOccurrencesDailyOverLimit() {
    // Select Schedule UserTask
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusDays(5).toInstant();
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, DAILY, 15);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"" + errorMessage + "\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(errorMessage));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void immedAfterOccurrencesEM() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, E_M, 4);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, null, E_M, 4);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);

    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 1
        + "; ";
    oneOptimizationLoop(flowExecution, 1);

    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));

    final String DEPLOYMENT_STEP = FlowTestUtils.getMessage("wf.execute.deployment");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    checkUsertaskActive(flowExecution, "Optimization Results Configuration");
    optimizationResultsConfiguration(flowExecution, "no");

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 2 + "; ";
    oneOptimizationLoop(flowExecution, 2);

    checkUsertaskActive(flowExecution, "Confirm Configuration");
    confirmConfiguration(flowExecution, "Yes");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.acceptedConfiguration"));
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 3 + "; ";
    oneOptimizationLoop(flowExecution, 3);

    checkUsertaskActive(flowExecution, "Confirm Configuration");
    confirmConfiguration(flowExecution, "No");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.droppedConfiguration"));

    checkUsertaskActive(flowExecution, "Optimization Results Configuration");
    optimizationResultsConfiguration(flowExecution, "yes");

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 4 + "; ";
    oneOptimizationLoop(flowExecution, 4);

    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    checkExecutionState(flowExecution, "COMPLETED");

    //Check execution report
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedEndDateEM() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(20).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, E_M, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, null, E_M, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);
    //Check execution report
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedEndDateHourly() throws JSONException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusMinutes(20).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, HOURLY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, null, HOURLY, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);

    //Check execution report
    checkExecutionReport(flowExecution);
    final String report = getExecutionReport(flowExecution);
    JSONObject JsonReport = new JSONObject(report);
    String endTimeText = (new JSONObject(JsonReport.get("header").toString())).getString("endTime");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    Instant endTime = dateTimeFormatter.parse(endTimeText, Instant::from);

    assert (endTime.isBefore(endDate));
    with(report).assertThat("$.header.status", equalTo("COMPLETED"));
  }

  @Test
  public void immedEndDateDaily() throws JSONException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusHours(23).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, DAILY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, null, HOURLY, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);

    //Check execution report
    checkExecutionReport(flowExecution);
    final String report = getExecutionReport(flowExecution);
    JSONObject JsonReport = new JSONObject(report);
    String endTimeText = (new JSONObject(JsonReport.get("header").toString())).getString("endTime");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    Instant endTime = dateTimeFormatter.parse(endTimeText, Instant::from);

    assert (endTime.isBefore(endDate));
    with(report).assertThat("$.header.status", equalTo("COMPLETED"));
  }

  @Test
  public void immedEndDateReachedBeforeOptimization() throws InterruptedException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, E_M, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, null, DAILY, endDate.toString());

    /*
     * Execute phase
     */
    checkUsertaskActive(flowExecution, "Visualize gNB to Trigger UE Measurement");
    sleep(15000);
    triggerUEMeasurements(flowExecution);
    String message = FlowTestUtils.getMessage("wf.execute.endDateBeforeStart");
    checkExecutionSummaryReport(flowExecution, message);
    checkExecutionEventIsRecorded(flowExecution, INFO, message);
    checkExecutionState(flowExecution, "COMPLETED");
  }

  @Test
  public void startDateInThePastError() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, "2022-02-03T00:00:00.000Z");
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, E_M, 4);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"Start date is in the past\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(FlowTestUtils.getMessage("scheduler.error.startDateInPast")));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void startDateAfterOneMonthError() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, Instant.now().atZone(ZoneId.systemDefault())
        .plusMonths(2).toInstant().toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, E_M, 4);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"" + FlowTestUtils.getMessage("scheduler.error.startDateMaxDelayExceeded")
          + "\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(
          FlowTestUtils.getMessage("scheduler.error.startDateMaxDelayExceeded")));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void startDateNoRecurrenceApplyResultsAutomatically() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "Yes");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), "Yes", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);
  }

  @Test
  public void startDateNoRecurrenceConfirmConfigurationYes() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "No");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), "No", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "Yes");
    checkExecutionReport(flowExecution);
  }

  @Test
  public void startDateNoRecurrenceConfirmConfigurationNo() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "No");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), "No", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "No");
  }

  @Test
  public void startDateAfterOccurrencesEM() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, E_M, 4);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), null, E_M, 4);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);

    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 1
        + "; ";
    oneOptimizationLoop(flowExecution, 1);

    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));

    final String DEPLOYMENT_STEP = FlowTestUtils.getMessage("wf.execute.deployment");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    checkUsertaskActive(flowExecution, "Optimization Results Configuration");
    optimizationResultsConfiguration(flowExecution, "no");

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 2 + "; ";
    oneOptimizationLoop(flowExecution, 2);

    checkUsertaskActive(flowExecution, "Confirm Configuration");
    confirmConfiguration(flowExecution, "Yes");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.acceptedConfiguration"));
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 3 + "; ";
    oneOptimizationLoop(flowExecution, 3);

    checkUsertaskActive(flowExecution, "Confirm Configuration");
    confirmConfiguration(flowExecution, "No");
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.droppedConfiguration"));

    checkUsertaskActive(flowExecution, "Optimization Results Configuration");
    optimizationResultsConfiguration(flowExecution, "yes");

    occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 4 + "; ";
    oneOptimizationLoop(flowExecution, 4);

    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), DEPLOYMENT_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
        DEPLOYMENT_STEP);

    checkExecutionState(flowExecution, "COMPLETED");

    //Check execution report
    checkExecutionReport(flowExecution);
  }

  @Test
  public void startDateEndDateEM() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(20).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, E_M, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), null, HOURLY, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);
  }

  @Test
  public void startDateEndDateHourly() throws JSONException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusMinutes(20).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, HOURLY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), null, HOURLY, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);

    //Check execution report
    checkExecutionReport(flowExecution);
    final String report = getExecutionReport(flowExecution);
    JSONObject JsonReport = new JSONObject(report);
    String endTimeText = (new JSONObject(JsonReport.get("header").toString())).getString("endTime");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    Instant endTime = dateTimeFormatter.parse(endTimeText, Instant::from);

    assert (endTime.isBefore(endDate));
    with(report).assertThat("$.header.status", equalTo("COMPLETED"));
  }

  @Test
  public void startDateEndDateDaily() throws JSONException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusHours(23).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, DAILY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), null, DAILY, endDate.toString());

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);

    //Check execution report
    checkExecutionReport(flowExecution);
    final String report = getExecutionReport(flowExecution);
    JSONObject JsonReport = new JSONObject(report);
    String endTimeText = (new JSONObject(JsonReport.get("header").toString())).getString("endTime");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    Instant endTime = dateTimeFormatter.parse(endTimeText, Instant::from);

    assert (endTime.isBefore(endDate));
    with(report).assertThat("$.header.status", equalTo("COMPLETED"));
  }

  @Test
  public void startDateEndDateReachedBeforeOptimization() throws InterruptedException {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(5).toInstant();
    selectExecutionScheduleStartTime(caseOfTest, startTime.toString());
    selectExecutionScheduleKPI(caseOfTest);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    selectExecutionScheduleRecurrence(caseOfTest, DAILY, endDate.toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(startTime.toString(), null, DAILY, endDate.toString());

    /*
     * Execute phase
     */
    checkUsertaskActive(flowExecution, "Visualize gNB to Trigger UE Measurement");
    sleep(15000);
    triggerUEMeasurements(flowExecution);
    String message = FlowTestUtils.getMessage("wf.execute.endDateBeforeStart");
    checkExecutionSummaryReport(flowExecution, message);
    checkExecutionEventIsRecorded(flowExecution, INFO, message);
    checkExecutionState(flowExecution, "COMPLETED");
  }

  @Test
  public void endDateBeforeStartDateError() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, Instant.now().atZone(ZoneId.systemDefault())
        .plusWeeks(2).toInstant().toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, HOURLY, Instant.now().atZone(
        ZoneId.systemDefault()).plusWeeks(1).toInstant().toString());
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"End date cannot be before start date\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(
          FlowTestUtils.getMessage("scheduler.error.endDateBeforeStartDate")));
      assert (e.getMessage().contains("UsertaskInputProcessingError"));
    }
  }

  @Test
  public void noEndDateNotSupportedYetError() {
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest, Instant.now().atZone(ZoneId.systemDefault())
        .plusWeeks(2).toInstant().toString());
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleRecurrence(caseOfTest, DAILY);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    try {
      completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);
      Assert.fail("\"NoEndDate is not supported\" error is not detected");
    } catch (AssertionError e) {
      assert (e.getClass().equals(AssertionError.class));
      assert (e.getMessage().contains(
          FlowTestUtils.getMessage("scheduler.error.unsupportedNoEndDate")));
    }
  }

  @Test
  public void immedNoRecurrenceStopGracefullyTest() {
    String stopExternalServiceFlowStep = FlowTestUtils.getMessage("wf.execute.stopExternalServices");
    String startOptimizationStep = FlowTestUtils.getMessage("wf.execute.startOptimization");
    String assignOptimizationIdStep = FlowTestUtils.getMessage("wf.execute.optimizationId");
    // Select Schedule UserTask
    UsertaskInputBuilder caseOfTest = new UsertaskInputBuilder();
    selectExecutionScheduleStartTime(caseOfTest);
    selectExecutionScheduleKPI(caseOfTest);
    selectExecutionScheduleNoRecurrence(caseOfTest, "No");
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    completeUsertask(flowExecution, "Select Execution Schedule", caseOfTest);

    endSetupPhase(immed, "No", null, null);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);

    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("optimization.id.successful"), assignOptimizationIdStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("startOptimization.status.successfulStart"), startOptimizationStep);
    checkUsertaskActive(flowExecution, "Stop Instance");
    completeUsertaskNoInput(flowExecution, "Stop Instance");
    checkUsertaskActive(flowExecution, "Confirm Stop Instance");
    completeUsertask(flowExecution, "Confirm Stop Instance",
            new UsertaskInputBuilder().input("Confirm stopping instance? > Yes",true));
    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("stopExternalServices.start"), stopExternalServiceFlowStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("stopExternalServices.successful"), stopExternalServiceFlowStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("stopExternalServices.arcOptimizationResponse.successful"), stopExternalServiceFlowStep);
    checkExecutionSummaryReport(flowExecution,"Flow stopped gracefully");
    checkExecutionState(flowExecution, "COMPLETED");
  }

  private void setMockedServices() {
    serviceMockUtils.mockCtsAvailableGnbdus(wireMockServerSimulator);
    serviceMockUtils.mockUeMeasurements(wireMockServerSimulator);
    serviceMockUtils.mockTriggerKpi(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationCreate(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationStart(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationStatus(wireMockServerSimulator);
    serviceMockUtils.mockPartnerConfiguration(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationFinish(wireMockServerSimulator);
  }

  private List<Map<String, Object>> getDefaultGNBDUs() {
    Map<String, Object> gnbduMap1 = new LinkedHashMap<>();
    gnbduMap1.put("gNBName", "Europe/Ireland/NR02gNodeBRadio00021/NR02gNodeBRadio00021/1");
    gnbduMap1.put("gNBId", 270);
    gnbduMap1.put("gNBCmHandle", "DC7B963A177405864810A2F5E640762D");

    Map<String, Object> gnbduMap2 = new LinkedHashMap<>();
    gnbduMap2.put("gNBName", "Europe/Ireland/NR04gNodeBRadio00023/NR04gNodeBRadio00023/1");
    gnbduMap2.put("gNBId", 279);
    gnbduMap2.put("gNBCmHandle", "18A620FDE38287A0B4095A9A28AD5045");
    return Stream.of(gnbduMap1, gnbduMap2).collect(Collectors.toList());
  }

  private List<Map<String, Object>> getSelectedGNBDUs() {
    Map<String, Object> gnbduMap1 = new LinkedHashMap<>();
    gnbduMap1.put("gNBName", "Europe/Ireland/NR02gNodeBRadio00021/NR02gNodeBRadio00021/1");
    gnbduMap1.put("gNBId", 270);
    gnbduMap1.put("gNBCmHandle", "DC7B963A177405864810A2F5E640762D");
    Map<String, Object> gnbduMap2 = new LinkedHashMap<>();
    gnbduMap2.put("gNBName", "Europe/Ireland/NR04gNodeBRadio00023/NR04gNodeBRadio00023/1");
    gnbduMap2.put("gNBId", 279);
    gnbduMap2.put("gNBCmHandle", "18A620FDE38287A0B4095A9A28AD5045");
    return Stream.of(gnbduMap1, gnbduMap2).collect(Collectors.toList());
  }

  private void selectExecutionScheduleStartTime(UsertaskInputBuilder caseOfTest) {
    caseOfTest.input("Select Execution Schedule > Start time > Immediately", true);
  }

  private void selectExecutionScheduleStartTime(UsertaskInputBuilder caseOfTest, String date) {
    caseOfTest.input("Select Execution Schedule > Start time > Specify date > date and time", date);
  }

  private void selectExecutionScheduleStartTime(UsertaskInputBuilder caseOfTest, int hours) {
    caseOfTest.input("Select Execution Schedule > Start time > Hours later > hour", hours);
  }

  private void selectExecutionScheduleKPI(UsertaskInputBuilder caseOfTest) {
    caseOfTest.input("Select Execution Schedule > Show baseline KPI > No", true);
  }

  private void selectExecutionScheduleNoRecurrence(UsertaskInputBuilder caseOfTest,
      String decision) {
    caseOfTest.input(
        "Select Execution Schedule > recurrence > no > Apply Optimization Results Automatically > "
            + decision, true);
  }

  @SuppressWarnings("SameParameterValue")
  private void selectExecutionScheduleRecurrence(UsertaskInputBuilder caseOfTest,
      String recurrencePattern) {
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Pattern > "
            + recurrencePattern, true);
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Stop > No end Date",
        true);
  }

  private void selectExecutionScheduleRecurrence(UsertaskInputBuilder caseOfTest,
      String recurrencePattern, String recurrenceStop) {
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Pattern > "
            + recurrencePattern, true);
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Stop > End by > date and time",
        recurrenceStop);
  }

  private void selectExecutionScheduleRecurrence(UsertaskInputBuilder caseOfTest,
      String recurrencePattern, int recurrenceStop) {
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Pattern > "
            + recurrencePattern, true);
    caseOfTest.input(
        "Select Execution Schedule > recurrence > yes > Fill recurrence > Recurrence Stop > After number of occurences > occurrences",
        recurrenceStop);
  }

  private void endSetupPhase(String startTime, String decision, String recurrencePattern,
      Object recurrenceStop) {

    UsertaskCheckBuilder usertaskCheckBuilder = new UsertaskCheckBuilder().check(
        "Select Node > GNBDUs table", selectedGNBDUs);

    if (immed.equals(startTime)) {
      usertaskCheckBuilder.check("Select execution schedule > Start time > Immediately", true);
    } else {
      usertaskCheckBuilder.check(
          "Select execution schedule > Start time > Specify date > date and time", startTime);
    }

    if (decision == null) {

      usertaskCheckBuilder.check(
          "Select execution schedule > recurrence > yes > Fill recurrence > Recurrence  Pattern > "
              + recurrencePattern, true);
      if (recurrenceStop.getClass() == String.class) {
        usertaskCheckBuilder.check(
            "Select execution schedule > recurrence > yes > Fill recurrence > Recurrence Stop > End by > date and time",
            recurrenceStop);
      } else {
        usertaskCheckBuilder.check(
            "Select execution schedule > recurrence > yes > Fill recurrence > Recurrence Stop > After number of occurences > occurrences",
            recurrenceStop);
      }
    } else {
      usertaskCheckBuilder.check(
          "Select Execution Schedule > recurrence > no > Apply Optimization Results Automatically > "
              + decision, true);
    }

    completeUsertaskReviewAndConfirm(flowExecution);
  }

  private void triggerUEMeasurements(FlowExecution flowExecution) {
    // Trigger UE Measurement
    String triggerUeMeasurementFlowStep = FlowTestUtils.getMessage("wf.execute.triggerUeMeasurement");
    completeUsertaskNoInput(flowExecution, "Visualize gNB to Trigger UE Measurement");
    checkExecutionEventIsRecorded(flowExecution, INFO,
            FlowTestUtils.getMessage("ncmp.status.connectionEstablished"), triggerUeMeasurementFlowStep);
  }

  private void optimizationResultsConfiguration(FlowExecution flowExecution, String decision) {
    completeUsertask(flowExecution, "Optimization Results Configuration",
        new UsertaskInputBuilder().input("Apply Optimization Results Automatically?", decision));
  }

  private void oneOptimizationLoop(FlowExecution flowExecution, int n) {
    String startOptimizationStep = FlowTestUtils.getMessage("wf.execute.startOptimization");
    String optimizationStep = FlowTestUtils.getMessage("wf.execute.optimization");
    String kpiFlowStep = FlowTestUtils.getMessage("wf.execute.getBaselineKpi");
    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + n
        + "; ";
    String optimizationStatusSuccessfulStarted = FlowTestUtils.getMessage("startOptimization.status.successfulStart");
    String optimizationStatusFinished = FlowTestUtils.getMessage("optimization.status.finished");
    // TODO consider checking this event when handling baselineKPI
    //checkExecutionEventIsRecorded(flowExecution, INFO, FlowTestUtils.getMessage("kpi.status.successfulRetrieveRequest"), kpiFlowStep);
    checkExecutionEventIsRecorded(flowExecution, INFO, occurrenceInfo + optimizationStatusSuccessfulStarted, startOptimizationStep);
    checkExecutionEventIsRecorded(flowExecution, INFO, occurrenceInfo + optimizationStatusFinished, optimizationStep);
  }

  private void oneOptimizationLoopNoRecurrence(FlowExecution flowExecution, String decision) {

    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 1
        + "; ";
    oneOptimizationLoop(flowExecution, 1);
    final String FLOW_STEP = FlowTestUtils.getMessage("wf.execute.deployment");
    if (decision == null) {
      checkExecutionEventIsRecorded(flowExecution, INFO,
          occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));
      checkExecutionEventIsRecorded(flowExecution, INFO,
          occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), FLOW_STEP);
      checkExecutionEventIsRecorded(flowExecution, INFO,
          occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"), FLOW_STEP);
    } else {
      confirmConfiguration(flowExecution, decision);
      if (decision.equals("Yes")) {
        checkExecutionEventIsRecorded(flowExecution, INFO,
            occurrenceInfo + FlowTestUtils.getMessage("wf.execute.acceptedConfiguration"));
        checkExecutionEventIsRecorded(flowExecution, INFO,
            occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), FLOW_STEP);
        checkExecutionEventIsRecorded(flowExecution, INFO,
            occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"),
            FLOW_STEP);
      } else {
        checkExecutionEventIsRecorded(flowExecution, INFO,
            occurrenceInfo + FlowTestUtils.getMessage("wf.execute.droppedConfiguration"));
      }
    }
    checkExecutionState(flowExecution, "COMPLETED");
  }

  private void oneOptimizationLoopRecurrence(FlowExecution flowExecution) {
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoop(flowExecution, 1);
    final String FLOW_STEP = FlowTestUtils.getMessage("wf.execute.deployment");
    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + 1
        + "; ";
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.applyingAutoConfiguration"));
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.sentConfiguration"), FLOW_STEP);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("wf.execute.receivedConfiguration"), FLOW_STEP);
    checkExecutionSummaryReport(flowExecution,
        FlowTestUtils.getMessage("applyConf.status.finished"));
    checkExecutionState(flowExecution, "COMPLETED");
  }

  private void confirmConfiguration(FlowExecution flowExecution, String decision) {
    completeUsertask(flowExecution, "Confirm Configuration",
        new UsertaskInputBuilder().input("Apply the new BB pairs links? > " + decision, true));
  }

  /*
   * This method generate JSON inputFile From interaction filled user task and save it on disk
   *
   *    Call this function if you want to generate the input file of a specific interactive scenario
   *    input : user task ( returned by checkUsertaskActive(..)  method ), the input builder , and the file name without .json extension
   *
   * NOTE : make sure to call this function just before calling completeUsertask() method and handle the exception throwed by this function
   */
  @SuppressWarnings("unused")
  private void generateJsonInputFile(UserTask userTask, UsertaskInputBuilder caseOfTest,
      String fileName) throws Exception {

    UserTaskSchema usertaskSchema = this.flowExecutionService.getUserTaskSchema(userTask.getId());
    Assert.assertNotNull(usertaskSchema);
    String userTaskInput = caseOfTest.buildInputString(usertaskSchema.getSchema());
    Map<String, byte[]> userTaskFileInput = caseOfTest.getFileInput();

    String selectedNodes =

        "{\n" + "  \"selectGNBDUs\" : {\n" + "    \"table\" : [ {\n"
            + "      \"gNBName\" : \"Europe/Ireland/NR04gNodeBRadio00023/NR04gNodeBRadio00023/1\",\n"
            + "      \"gNBId\" : 279\n" + "    }, {\n"
            + "      \"gNBName\" : \"Europe/Ireland/NR01gNodeBRadio00002/NR01gNodeBRadio00002/1\",\n"
            + "      \"gNBId\" : 289\n" + "    } ]\n" + "  },";

    // Change The path to wherever you want BUT don't forget to move the saved files to the resources folder
    String path = "c:/" + fileName + ".json";

    String jsonContent = selectedNodes + userTaskInput.substring(1);
    Files.write(Paths.get(path), jsonContent.getBytes());
  }
}