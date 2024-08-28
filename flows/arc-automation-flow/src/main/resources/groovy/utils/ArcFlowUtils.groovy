// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy.utils

import groovy.exceptions.JsonSchemaException
import groovy.json.JsonBuilder
import java.util.stream.Collectors

import org.apache.http.client.utils.URIBuilder
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources


/**
 * Utility class to expose reused functionalities in ARC Flow
 */
class ArcFlowUtils {
  private static final String ERROR_ARC_AUTOMATION_EXCEPTION = "error.arc_automation.exception"
  private static final String MESSAGES_FILE_READ_ERROR = "Messages file could not be read"
  private static final String PROPERTIES_FILE_PATH = "resources/config.properties"
  private static final String MESSAGES_FILE_PATH = "resources/messages.properties"
  static Properties properties = new Properties()
  static Properties messages = null
  static final Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName())

  private ArcFlowUtils() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Utility class cannot be instantiated")
  }

  static String getUrlFromPropertiesFile(DelegateExecution execution, String host, String path)
      throws BpmnError {
    String url = ""
    String propertiesFileContent = FlowPackageResources.get(execution, PROPERTIES_FILE_PATH)
    if (propertiesFileContent) {
      properties.load(new StringReader(propertiesFileContent))
      String hostHttp = properties.getProperty(host)
      String pathHttp = properties.getProperty(path)
      url = buildUri(execution, hostHttp, pathHttp)
    }
    return url
  }

  private static String buildUri(DelegateExecution execution, String hostHttp, String pathHttp)
      throws BpmnError {
    try {
      URI uri = new URIBuilder()
          .setScheme("http")
          .setHost(hostHttp)
          .setPath(pathHttp)
          .build()
      return uri.toString()
    } catch (URISyntaxException syntaxException) {
      LOGGER.error("Malformed URL, host : ${hostHttp}, path : ${pathHttp}")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION,
          getMessage(execution, "resource.error.uriConstruction"), syntaxException)
    }
  }

  static void validateSchema(DelegateExecution delegateExecution, String jsonString,
      String schemaLocation) throws BpmnError {
    try {
      JsonSchemaValidator.validateSchema(delegateExecution, jsonString, schemaLocation)
    } catch (JsonSchemaException invalidSchemaException) {
      LOGGER.error(invalidSchemaException.getMessage(), invalidSchemaException)
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, invalidSchemaException.getMessage(),
          invalidSchemaException)
    }
  }

  static String getMessage(DelegateExecution execution, String messageCode) throws BpmnError {
    if (messages == null) {
      String fileContent
      if (execution != null &&
          !execution.getClass().getSimpleName().contains("DelegateExecutionFake")) {
        fileContent = FlowPackageResources.get(execution, MESSAGES_FILE_PATH)
      } else {
        try {
          InputStream inputStream =
              ArcFlowUtils.class.getClassLoader().getResourceAsStream(MESSAGES_FILE_PATH)
          if (inputStream != null) {
          fileContent = getFileContent(inputStream)
        } else {
            throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, MESSAGES_FILE_READ_ERROR)
          }
        } catch (UncheckedIOException | BpmnError e) {
          throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, MESSAGES_FILE_READ_ERROR, e)
        }
      }
      if (fileContent != null) {
        messages = new Properties()
        messages.load(new StringReader(fileContent))
      } else {
        throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, MESSAGES_FILE_READ_ERROR)
      }
    }
    return messages.getProperty(messageCode)
  }

  private static String getFileContent(InputStream inputStream) {
    inputStream
        ?.withCloseable {
          new InputStreamReader(it)
              .withCloseable {
                new BufferedReader(it).lines()
                    .collect(Collectors.joining('\n'))
              }
        } ?: ""
  }


  static boolean isUniqueGnbdu(int id, String name, String cmHandle, List <?> gNBList) {
    for (gnbdu in gNBList) {
      if (gnbdu.gNBId == id || gnbdu.gNBName == name || gnbdu.gNBCmHandle == cmHandle) {
        return false
      }
    }
    return true
  }

  static String buildGetKpisRequestBody(List<Map<String, Object>> gnbs) {
    JsonBuilder json = new JsonBuilder()
    json {
      gnbList(gnbs.collect { ["gnbId": it.gNBId, "cmHandle": it.gNBCmHandle] })
    }
    return json.toString()
  }

//TODO Deprecating
  static String buildJsonWithKeyFromGnbIdList(List<Integer> gNBIdsList) {
    JsonBuilder selectedNodes = new JsonBuilder()
    selectedNodes {
      gnbIdList(gNBIdsList.collect { [gnbId: "${it}"] })
    }
    return selectedNodes.toString()
  }

  static String buildJson(List<Integer> gNBIdsList) {
    JsonBuilder selectedNodes = new JsonBuilder()
    selectedNodes {
      gnbIdList(gNBIdsList.collect { "${it}" })
    }
    return selectedNodes.toString()
  }

  static String buildJsonWithKey(List<Map<String, Object>> gnbs) {
    JsonBuilder selectedNodes = new JsonBuilder()
    selectedNodes {
      gnbIdList(gnbs.collect { ["gnbId": it.gNBId, "cmHandle": it.gNBCmHandle]})
    }
    return selectedNodes.toString()
  }

  static String buildJsonResponse(List optimizationResultSettings) {
    JsonBuilder selectedNodes = new JsonBuilder()

    selectedNodes {
      gnbIdList(optimizationResultSettings.collect { ["pGnbId": it.pGnbId, "sGnbId": it.sGnbId]})
    }
    return selectedNodes.toString()
  }
}



