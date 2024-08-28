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
import com.ericsson.oss.services.flowautomation.test.fwk.FlowAutomationScriptBaseTest
import static org.junit.Assert.assertEquals

class ValidateMandatoryGNodeBPairSelectionTest extends FlowAutomationScriptBaseTest {

  List mandatoryPairs
  List excludedPairs
  Map<String, List> mandatoryGNodeBPairsSelection

  @Rule
  public ExpectedException thrown = ExpectedException.none()

  @Before
  void init() throws IOException {
    mandatoryGNodeBPairsSelection = ["primaryGNBDUMandatory": [] as List, "secondaryGNBDUMandatory": [] as List ]
    mandatoryPairs = [] as List
    excludedPairs = [] as List

    mandatoryPairs.add("pGnbduId"  : 208728,
        "pGnbduName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
        "sGnbduId"  : 208730,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    mandatoryPairs.add("pGnbduId"  : 208728,
        "pGnbduName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
        "sGnbduId"  : 208731,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    excludedPairs.add("pGnbduId"  : 208727,
        "pGnbduName": "tc11-vdu/1",
        "sGnbduId"  : 208730,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    excludedPairs.add("pGnbduId"  : 208727,
        "pGnbduName": "tc11-vdu/1",
        "sGnbduId"  : 208731,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    delegateExecution.setVariable("mandatoryPairs", mandatoryPairs)
    delegateExecution.setVariable("excludedPairs", excludedPairs)
  }


  @Test
  @Ignore
  void successMultipleSelection() {
    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")
    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208731,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1")

    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208727,
        "gNBName": "tc11-vdu/1")
    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)


    List<?> expectedMandatoryPairs = mandatoryPairs
    expectedMandatoryPairs.add("pGnbduId"  : 208730,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
        "sGnbduId"  : 208727,
        "sGnbduName": "tc11-vdu/1")
    expectedMandatoryPairs.add("pGnbduId"  : 208730,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1",
        "sGnbduId"  : 208728,
        "sGnbduName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")
    expectedMandatoryPairs.add("pGnbduId"  : 208731,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
        "sGnbduId"  : 208727,
        "sGnbduName": "tc11-vdu/1")
    expectedMandatoryPairs.add("pGnbduId"  : 208731,
        "pGnbduName": "Europe/Ireland/NR01gNodeBRadio00005/NR01gNodeBRadio00005/1",
        "sGnbduId"  : 208728,
        "sGnbduName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(expectedMandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(true,delegateExecution.getVariable("newConstraintProvided"))

  }

  @Test
  @Ignore
  void successRemovePair() {

    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)

    List<?> expectedMandatoryPairs = mandatoryPairs
    expectedMandatoryPairs.remove("pGnbduId"  : 208728,
        "pGnbduName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1",
        "sGnbduId"  : 208730,
        "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(expectedMandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(true,delegateExecution.getVariable("newConstraintProvided"))
  }

  @Test
  @Ignore
  void pairAlreadyExcluded() {

    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208727,
        "gNBName": "tc11-vdu/1")

    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208730,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1")

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)

    List<?> expectedMandatoryPairs = mandatoryPairs
    Map<String, Object> newLink = ["pGnbduId"  : 208727,
                                   "pGnbduName": "tc11-vdu/1",
                                   "sGnbduId"  : 208730,
                                   "sGnbduName": "Europe/Ireland/NR01gNodeBRadio00003/NR01gNodeBRadio00003/1"]

    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "mandatoryGNodeBPairValidation.pairAlreadyExcluded") + newLink.toString())

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(expectedMandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(true,delegateExecution.getVariable("newConstraintProvided"))

  }

  @Test
  @Ignore
  void mandatoryLimitReached() {

    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208733,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00001/NR01gNodeBRadio00001/1")
    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208735,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00008/NR01gNodeBRadio00008/1")
    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208736,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00006/NR01gNodeBRadio00006/1")
    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208737,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00012/NR01gNodeBRadio00012/1")
    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208746,
        "gNBName": "Europe/Ireland/NR01gNodeBRadio00011/NR01gNodeBRadio00011/1")

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)

    List<?> expectedMandatoryPairs = mandatoryPairs

    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "mandatoryGNodeBPairValidation.tooManyMandatory") +
            " For the gNodeB ID : " + "208728" + " ; gNodeB Name : " + "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(expectedMandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(true,delegateExecution.getVariable("newConstraintProvided"))

  }

  @Test
  @Ignore
  void forbiddenEntry() {

    mandatoryGNodeBPairsSelection.primaryGNBDUMandatory.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    mandatoryGNodeBPairsSelection.secondaryGNBDUMandatory.add("gNBId": 208728,
        "gNBName": "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)

    List<?> expectedMandatoryPairs = mandatoryPairs

    thrown.expect(UsertaskInputProcessingError.class)
    thrown.expectMessage(
        ArcFlowUtils.getMessage(delegateExecution, "mandatoryGNodeBPairValidation.repeatedGNodeB") +
            " gNodeB ID : " + "208728" + " ; gNodeB Name : " + "Europe/Ireland/NETSimW/NR02gNodeBRadio00005/1")

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(expectedMandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(true,delegateExecution.getVariable("newConstraintProvided"))
  }

  @Test
  @Ignore
  void noInputContinue() {

    delegateExecution.setVariable("mandatoryGNodeBPairsSelection", mandatoryGNodeBPairsSelection)

    runFlowScript(delegateExecution, "groovy/ValidateMandatoryGNodeBPairSelection.groovy")
    assertEquals(mandatoryPairs, delegateExecution.getVariable("mandatoryPairs"))
    assertEquals(false,delegateExecution.getVariable("newConstraintProvided"))

  }
}