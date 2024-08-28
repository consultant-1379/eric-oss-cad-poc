// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.transform.Field
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
final String FLOW_STEP = "Validate gNodeB pair exclusion"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

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

unwantedGNodeBPairsSelection = execution.getVariable("unwantedGNodeBPairsSelection") as Map
List<Map<String, Object>> excludedPairs = execution.getVariable("excludedPairs") as List
primaryGNBDU = unwantedGNodeBPairsSelection.primaryGNBDUExclusion as List
secondaryGNBDU = unwantedGNodeBPairsSelection.neighbouring as List
flowInput?.unwantedGNBDUs = secondaryGNBDU
n = ((flowInput.selectGNBDUs as Map).table as List).size()
primaryGNBDU.forEach({
  pGnb ->
    secondaryGNBDU.forEach({
      sGnb ->
        if (pGnb == sGnb) {
          throw new UsertaskInputProcessingError(
              flowAutomationErrorCodes.ERROR_USER_TASK_INVALID_INPUT,
              arcFlowUtils.getMessage(execution, "unwantedGNodeBPairValidation.repeatedGNodeB") +
                  " gNodeB ID : " + pGnb.gNBId.toString() + " ; gNodeB Name : " +
                  pGnb.gNBName.toString()
          )
        }
        Map<String, Object> newLink = ["pGnbduId"  : pGnb["gNBId"],
                                       "pGnbduName": pGnb["gNBName"],
                                       "sGnbduId"  : sGnb["SecondarygNBId"],
                                       "sGnbduName": sGnb["SecondarygNBName"]]
        if (excludedPairs.contains(newLink)) {
          excludedPairs.remove(newLink)
        } else {
          excludedPairs.add("pGnbduId": pGnb["gNBId"],
              "pGnbduName": pGnb["gNBName"],
              "sGnbduId"  : sGnb["SecondarygNBId"],
              "sGnbduName": sGnb["SecondarygNBName"])
        }
        if (
        excludedPairs.stream().filter({ excluPair -> excluPair.containsValue(newLink["pGnbduId"]) })
            .count() == 2 * n - 2) {
          EventRecorder.warn(execution,
              arcFlowUtils.getMessage(execution, "unwantedGNodeBPairValidation.excludedGNodeB") +
                  " gNodeB ID : " + newLink.pGnbduId.toString() + " ; gNodeB Name : " +
                  newLink.pGnbduName.toString())
        }
        if (
        excludedPairs.stream().filter({ excluPair -> excluPair.containsValue(newLink["sGnbduId"]) })
            .count() == 2 * n - 2) {
          EventRecorder.warn(execution,
              arcFlowUtils.getMessage(execution, "unwantedGNodeBPairValidation.excludedGNodeB") +
                  " gNodeB ID : " + newLink.sGnbduId.toString() + " ; gNodeB Name : " +
                  newLink.sGnbduName.toString())
        }
    })
})
execution.setVariable("excludedPairs", excludedPairs)


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
