// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy.utils

import groovy.exceptions.JsonSchemaException
import java.util.stream.Collectors

import org.camunda.bpm.engine.delegate.DelegateExecution
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory

/**
 * Utility class allowing to check the JSON schema on JSON strings.
 */
class JsonSchemaValidator {

  private static String schemaLoadingError = "Could not load schema from file"
  private static String parsingError = "An error occurred while parsing JSON"
  private static String validationError = "An error occurred while validating the JSON schema"

  /**
   * Validates the JSON schema of a given JSON string. The schema is read from the given schema
   * location.
   * @param execution The flow execution.
   * @param jsonString The target string to be validated.
   * @param schemaLocation The Schema location.
   * @throws JsonSchemaException If the schema is not valid.
   */
  static void validateSchema(DelegateExecution execution, String jsonString,
      String schemaLocation) throws JsonSchemaException {
    JsonNode jsonNode = getJsonNodeFromString(jsonString)
    JsonSchema schema = getJsonSchemaFromFile(execution, schemaLocation)
    validateSchema(schema, jsonNode)
  }

  /**
   * Reads the JSON schema from the given resource file.
   * @param delegateExecution The flow execution.
   * @param resourceFilePath The JSON resource file path.
   * @return The schema built from the retrieved string.
   * @throws JsonSchemaException If an exception occurs while building the schema.
   */
  static JsonSchema getJsonSchemaFromFile(DelegateExecution delegateExecution,
      String resourceFilePath) throws JsonSchemaException {
    String schemaString = getStringFromResourceFile(delegateExecution, resourceFilePath)
    JsonNode schemaNode = getJsonNodeFromString(schemaString)
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault()
    if(schemaNode != null){
      try {
        return schemaFactory.getJsonSchema(schemaNode)
      } catch (ProcessingException ex) {
        throw new JsonSchemaException("$schemaLoadingError($resourceFilePath): ${ex.getMessage()}",
            ex)
      }
    } else {
      throw new JsonSchemaException("$schemaLoadingError($resourceFilePath): null schema node")
    }
  }

  /**
   * Reads the string content of a given file. It uses the FlowPackagesResources class to read the file.
   * @param execution The delegate execution.
   * @param resourceFilePath The path of the resource file.
   * @return The string read from the given file.
   * @throws JsonSchemaException If the String cannot be read from the file
   */
  static String getStringFromResourceFile(DelegateExecution execution,
      String resourceFilePath) throws JsonSchemaException {
    String schemaContent
    if (execution.getClass().getSimpleName().contains("DelegateExecutionFake")) {
      try {
        InputStream inputStream =
            JsonSchemaValidator.class.getClassLoader().getResourceAsStream(resourceFilePath)
        if (inputStream != null) {
          schemaContent =
              inputStream
                  .withCloseable {
                    new InputStreamReader(it)
                        .withCloseable {
                          new BufferedReader(it).lines()
                              .collect(Collectors.joining('\n'))
                        }
                  }
        } else {
          throw new JsonSchemaException("$schemaLoadingError($resourceFilePath): null content")
        }
      } catch (UncheckedIOException | JsonSchemaException e) {
        throw new JsonSchemaException("$schemaLoadingError($resourceFilePath): ${e.getMessage()}",
            e)
      }
    } else {
      schemaContent = FlowPackageResources.get(execution, resourceFilePath)
      if (schemaContent == null) {
        throw new JsonSchemaException("$schemaLoadingError($resourceFilePath): null content")
      }
    }
    return schemaContent
  }

  /**
   * Creates a JsonNode from a given string.
   * @param jsonString String to build node from.
   * @return JSON node built from string.
   * @throws JsonSchemaException If the string cannot be correctly parsed.
   */
  static JsonNode getJsonNodeFromString(String jsonString) throws JsonSchemaException {
    ObjectMapper mapper = new ObjectMapper()
    try {
      return mapper.readTree(jsonString)
    } catch (IllegalArgumentException | JsonProcessingException ex) {
      throw new JsonSchemaException("$parsingError: ${ex.getMessage()}", ex)
    }
  }

  /**
   * Validates the JSON schema of a given JSON node.
   * @param jsonSchema The JSON schema used to validate.
   * @param jsonNode The target JSON node to be validated.
   * @throws JsonSchemaException If the validation of the node schema is not successful or an error is present in the schema.
   */
  static void validateSchema(JsonSchema jsonSchema, JsonNode jsonNode) throws JsonSchemaException {
    try {
      ProcessingReport processingReport = jsonSchema.validate(jsonNode)
      if (!processingReport.isSuccess()) {
        throw new JsonSchemaException("$validationError: ${processingReport.toString()}")
      }
    } catch (NullPointerException nullPointerException) {
      throw new JsonSchemaException("$validationError: Null JSON node", nullPointerException)
    } catch (ProcessingException processingException) {
      throw new JsonSchemaException("$validationError: ${processingException.getMessage()}",
          processingException)
    }
  }
}
