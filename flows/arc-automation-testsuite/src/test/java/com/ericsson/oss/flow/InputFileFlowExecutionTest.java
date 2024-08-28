/*
 * ------------------------------------------------------------------------------
 *  *******************************************************************************
 *  * COPYRIGHT Ericsson 2020
 *  *
 *  * The copyright to the computer program(s) herein is the property of
 *  * Ericsson Inc. The programs may be used and/or copied only with written
 *  * permission from Ericsson Inc. or in accordance with the terms and
 *  * conditions stipulated in the agreement/contract under which the
 *  * program(s) have been supplied.
 *  *******************************************************************************
 *  *----------------------------------------------------------------------------
 */
package com.ericsson.oss.flow;

import com.ericsson.oss.flow.util.FlowTestUtils;
import com.ericsson.oss.flow.util.ServiceMockUtils;
import com.ericsson.oss.services.flowautomation.model.FlowDefinition;
import com.ericsson.oss.services.flowautomation.model.FlowExecution;
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationRestBaseTest;
import com.ericsson.oss.services.flowautomation.test.fwk.UsertaskInputBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity.ERROR;
import static com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity.INFO;
import static com.ericsson.oss.services.flowautomation.test.fwk.TestUtils.getFlowPackageBytes;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.equalTo;


/**
 * Test cases for Flow Automation flow using a simple DSL.
 */
public class InputFileFlowExecutionTest extends FlowAutomationRestBaseTest {

  private final String FLOW_ID = "com.ericsson.oss.flow.arc-automation";
  private final String VALIDATE_FILE_INPUT_FLOW_STEP = FlowTestUtils.getMessage("wf.setup.validateSetupFileInput");
  private static final boolean isOnline;
  private static final ServiceMockUtils serviceMockUtils;

  static {
    try {
      InputStream is = InputFileFlowExecutionTest.class.getResourceAsStream("config.properties");
      Properties configProperties = new Properties();
      configProperties.load(is);
      isOnline = Boolean.parseBoolean(configProperties.getProperty("onlineTesting"));
      serviceMockUtils = isOnline ? null : new ServiceMockUtils();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  FlowDefinition flowDefinition;


  @Before
  public void before() {
    String flowPackage = "arc-automation-flow";
    flowDefinition = importFlow(FLOW_ID, getFlowPackageBytes(flowPackage));
    FlowTestUtils.setRoutes(isOnline, wireMockRule);
    if (!isOnline) {
      setMockedServices();
    }
  }

  @After
  public void after() {
    removeFlow(FLOW_ID);
  }

  // This test verifies that when the user provides GNBDUs that are not existing (referring to the CTS) its caught successfully
  @Test
  public void gNBsNotAvailable() throws IOException {
    testFlowSetupPhaseWithFileInput("gNBsNotAvailable.json",
        "Invalid GNBDUs table: {\"gNBName\": Kisqsdta_2, \"gNBId\": 20541398729} Not an available node.",
        VALIDATE_FILE_INPUT_FLOW_STEP);
  }

  /**
   * This section includes only FAILING tests for the "selectScheduleUserTask" parameters so for every task in
   * this section is with "selectGNBDUs" : { "table" : [ { "gNBName" : "Kista_1","gNBId" : 270 },
   * { "gNBName" : "Kista_2","gNBId" : 279 } ]    }
   * How tests are named : "name_of_variable"+business_error, these tests ensure that variables respect
   * business constraints.
   */

  @Test
  public void immedAfterOccurrencesEMOverLimit() throws Exception {

    // Defining the Input file for this test
    String inputFileName = "immedAfterOccurrencesEMOverLimit.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateEMExplication");
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    checkExecutionEventIsRecorded(flowExecution, ERROR, errorMessage, flowStep);
  }

  @Test
  public void hoursLaterAfterOccurrencesHourlyOverLimit() throws Exception {

    // Defining the Input file for this test
    String inputFileName = "hoursLaterAfterOccurrencesHourlyOverLimit.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateHourlyExplication");
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    checkExecutionEventIsRecorded(flowExecution, ERROR, errorMessage, flowStep);
  }

  @Test
  public void hoursLaterEndDateDailyOverLimit() throws Exception {

    // Defining the Input file for this test
    String inputFileName = "hoursLaterEndDateDailyOverLimit.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusDays(14).plusHours(73)
        .toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate");
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    checkExecutionEventIsRecorded(flowExecution, ERROR, errorMessage, flowStep);
  }

