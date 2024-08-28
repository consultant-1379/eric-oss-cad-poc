// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy

import groovy.json.JsonSlurper
import groovy.transform.Field
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

@Field
final String FLOW_STEP = "Validate setup input file"
@Field
final Logger LOGGER = LoggerFactory.getLogger(FLOW_STEP)

// Variables of type string containing the contents of the explicit classes defined in constants, exceptions and utils package
@Field
String apacheHttpClientAgentClassContent
@Field
String arcFlowUtilsClassContent
@Field
String jsonSchemaLocationsClassContent
@Field
String flowAutomationErrorCodesClassContent
@Field
String jsonSchemaExceptionClassContent
@Field
String jsonSchemaValidatorClassContent

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
Class jsonSchemaLocations
@Field
Class arcFlowUtils
@Field
Class apacheHttpClientAgent

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

final List<Map<String, Object>> gnbduTable = (flowInput.selectGNBDUs as Map).table as List

@Field
List<Map<String, Object>> gnbListTupleCts = new ArrayList<>()
@Field
List<Map<String, Object>> gnbListPairCts = new ArrayList<>()
@Field
List<?> gnbduList = new ArrayList<>()

Reporter.updateReportSummary(execution, FLOW_STEP)
LOGGER.info("Checking licence")
validateLicenceCheck()
LOGGER.info("Validating target gNBDUs")
retrieveCtsGnbdus()
validateSelectGNBDUs(gnbduTable)
boolean partnerConstraintsProvided = flowInput.containsKey("unwantedGNodeBPairsSelection") ||
    flowInput.containsKey("mandatoryGNodeBPairsSelection")
execution.setVariable("partnerConstraintsProvided", partnerConstraintsProvided)
execution.setVariable("scheduleProvided", flowInput.containsKey("selectScheduleUserTask"))

List<Map<String, Object>> excludedPairs = []
if (flowInput.containsKey("unwantedGNodeBPairsSelection")) {
  if (flowInput.unwantedGNodeBPairsSelection.containsKey("readOnlyTable")) {
    excludedPairs = flowInput.unwantedGNodeBPairsSelection.readOnlyTable as List
  }
  LOGGER.info("Validating Excluded Partner Links")
  excludedPairs = validateExcludedLinks(excludedPairs)
}

List<Map<String, Object>> mandatoryPairs = []
if (flowInput.containsKey("mandatoryGNodeBPairsSelection")) {
  if (flowInput.mandatoryGNodeBPairsSelection.containsKey("readOnlyTable")) {
    mandatoryPairs = flowInput.mandatoryGNodeBPairsSelection.readOnlyTable as List
  }
  LOGGER.info("Validating Mandatory Partner Links")
  mandatoryPairs = validateMandatoryLinks(mandatoryPairs)
}

validateUserConstraints(mandatoryPairs, excludedPairs)
execution.setVariable("excludedPairs", excludedPairs)
execution.setVariable("mandatoryPairs", mandatoryPairs)

private void validateLicenceCheck() {
  LOGGER.info("Nothing is being done for the moment.\n")
}

