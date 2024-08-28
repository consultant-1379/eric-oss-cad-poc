// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Links Deployment"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
String apacheHttpClientAgentClassContent, arcFlowUtilsClassContent, flowAutomationErrorCodesClassContent,
       jsonSchemaExceptionClassContent, jsonSchemaValidatorClassContent
try {
    apacheHttpClientAgentClassContent =
        getStringFromResourceFile(execution, "groovy/utils/ApacheHttpClientAgent.groovy")
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
Class flowAutomationErrorCodes, arcFlowUtils, apacheHttpClientAgent
try {
    flowAutomationErrorCodes = loader.parseClass(flowAutomationErrorCodesClassContent)
    loader.parseClass(jsonSchemaExceptionClassContent)
    loader.parseClass(jsonSchemaValidatorClassContent)
    arcFlowUtils = loader.parseClass(arcFlowUtilsClassContent)
    apacheHttpClientAgent = loader.parseClass(apacheHttpClientAgentClassContent)
} catch (CompilationFailedException compilationFailedException) {
    String errorMessage = "Failed to load class"
    LOGGER.error(errorMessage, compilationFailedException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Loading error"])
    throw new BpmnError("error.class_loader.exception",
        "${compilationFailedException.getMessage()}", compilationFailedException)
}

final String OCCURRENCE_INFO = execution.getVariable("occurrenceInfo")
final String MESSAGE_SENT = arcFlowUtils.getMessage(execution, "wf.execute.sentConfiguration")
final String MESSAGE_APPLIED = arcFlowUtils.getMessage(execution, "wf.execute.receivedConfiguration")

final String ARC_OPTIMIZATION_HOSt = "arcOptimizer.host"
final String CONFIGURATION_PATH = "arcOptimizerConfiguration.path"

final String OPTIMIZATION_CONFIGURATION_URL = System.getProperty("optimizationConfigurationUrl",
        arcFlowUtils.getUrlFromPropertiesFile(execution, ARC_OPTIMIZATION_HOSt, CONFIGURATION_PATH))

boolean configureBBpairs = execution.getVariable('ConfigureBBpairs.YesRadioButton')
try {
    List optimizationResult = execution.getVariable("optimizationResultTable") as List
    if (!optimizationResult.isEmpty()){
        String selectedNodes
        if (configureBBpairs) {
            List configureBBPairsUi = (execution.getVariable('table')) as List
            selectedNodes = arcFlowUtils.buildJsonResponse(configureBBPairsUi)
        } else {
            selectedNodes = arcFlowUtils.buildJsonResponse(optimizationResult)
        }

        Reporter.updateReportSummary(execution, MESSAGE_SENT)
        Object apacheHttpClient = apacheHttpClientAgent
            .initialize()
            .url(OPTIMIZATION_CONFIGURATION_URL)

        EventRecorder.info(execution, OCCURRENCE_INFO + MESSAGE_SENT as String, FLOW_STEP)

        apacheHttpClient.processPostRequest(selectedNodes)

        int responseCode = apacheHttpClient.getResponseCode()

        if (responseCode == HttpURLConnection.HTTP_OK) {
            EventRecorder.info(execution, OCCURRENCE_INFO + MESSAGE_APPLIED as String, FLOW_STEP)
        } else {
            throw new BpmnError(arcFlowUtils.getMessage(execution, "applyConf.error.unsuccessfulRequest"))
        }
    }
}catch(BpmnError bpmnError) {
    String errorMessage = arcFlowUtils.getMessage(execution, "applyConf.error.unsuccessfulRequest")
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": bpmnError.getMessage()])
    LOGGER.error(errorMessage, bpmnError)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, arcFlowUtils.getMessage(execution, "applyConf.error.unsuccessfulRequest"),
            bpmnError)
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