  @Test
  public void startDateAfterOccurrencesDailyOverLimit() throws Exception {

    // Defining the Input file for this test
    String inputFileName = "startDateAfterOccurrencesDailyOverLimit.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusDays(5).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    String errorMessage = FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDate") + ": " + FlowTestUtils.getMessage(
        "scheduler.error.endDateAfterTwoWeeksFromStartDateDailyExplication");
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    checkExecutionEventIsRecorded(flowExecution, ERROR, errorMessage, flowStep);
  }

  @Test
  public void startDateInThePast() throws IOException {
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    testFlowSetupPhaseWithFileInput("startDateInThePast.json",
        FlowTestUtils.getMessage("scheduler.error.startDateInPast"), flowStep);
  }

  @Test
  public void startDateAfterOneMonthError() throws Exception {

    // Defining the Input file for this test
    String inputFileName = "startDateAfterOneMonthError.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startDate = Instant.now().atZone(ZoneId.systemDefault()).plusMonths(2).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    checkExecutionEventIsRecorded(flowExecution, ERROR,
        FlowTestUtils.getMessage("scheduler.error.startDateMaxDelayExceeded"), flowStep);
  }

  @Test
  public void endDateBeforeStartDate() throws IOException {
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    testFlowSetupPhaseWithFileInput("endDateBeforeStartDate.json",
        FlowTestUtils.getMessage("scheduler.error.endDateBeforeStartDate"), flowStep);
  }

  @Test
  public void noEndDateNotSupportedYet() throws IOException {
    String flowStep = FlowTestUtils.getMessage("wf.setup.validateExecutionSchedule");
    testFlowSetupPhaseWithFileInput("noEndDateNotSupportedYet.json",
        FlowTestUtils.getMessage("scheduler.error.unsupportedNoEndDate"), flowStep);
  }

  /**
   * This section includes only SUCCESSFUL tests for the "selectScheduleUserTask" parameters so for every task
   * in this section is with "selectGNBDUs" : { "table" : [ { "gNBName" : "Kista_1","gNBId" : 270 },
   * { "gNBName" : "Kista_2","gNBId" : 279 } ]    }
   * How tests are named:
   * - We start with "startTime" property, and it can be : "immed", for immediate.
   * "hL"+integer, for Hours Later with the number of hours.
   * "SD"+month_integer+day_integer with leading zeroes if
   * needed, for a specified date.
   * - Then : if without recurrence we add "NoRecurrence".
   * if with recurrence we first add the recurrence pattern, second we add the recurrence stop.
   * <p>
   * * Recurrence patterns : "Daily" for daily
   * "Hourly" for hourly
   * "EM" for everyMinute
   * * Recurrence stop : "EndDate"+month_integer+day_integer with leading zeroes if needed for a specified date.
   * "Occurrences"+integer, for occurrences case with its number.
   * "NoEnd" for the noEndDate option. (not supported currently by our flow)
   */

  @Test
  public void immedNoRecurrenceApplyResultsAutomatically() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedNoRecurrenceApplyResultsAutomatically.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);
  }



  @Test
  public void immedNoRecurrenceApplyResultsAutomaticallyWithUnavailableGnbs() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedNoRecurrenceApplyResultsAutomaticallyWithUnavailableGnbs.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */


    triggerUEMeasurementsWithUnavailableGnbs(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);
  }