void retrieveCtsGnbdus() throws BpmnError {
  final String CTS_PROXY_HOST = "cts.proxy.host"
  final String GNBDU_LIST_PATH = "gnbduList.path"

  String commonTopologyServiceUrl = System.getProperty("commonTopologyServiceUrl",
      arcFlowUtils.getUrlFromPropertiesFile(execution, CTS_PROXY_HOST, GNBDU_LIST_PATH))
  Object apacheHttpClient = apacheHttpClientAgent
      .initialize()
      .url(commonTopologyServiceUrl)
  try {
    apacheHttpClient.processGetRequest()
  } catch (BpmnError connectionException) {
    String errorMessage = arcFlowUtils.getMessage(execution, "cts.error.connectionFailed")
    LOGGER.error(errorMessage, connectionException)
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage,
        connectionException)
  }
  int ctsResponseCode = apacheHttpClient.getResponseCode()
  if (ctsResponseCode == HttpStatus.SC_OK) {

    EventRecorder
        .info(execution, arcFlowUtils.getMessage(execution, "cts.status.connectionEstablished"),
            FLOW_STEP)
    try {
      List<Object> parsedResponse = new JsonSlurper()
          .parseText(apacheHttpClient.getResponseBody()) as List
      arcFlowUtils.validateSchema(execution, apacheHttpClient.getResponseBody(),
          jsonSchemaLocations.GNBDU_LIST_GET)

      LOGGER.info("Reading response data from Topology Service ..\n")
      parsedResponse.each { it ->
        int id = it?.id
        String name = it?.name
        String extId = (it?.externalId)
        String cmHandle = (extId.indexOf('/') > 0) ? extId.split("/")[0] : null
        if ((id && name && cmHandle) && (id > 0) &&
            (arcFlowUtils.isUniqueGnbdu(id, name, cmHandle, gnbListTupleCts))) {
          gnbListTupleCts.add("gNBName": name, "gNBId": id, "gNBCmHandle": cmHandle)
          gnbListPairCts.add("gNBName": name, "gNBId": id)
        }
      }
    } catch (IllegalArgumentException ex) {
      String errorMessage = "${arcFlowUtils.getMessage(execution, "request.error.responseParsing")}: ${ex.getMessage()}"
      LOGGER.error(errorMessage, ex)
      EventRecorder.error(execution, errorMessage, FLOW_STEP)
      throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, errorMessage, ex)
    } catch (BpmnError e) {
      String errorMessage = "${arcFlowUtils.getMessage(execution, "request.error.responseParsing")}: ${ex.getMessage()}"
      EventRecorder.error(execution, errorMessage, FLOW_STEP)
      throw e
    }
  } else {
    String errorMessage = arcFlowUtils.getMessage(execution, "cts.error.connectionFailed")
    LOGGER.error("$errorMessage: $ctsResponseCode")
    EventRecorder.error(execution, errorMessage, FLOW_STEP)
    throw new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION,
        "$errorMessage: $ctsResponseCode")
  }
}

private void validateSelectGNBDUs(List<Map<String, Object>> gnbduTable) throws BpmnError {
  Set<?> uniqueGndbus = new HashSet<>()
  Set<?> duplicateGnbdus = new HashSet<>()
  Set<?> gnbCtsIds =
      gnbListPairCts.stream().map { gnbdu -> gnbdu.gNBId }.collect(Collectors.toSet())

  for (Map<String, Object> gnbdu : gnbduTable) {
    Boolean isPair = (gnbdu.size() == 2)
    if (gnbCtsIds.contains(gnbdu.gNBId)) {
      Map<String, Object> node = (isPair) ?
          (gnbListTupleCts[gnbListPairCts.indexOf(gnbdu)] as Map) : gnbdu
      if (uniqueGndbus.add(node)) {
        gnbduList.add(node)
      } else {
        duplicateGnbdus.add(node)
      }
    } else {
      String msgP = $/Invalid GNBDUs table: {"gNBName": $gnbdu.gNBName,/$ +
          $/ "gNBId": $gnbdu.gNBId} Not an available node./$
      String msgT = $/Invalid GNBDUs table: {"gNBName": $gnbdu.gNBName,/$ +
          $/ "gNBId": $gnbdu.gNBId, "gNBCmHandle": $gnbdu.gNBCmHandle} Not an available node./$
      String msg = (isPair) ? msgP : msgT
      failValidation(msg, null)
    }
  }

  if (duplicateGnbdus.size() > 0) {
    EventRecorder
        .warn(execution, arcFlowUtils.getMessage(execution, "selectGnbdus.error.duplicateGnbdus"),
            FLOW_STEP, [duplicates: duplicateGnbdus])
  }
  if (gnbduList.size() < 2) {
    failValidation(arcFlowUtils.getMessage(execution, "selectGnbdus.error.gnbListSizeLessThan2"),
        null)
  }
  flowInput?.selectGNBDUs?.table = gnbduList
  execution.setVariable("gnbduList", gnbduList)
}

List<Map<String, Object>> validateExcludedLinks(List<Map<String, Object>> excludedLinksTable) {
  validateLinksAreFromSelectedGnbds(excludedLinksTable)
  int n = gnbduList.size()
  List<Map<String, Object>> verifiedExcludedLinks =[]
  gnbduList.forEach({ gNodeB ->
    if (
    excludedLinksTable.stream().filter({ excluPair -> excluPair.containsValue(gNodeB["gNBId"]) })
        .count() == 2 * n - 2) {
      EventRecorder.warn(execution,
          arcFlowUtils.getMessage(execution, "unwantedGNodeBPairValidation.excludedGNodeB") +
              " gNodeB ID : " + gNodeB["gNBId"].toString() + " ; gNodeB Name : " +
              gNodeB["gNBName"].toString())
    }
  })
  excludedLinksTable.forEach({samplePair ->
    if (verifiedExcludedLinks.stream().filter({excludedPair -> excludedPair.pGnbduId == samplePair.pGnbduId && excludedPair.sGnbduId == samplePair.sGnbduId}).count() == 0 ) {
      verifiedExcludedLinks.add(samplePair)
    }
  })
  flowInput?.unwantedGNodeBPairsSelection?.readOnlyTable = verifiedExcludedLinks
  return verifiedExcludedLinks
}

