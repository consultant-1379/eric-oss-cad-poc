// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package execute

import groovy.utils.ArcFlowUtils

import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.extension.mockito.delegate.DelegateExecutionFake
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import com.ericsson.oss.services.flowautomation.flowapi.FlowPackageResources

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

@RunWith(PowerMockRunner.class)
@PrepareForTest(FlowPackageResources.class)
class ArcFlowUtilsTest {

  DelegateExecution delegateExecution
  String Expected_URL = "http://ctsmock:8001/ctw/gnbdu/all/"
  String Properties_Path = "resources/config.properties"
  String PROPERTIES_FILE_Value = "cts.host = ctsmock:8001\n" + "gnbduList.path = /ctw/gnbdu/all/"

  @Rule
  private ExpectedException thrown = ExpectedException.none()

  @Before
  void init() throws IOException {
    delegateExecution = new DelegateExecutionFake()
  }

  @Test
  void whenCreatingInstanceThenUnsupportedOperationException() {
    thrown.expect(UnsupportedOperationException.class)
    new ArcFlowUtils()
  }

  @Test
  void testGetUrlFromPropertiesFileSuccess() {
    String host = "cts.host"
    String path = "gnbduList.path"
    PowerMockito.mockStatic(FlowPackageResources.class)
    Mockito.when(FlowPackageResources.get(delegateExecution, Properties_Path)).thenReturn(
        PROPERTIES_FILE_Value)
    assertEquals(Expected_URL, ArcFlowUtils.getUrlFromPropertiesFile(delegateExecution, host, path))
  }

  @Test
  void testGetUrlFromPropertiesFileWithPropertiesContentIsNull() {
    String host = "cts.host"
    String path = "gnbduList.path"
    PowerMockito.mockStatic(FlowPackageResources.class)
    Mockito.when(FlowPackageResources.get(delegateExecution, Properties_Path)).thenReturn(null)
    assertEquals("", ArcFlowUtils.getUrlFromPropertiesFile(delegateExecution, host, path))
  }

  @Test
  void testBuildUriSuccess() {
    String host = "ctsmock:8001"
    String path = "/ctw/gnbdu/all/"
    String uri = ArcFlowUtils.buildUri(new DelegateExecutionFake(), host, path)
    assertEquals(Expected_URL, uri)
  }

  @Test
  void testBuildUriFailedWithWrongHostValue() {
    String host = "WrongHostValue"
    String path = "/optimizations/"
    try {
      ArcFlowUtils.buildUri(new DelegateExecutionFake(), host, path)
    } catch (BpmnError error) {
      assertEquals("BpmnError caught, but unexpected message",
          ArcFlowUtils.getMessage(delegateExecution, "resource.error.uriConstruction"),
          error.getMessage())
    }
  }

  @Test
  void givenWrongJsonWhenCheckSchemaThenBpmnError() {
    String json = "{}"
    String jsonSchemaFile = "resources/schemas/startOptimization-post.json"

    thrown.expect(BpmnError.class)
    ArcFlowUtils.validateSchema(new DelegateExecutionFake(), json, jsonSchemaFile)
  }

  @Test
  void givenCorrectJsonWhenCheckSchemaThenNormalExecution() {
    String json = '{"status": "OK", "result": "Correctly started"}'
    String jsonSchemaFile = "resources/schemas/startOptimization-post.json"

    ArcFlowUtils.validateSchema(new DelegateExecutionFake(), json, jsonSchemaFile)
  }

  @Test
  void givenNonExistentMessageCodeWhenGetMessageThenMessageCode() {
    String messageCode = "someNonExistentCode"
    assertNull(ArcFlowUtils.getMessage(new DelegateExecutionFake(), messageCode))
  }

  @Test
  void givenValidMessageCodeWhenGetMessageThenMessage() {
    String messageCode = "status.inProgress"
    assertEquals("In progress", ArcFlowUtils.getMessage(new DelegateExecutionFake(), messageCode))
  }

  @Test
  void givenEmptyListWhenBuildJsonFromGnbIdListThenEmptyListInJson() {
    List<Integer> ids = []
    String expected = '{"gnbIdList":[]}'
    assertEquals(expected, ArcFlowUtils.buildJson(ids))
  }

  @Test
  void givenTwoIdListWhenBuildJsonFromGnbIdListThenListInJson() {
    List<Integer> ids = [1, 2]
    String expected = '{"gnbIdList":["1","2"]}'
    assertEquals(expected, ArcFlowUtils.buildJson(ids))
  }

  @Test
  void givenEmptyListWhenBuildJsonWithKeyFromGnbIdListThenEmptyListInJson() {
    List<Integer> ids = []
    String expected = '{"gnbIdList":[]}'
    assertEquals(expected, ArcFlowUtils.buildJsonWithKeyFromGnbIdList(ids))
  }

  @Test
  void givenTwoIdListWhenBuildJsonWithKeyFromGnbIdListThenMapListInJson() {
    List<Integer> ids = [1, 2]
    String expected = '{"gnbIdList":[{"gnbId":"1"},{"gnbId":"2"}]}'
    assertEquals(expected, ArcFlowUtils.buildJsonWithKeyFromGnbIdList(ids))
  }
}