@Test
  public void immedNoRecurrenceConfirmConfigurationYes() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedNoRecurrenceConfirmConfiguration.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "Yes");
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedNoRecurrenceConfirmConfigurationNo() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedNoRecurrenceConfirmConfiguration.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "No");
  }

  @Test
  public void immedAfterOccurrencesEM() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedAfterOccurrencesEM.json";

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void immedEndDateEM() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedEndDateEM.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(20).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);
    //Check execution report
    checkExecutionReport(flowExecution);
  }

  @Test
  public void immedEndDateHourly() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedEndDateHourly.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusMinutes(20).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void immedEndDateDaily() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedEndDateDaily.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusHours(23).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void immedEndDateReachedBeforeOptimization() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "immedEndDateReachedBeforeOptimization.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void startDateNoRecurrenceApplyResultsAutomatically() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateNoRecurrenceApplyResultsAutomatically.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);
  }

  @Test
  public void startDateNoRecurrenceConfirmConfigurationYes() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateNoRecurrenceConfirmConfiguration.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "Yes");
    checkExecutionReport(flowExecution);
  }

  @Test
  public void startDateNoRecurrenceConfirmConfigurationNo() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateNoRecurrenceConfirmConfiguration.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    triggerUEMeasurements(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, "No");
  }

  @Test
  public void startDateAfterOccurrencesEM() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateAfterOccurrencesEM.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void startDateEndDateEM() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateEndDateEM.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(20).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    /*
     * Execute phase
     */
    oneOptimizationLoopRecurrence(flowExecution);
  }

  @Test
  public void startDateEndDateHourly() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateEndDateHourly.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusMinutes(20).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void startDateEndDateDaily() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateEndDateDaily.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusHours(23).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void startDateEndDateReachedBeforeOptimization() throws Exception {
    // Defining the Input file for this test
    String inputFileName = "startDateEndDateReachedBeforeOptimization.json";

    // Update endDate based on execution time
    JSONObject inputFileJSONObject = getJSONObjectFromFile(inputFileName);
    Instant startTime = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(5).toInstant();
    updateJSONObjectStartDate(inputFileJSONObject, startTime);
    Instant endDate = Instant.now().atZone(ZoneId.systemDefault()).plusSeconds(10).toInstant();
    updateJSONObjectEndDate(inputFileJSONObject, endDate);

    // Updating changes to the json input file
    writeStringToFile(inputFileName, inputFileJSONObject.toString());

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

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
  public void testGivenInputFileWithSelectedGNodeBAndConstraintsThenCallExecutionSchedule() throws IOException {
    String inputFileName = "selectedGNodeBAndConstraintsWithoutScheduleInput.json";
    FlowExecution flowExecution = launchExecution(inputFileName);
    checkExecutionIsActive(flowExecution);
    checkExecutionEventIsRecorded(flowExecution, INFO, FlowTestUtils.getMessage("cts.status.connectionEstablished"), VALIDATE_FILE_INPUT_FLOW_STEP);
    checkUsertaskActive(flowExecution, "Select Execution Schedule");
    UsertaskInputBuilder userTaskInput = new UsertaskInputBuilder();
    userTaskInput.input("Select Execution Schedule > Start time > Immediately", true)
                 .input("Select Execution Schedule > Show baseline KPI > No", true)
                 .input("Select Execution Schedule > recurrence > no > Apply Optimization Results Automatically > Yes", true);
    completeUsertask(flowExecution, "Select Execution Schedule", userTaskInput);
    triggerUEMeasurementsWithUnavailableGnbs(flowExecution);
    oneOptimizationLoopNoRecurrence(flowExecution, null);
    checkExecutionReport(flowExecution);

  }

  /*
   * End of success scenarios
   */

  private void setMockedServices() {
    serviceMockUtils.mockCtsAvailableGnbdus(wireMockServerSimulator);
    serviceMockUtils.mockUeMeasurementsWithUnavailableGnbdu(wireMockServerSimulator);
    serviceMockUtils.mockUeMeasurements(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationCreate(wireMockServerSimulator);
    serviceMockUtils.mockTriggerKpi(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationStart(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationStatus(wireMockServerSimulator);
    serviceMockUtils.mockPartnerConfiguration(wireMockServerSimulator);
    serviceMockUtils.mockOptimizationFinish(wireMockServerSimulator);
  }

  private void testFlowSetupPhaseWithFileInput(String inputFileName, String message,
      String flowStep) throws IOException {

    // Start the flow
    FlowExecution flowExecution = launchExecution(inputFileName);

    // Check if execution activated
    checkExecutionIsActive(flowExecution);

    // Check the Error Event caused by this file
    checkExecutionEventIsRecorded(flowExecution, ERROR, message, flowStep);
  }

  private FlowExecution launchExecution(String fileName) throws IOException {

    InputStream is = getClass().getResourceAsStream(fileName);

    // prepare file in byte[]
    assert is != null;
    byte[] firstFileInBytes = new byte[is.available()];
    is.read(firstFileInBytes);

    // Start the flow
    return startFlowExecutionWithFile(flowDefinition, createUniqueInstanceName(FLOW_ID), fileName,
        firstFileInBytes);
  }

  private void triggerUEMeasurements(FlowExecution flowExecution) {
    int[] enabledGNBDUlist = new int[2];
    enabledGNBDUlist[0] = 270;
    enabledGNBDUlist[1] = 279;
    // Trigger UE Measurement
    completeUsertask(flowExecution, "Visualize gNB to Trigger UE Measurement",
        new UsertaskInputBuilder().input("Trigger UE Measurement > List of Enabled gNB",
            enabledGNBDUlist));
  }

  private void triggerUEMeasurementsWithUnavailableGnbs(FlowExecution flowExecution) {
    int[] enabledGNBDUlist = new int[2];
    enabledGNBDUlist[0] = 270;
    enabledGNBDUlist[1] = 279;
    int[] unavailableGnbduList = new int[1];
    unavailableGnbduList[0] = 1503;
    // Trigger UE Measurement
    checkUsertaskActive(flowExecution, "Visualize gNB to Trigger UE Measurement");
    completeUsertask(flowExecution, "Visualize gNB to Trigger UE Measurement",
        new UsertaskInputBuilder().input("Trigger UE Measurement > List of Enabled gNB",
            enabledGNBDUlist)
            .input("Trigger UE Measurement > List of Not found gNB", unavailableGnbduList));
  }

  private void oneOptimizationLoop(FlowExecution flowExecution, int n) {
    String optimizationIdStep = FlowTestUtils.getMessage("wf.execute.optimizationId");
    String startOptimizationStep = FlowTestUtils.getMessage("wf.execute.startOptimization");
    String optimizationStep = FlowTestUtils.getMessage("wf.execute.optimization");
    String occurrenceInfo = FlowTestUtils.getMessage("optimization.occurrenceNumber") + ": " + n
        + "; ";
    // TODO consider checking this event when handling baselineKPI
    // checkExecutionEventIsRecorded(flowExecution, INFO, FlowTestUtils.getMessage("kpi.status.successfulRetrieveRequest"), kpiFlowStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        FlowTestUtils.getMessage("optimization.id.successful"), optimizationIdStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("startOptimization.status.successfulStart"),
        startOptimizationStep);
    checkExecutionEventIsRecorded(flowExecution, INFO,
        occurrenceInfo + FlowTestUtils.getMessage("optimization.status.finished"),
        optimizationStep);
  }

  private void optimizationResultsConfiguration(FlowExecution flowExecution, String decision) {
    completeUsertask(flowExecution, "Optimization Results Configuration",
        new UsertaskInputBuilder().input("Apply Optimization Results Automatically?", decision));
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


  private void confirmConfiguration(FlowExecution flowExecution, String decision) {
    completeUsertask(flowExecution, "Confirm Configuration",
        new UsertaskInputBuilder().input("Apply the new BB pairs links? > " + decision, true));
  }

  private JSONObject getJSONObjectFromFile(String fileName) throws JSONException, IOException {

    @SuppressWarnings("IOStreamConstructor") InputStream inputStream = new FileInputStream(
        Objects.requireNonNull(getClass().getResource(fileName)).getFile());
    String jsonTxt = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    return new JSONObject(jsonTxt);
  }

  // get a file from the resources folder
  // works everywhere, IDEA, unit test and JAR file.
  @SuppressWarnings("unused")
  private InputStream getFileFromResourceAsStream(String fileName) {

    // The class loader that loaded the class
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    // the stream holding the file content
    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      return inputStream;
    }
  }

  private void writeStringToFile(String fileName, String data) {
    File file = new File(Objects.requireNonNull(getClass().getResource(fileName)).getFile());
    try {
      FileUtils.writeStringToFile(file, data, (String) null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateJSONObjectEndDate(JSONObject inputFileJSONObject, Instant endDate)
      throws JSONException {
    inputFileJSONObject.getJSONObject("selectScheduleUserTask").getJSONObject("recurrence")
        .getJSONObject("withRecurrence").getJSONObject("selectRecurrencePreference").getJSONObject(
            "recurrenceStop").getJSONObject("endDate").put("specifyDate", endDate.toString());
  }

  private void updateJSONObjectStartDate(JSONObject inputFileJSONObject, Instant startDate)
      throws JSONException {
    inputFileJSONObject.getJSONObject("selectScheduleUserTask").getJSONObject("startTime")
        .getJSONObject("specifyDate").put("specifyDate", startDate.toString());
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
}