List<Map<String, Object>> validateMandatoryLinks(List<Map<String, Object>> mandatoryLinks) {
  validateLinksAreFromSelectedGnbds(mandatoryLinks)
  List<Map<String, Object>> verifiedMandatoryLinks =[]
  mandatoryLinks.forEach({ samplePair ->
    if (verifiedMandatoryLinks.stream().filter(
        { mandatoryPair -> mandatoryPair.pGnbduId == samplePair.pGnbduId }).count() >= 6) {
      failValidation(
          arcFlowUtils.getMessage(execution, "mandatoryGNodeBPairValidation.tooManyMandatory") +
              " For the gNodeB ID : " + samplePair.pGnbduId.toString() + " ; gNodeB Name : " +
              samplePair.pGnbduName.toString(),
          null)
    }
    if (verifiedMandatoryLinks.stream().filter(
        { mandatoryPair -> mandatoryPair.sGnbduId == samplePair.sGnbduId }).count() >= 6) {
      failValidation(
          arcFlowUtils.getMessage(execution, "mandatoryGNodeBPairValidation.tooManyMandatory") +
              " For the gNodeB ID : " + samplePair.sGnbduId.toString() + " ; gNodeB Name : " +
              samplePair.sGnbduName.toString(),
          null)
    }
    if (verifiedMandatoryLinks.stream().filter(
        { mandatoryPair -> mandatoryPair.pGnbduId == samplePair.pGnbduId && mandatoryPair.sGnbduId == samplePair.sGnbduId }).count() == 0) {
      verifiedMandatoryLinks.add(samplePair)
    }
  })
  flowInput?.mandatoryGNodeBPairsSelection?.readOnlyTable = verifiedMandatoryLinks
  return verifiedMandatoryLinks
}

void validateLinksAreFromSelectedGnbds(List<Map<String, Object>> linksTable)
    throws BpmnError {
  Map<Integer, String> ctsGnbs = gnbListPairCts.stream().collect(
      Collectors.toMap({ node -> node.gNBId }, { node -> node.gNBName }))
  List<Map<String, Object>> invalidExcludedLinks =
      linksTable.stream().filter({ partnerPair ->
        return (partnerPair.pGnbduId == partnerPair.sGnbduId) ||
            (!ctsGnbs.keySet().contains(partnerPair.pGnbduId)) ||
            (!ctsGnbs.keySet().contains(partnerPair.sGnbduId)) ||
            (ctsGnbs.get(partnerPair.pGnbduId) != partnerPair.pGnbduName) ||
            (ctsGnbs.get(partnerPair.sGnbduId) != partnerPair.sGnbduName)
      }).collect(Collectors.toList())

  if (!invalidExcludedLinks.isEmpty()) {
    failValidation(
        "${arcFlowUtils.getMessage(execution, "selectGnbdus.error.invalidPairConstraints")}: $invalidExcludedLinks",
        null)
  }
}

void validateUserConstraints(List<Map<String, Object>> mandatoryLinks,
    List<Map<String, Object>> excludedLinksTable) throws BpmnError {
  List<Map<String, Object>> invalidExcludedLinks =
      excludedLinksTable.stream().filter({ partnerPair ->
        return mandatoryLinks.contains(partnerPair)
      }).collect(Collectors.toList())

  if (!invalidExcludedLinks.isEmpty()) {
    failValidation(
        "${arcFlowUtils.getMessage(execution, "selectGnbdus.error.invalidPairConstraints")}: $invalidExcludedLinks",
        null)
  }
}

private void failValidation(String message, Throwable cause) throws BpmnError {
  EventRecorder.error(execution, message, FLOW_STEP)
  throw (cause ?
      new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, message, cause) :
      new BpmnError(flowAutomationErrorCodes.ERROR_ARC_AUTOMATION_EXCEPTION, message))
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

