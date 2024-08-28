// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.json.JsonSlurper
import java.util.stream.Collectors

import org.apache.http.HttpStatus
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.codehaus.groovy.control.CompilationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.EventRecorder
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.ericsson.oss.services.flowautomation.flowapi.Reporter

final String FLOW_STEP = "Collect Secondary node"
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)


// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
String apacheHttpClientAgentClassContent, arcFlowUtilsClassContent, jsonSchemaLocationsClassContent,
       flowAutomationErrorCodesClassContent, jsonSchemaExceptionClassContent, jsonSchemaValidatorClassContent
try {
    apacheHttpClientAgentClassContent =
            getStringFromResourceFile(execution, "groovy/utils/ApacheHttpClientAgent.groovy")
    arcFlowUtilsClassContent =
            getStringFromResourceFile(execution, "groovy/utils/ArcFlowUtils.groovy")
    jsonSchemaLocationsClassContent =
            getStringFromResourceFile(execution, "groovy/constants/JsonSchemaLocations.groovy")
    flowAutomationErrorCodesClassContent =
            getStringFromResourceFile(execution, "groovy/constants/FlowAutomationErrorCodes.groovy")
    jsonSchemaExceptionClassContent =
            getStringFromResourceFile(execution, "groovy/exceptions/JsonSchemaException.groovy")
    jsonSchemaValidatorClassContent =
            getStringFromResourceFile(execution, "groovy/utils/JsonSchemaValidator.groovy")

} catch (NullPointerException nullPointerException) {
    String errorMessage = "Failed to retrieve class content"
    LOGGER.error(errorMessage, nullPointerException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Retrieval error"])
    throw new BpmnError("error.get_string_from_resource_file.exception",
            "${nullPointerException.getMessage()}", nullPointerException)
} catch (IOException ioException) {
    String errorMessage = "Failed to retrieve class content"
    LOGGER.error(errorMessage, ioException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Retrieval error"])
    throw new BpmnError("error.get_string_from_resource_file.exception",
            "${ioException.getMessage()}", ioException)
}

final String TOPOLOGY_HOST = "arcOptimizer.host"
final String NODE_RELATION = "cm.query.node.relations"

//Defining the local class loader
GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())

// Loading the explicit classes with the locally created class loader
Class flowAutomationErrorCodes, jsonSchemaLocations, arcFlowUtils, apacheHttpClientAgent
try {
    flowAutomationErrorCodes = loader.parseClass(flowAutomationErrorCodesClassContent)
    loader.parseClass(jsonSchemaExceptionClassContent)
    loader.parseClass(jsonSchemaValidatorClassContent)
    jsonSchemaLocations = loader.parseClass(jsonSchemaLocationsClassContent)
    arcFlowUtils = loader.parseClass(arcFlowUtilsClassContent)
    apacheHttpClientAgent = loader.parseClass(apacheHttpClientAgentClassContent)
} catch (CompilationFailedException compilationFailedException) {
    String errorMessage = "Failed to load class"
    LOGGER.error(errorMessage, compilationFailedException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP, ["Reason": "Class Loading error"])
    throw new BpmnError("error.class_loader.exception", "${compilationFailedException.getMessage()}",
            compilationFailedException)
}
String commonTopologyServiceUrl = System.getProperty("optimizationUrl",
        arcFlowUtils.getUrlFromPropertiesFile(execution, TOPOLOGY_HOST, NODE_RELATION))
Object apacheHttpClient =
        apacheHttpClientAgent.initialize().url(commonTopologyServiceUrl)

List<?> selectedgnblst = (flowInput?.selectGNBDUs as Map)?.table as List
List<?> neighbourGNBDUs = new ArrayList<>()
try {
    apacheHttpClient.processGetRequest()
} catch (BpmnError connectionException) {
    String errorMessage = arcFlowUtils.getMessage(execution, "cts.error.connectionFailed")
    LOGGER.error(errorMessage, connectionException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage, connectionException)
}
int ctsResponseCode = apacheHttpClient.getResponseCode()
if (ctsResponseCode == HttpStatus.SC_OK) {
    String responseBody = apacheHttpClient.getResponseBody()
    List<?> parsedResponse = new JsonSlurper().parseText(responseBody) as List
    for(gnb in selectedgnblst) {
        try {
            for (gnbitem in parsedResponse) {
                if(gnbitem.node.gNBId == gnb.gNBId) {
                    for (gnbneighbor in gnbitem.neighbors) {
                        neighbourGNBDUs.add("gNBId": gnbitem.node.gNBId,
                                "gNBName": gnbitem.node.name,
                                "gNBCmHandle": gnbitem.node.cmHandle,
                                "SecondarygNBId": gnbneighbor.gNBId,
                                "SecondarygNBName": gnbneighbor.name)
                    }
                }
            }
        }catch(Exception e){
            String errorMessage = "Query relationship failure"
            LOGGER.error(errorMessage, e)
        }
    }
}else{
    EventRecorder.info(execution,arcFlowUtils.getMessage(execution, "unable to fetch neighbour node"), FLOW_STEP)
}
List<?> uniqueneighbourGNBDUs =  neighbourGNBDUs.unique()
execution.setVariable("secGNBDUs", uniqueneighbourGNBDUs)
flowInput?.reportGNBDUs = uniqueneighbourGNBDUs
