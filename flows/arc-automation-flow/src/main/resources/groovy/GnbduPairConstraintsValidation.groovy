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
import com.ericsson.oss.services.flowautomation.flowapi.Reporter
import com.ericsson.oss.services.flowautomation.flowapi.usertask.UsertaskInputProcessingError

@Field
final String FLOW_STEP = "Validate execution schedule"
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
Class arcFlowUtils

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



Reporter.updateReportSummary(execution, FLOW_STEP)
List<Map<String, Object>> mandatoryLinks = (flowInput.userGNBDUPartnerConstraints.
    mandatoryGnbduLinks as Map).mandatoryGNodeBPairsTable as List
List<Map<String, Object>> excludedLinksTable = (flowInput.userGNBDUPartnerConstraints.
    excludedGnbduLinks as Map).excludedGNodeBPairsTable as List
LOGGER.info(arcFlowUtils.getMessage(execution,"wf.setup.validatePairConstraints"))
validateUserConstraints(mandatoryLinks, excludedLinksTable)

void validateUserConstraints(List<Map<String, Object>> mandatoryLinks,
    List<Map<String, Object>> excludedLinksTable) throws BpmnError {
  List<Map<String, Object>> invalidExcludedLinks =
      excludedLinksTable.stream().filter({ partnerPair ->
        return mandatoryLinks.contains(partnerPair)
      }).collect(Collectors.toList())

  if (!invalidExcludedLinks.isEmpty()) {
    errorMessageValidation("${arcFlowUtils.getMessage(execution, "selectGnbdus.error.gNBExcludedAndMandatory")}: $invalidExcludedLinks")
  }
}

private errorMessageValidation(String message) {
  throw new UsertaskInputProcessingError(flowAutomationErrorCodes.ERROR_USER_TASK_INVALID_INPUT, message)
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

