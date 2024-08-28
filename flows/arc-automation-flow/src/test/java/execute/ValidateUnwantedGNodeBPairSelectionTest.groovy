// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import groovy.utils.ArcFlowUtils

import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import com.ericsson.oss.services.flowautomation.flowapi.usertask.UsertaskInputProcessingError
import com.ericsson.oss.services.flowautomation.model.FlowExecutionEventSeverity
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest

import static org.junit.Assert.assertEquals

class ValidateUnwantedGNodeBPairSelectionTest extends FlowAutomationScriptBaseTest {

  List excludedPairs
  Map<String, Map<String, List>> flowInput
  Map<String, List> unwantedGNodeBPairsSelection

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Before
  void init() throws IOException {
    unwantedGNodeBPairsSelection =
        ["primaryGNBDUExclusion": [] as List, "secondaryGNBDUExclusion": [] as List]
    flowInput = ["selectGNBDUs": ["table": [] as List]]
    excludedPairs = [] as List

    flowInput.selectGNBDUs.table.add("gNBId": 208727,
        "gNBName": "tc11-vdu/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208731,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208733,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208735,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208736,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00006/NR01gNodeBRadio00006/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208737,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00012/NR01gNodeBRadio00012/1")
    flowInput.selectGNBDUs.table.add("gNBId": 208746,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00011/NR01gNodeBRadio00011/1")

    excludedPairs.add("pGnbduId": 208727,
        "pGnbduName": "tc11-vdu/1",
        "sGnbduId": 208730,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    excludedPairs.add("pGnbduId": 208727,
        "pGnbduName": "tc11-vdu/1",
        "sGnbduId": 208731,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    delegateExecution.setVariable("flowInput", flowInput)
    delegateExecution.setVariable("excludedPairs", excludedPairs)
  }


  @Test
  @Ignore
  void successMultipleSelection() {
    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208731,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    unwantedGNodeBPairsSelection.secondaryGNBDUExclusion.add("gNBId": 208733,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    unwantedGNodeBPairsSelection.secondaryGNBDUExclusion.add("gNBId": 208735,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")

    delegateExecution.setVariable("unwantedGNodeBPairsSelection", unwantedGNodeBPairsSelection)

    List<?> expectedExcludedPairs = [] as List
    excludedPairs.forEach({excluPair -> expectedExcludedPairs.add(excluPair)})
    expectedExcludedPairs.add("pGnbduId": 208730,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
        "sGnbduId": 208733,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    expectedExcludedPairs.add("pGnbduId": 208730,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
        "sGnbduId": 208735,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")
    expectedExcludedPairs.add("pGnbduId": 208731,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
        "sGnbduId": 208733,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    expectedExcludedPairs.add("pGnbduId": 208731,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
        "sGnbduId": 208735,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")

    runFlowScript(delegateExecution, "groovy/ValidateUnwantedGNodeBPairSelection.groovy")
    assertEquals(expectedExcludedPairs, delegateExecution.getVariable("excludedPairs"))
    assertEquals(true, delegateExecution.getVariable("newConstraintProvided"))
  }

  @Test
  @Ignore
  void successRemovePair() {

    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208727,
        "gNBName": "tc11-vdu/1")

    unwantedGNodeBPairsSelection.secondaryGNBDUExclusion.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")

    delegateExecution.setVariable("unwantedGNodeBPairsSelection", unwantedGNodeBPairsSelection)

    List<?> expectedExcludedPairs = [] as List
    excludedPairs.forEach({excluPair -> expectedExcludedPairs.add(excluPair)})
    expectedExcludedPairs.remove("pGnbduId": 208727,
        "pGnbduName": "tc11-vdu/1",
        "sGnbduId": 208730,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")

    runFlowScript(delegateExecution, "groovy/ValidateUnwantedGNodeBPairSelection.groovy")
    assertEquals(expectedExcludedPairs, delegateExecution.getVariable("excludedPairs"))
    assertEquals(true, delegateExecution.getVariable("newConstraintProvided"))
  }

  @Test
  @Ignore
  void gNodeBAllLinksExcluded() {
    flowInput.selectGNBDUs.table.remove("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")
    flowInput.selectGNBDUs.table.remove("gNBId": 208733,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    flowInput.selectGNBDUs.table.remove("gNBId": 208735,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")
    flowInput.selectGNBDUs.table.remove("gNBId": 208736,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00006/NR01gNodeBRadio00006/1")
    flowInput.selectGNBDUs.table.remove("gNBId": 208737,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00012/NR01gNodeBRadio00012/1")
    flowInput.selectGNBDUs.table.remove("gNBId": 208746,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00011/NR01gNodeBRadio00011/1")
    delegateExecution.setVariable("flowInput", flowInput)

    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208731,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    unwantedGNodeBPairsSelection.secondaryGNBDUExclusion.add("gNBId": 208727,
        "gNBName": "tc11-vdu/1")

    delegateExecution.setVariable("unwantedGNodeBPairsSelection", unwantedGNodeBPairsSelection)

    List<?> expectedExcludedPairs = [] as List
    excludedPairs.forEach({excluPair -> expectedExcludedPairs.add(excluPair)})
    expectedExcludedPairs.add("pGnbduId": 208730,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
        "sGnbduId": 208727,
        "sGnbduName": "tc11-vdu/1")
    expectedExcludedPairs.add("pGnbduId": 208731,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
        "sGnbduId": 208727,
        "sGnbduName": "tc11-vdu/1")

    runFlowScript(delegateExecution, "groovy/ValidateUnwantedGNodeBPairSelection.groovy")
    assertEquals(expectedExcludedPairs, delegateExecution.getVariable("excludedPairs"))
    assertEquals(true, delegateExecution.getVariable("newConstraintProvided"))
    checkExecutionEventIsRecorded(flowExecution, FlowExecutionEventSeverity.WARNING,
        ArcFlowUtils.getMessage(delegateExecution, "unwantedGNodeBPairValidation.excludedGNodeB") +
            " gNodeB ID : " + "208727" + " ; gNodeB Name : " + "tc11-vdu/1")
  }

  @Test
  @Ignore
  void forbiddenEntry() {

    unwantedGNodeBPairsSelection.primaryGNBDUExclusion.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    unwantedGNodeBPairsSelection.secondaryGNBDUExclusion.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    delegateExecution.setVariable("unwantedGNodeBPairsSelection", unwantedGNodeBPairsSelection)

    List<?> expectedExcludedPairs = [] as List
    excludedPairs.forEach({excluPair -> expectedExcludedPairs.add(excluPair)})

    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "unwantedGNodeBPairValidation.repeatedGNodeB") +
            " gNodeB ID : " + "208728" + " ; gNodeB Name : " +
            "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    runFlowScript(delegateExecution, "groovy/ValidateUnwantedGNodeBPairSelection.groovy")
    assertEquals(expectedExcludedPairs, delegateExecution.getVariable("excludedPairs"))
    assertEquals(true, delegateExecution.getVariable("newConstraintProvided"))
  }

  @Test
  @Ignore
  void noInputContinue() {

    delegateExecution.setVariable("unwantedGNodeBPairsSelection", unwantedGNodeBPairsSelection)

    List<?> expectedExcludedPairs = [] as List
    excludedPairs.forEach({excluPair -> expectedExcludedPairs.add(excluPair)})

    runFlowScript(delegateExecution, "groovy/ValidateUnwantedGNodeBPairSelection.groovy")
    assertEquals(expectedExcludedPairs, delegateExecution.getVariable("excludedPairs"))
    assertEquals(false, delegateExecution.getVariable("newConstraintProvided"))
  }
}