// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy.utils

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ParseException
import org.apache.http.StatusLine
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.ServiceUnavailableRetryStrategy
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.camunda.bpm.engine.delegate.BpmnError
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Utility class simplifying HTTP requests in ARC Flow.
 */
class ApacheHttpClientAgent {
  private static final String ERROR_ARC_AUTOMATION_EXCEPTION = "error.arc_automation.exception"
  static final Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName())

  /**
   * Timeout to receive data.
   */
  final int HTTP_SOCKET_TIMEOUT = 8000

  /**
   * Timeout until a connection with Service is established.
   */
  final int HTTP_CONNECTION_TIMEOUT = 8000

  /**
   * Determines whether redirects should be handled automatically.
   */
  final boolean HTTP_REDIRECT_ENABLED = true

  /**
   * Number of times a HTTPRequest should be retried after an IOException occurs during execution.
   * Default is 3, but can be changed depending on the application.
   */
  int httpRetryRequestCount = 3

  /**
   * HTTP response status codes that are issued by a server in response to a client's request made to the server.
   */
  private int responseCode = 0
  String endPointUrl
  private String responseBody

  ApacheHttpClientAgent() {}

  private void executeRequest(HttpRequestBase httpRequest) throws IOException, BpmnError {

    //Immutable class encapsulating request configuration items/
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
        .setSocketTimeout(HTTP_SOCKET_TIMEOUT)
        .setRedirectsEnabled(HTTP_REDIRECT_ENABLED)
        .build()

    // A handler to determine if an HttpRequest should be retried after a recoverable IOException during execution.
    // returns true if the method should be retried, false otherwise
    HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
      @Override
      boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        LOGGER.info("Request Attempt Nr: ${executionCount}")
        if (exception instanceof UnknownHostException) {
          LOGGER.info("Request to unknown host - FAILURE")
          return false
        }
        if (exception instanceof ConnectTimeoutException) {
          LOGGER.info("Connection timeout - FAILURE")
          return false
        }
        if (exception instanceof InterruptedIOException) {
          LOGGER.info("Request refused - FAILURE")
          return false
        }
        return executionCount <= httpRetryRequestCount
      }
    }

    // HTTP Strategy for dynamically adjusting the retry interval
    // A Strategy interface that allows API users to control whether or not a retry should automatically be done, how many times it should be retried and so on
    // When the handshake succeeds and no socket/connect timeout occurs we might still get a HTTP response with status codes such as 502 (Bad Gateway) or 503 (Gateway Timeout).
    ServiceUnavailableRetryStrategy retryStrategy = new ServiceUnavailableRetryStrategy() {

      // The interval between the subsequent auto-retries in milliseconds
      int waitPeriod = 100

      @Override
      boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        waitPeriod *= 2
        return (
            executionCount <= httpRetryRequestCount && response.getStatusLine().getStatusCode() >=
                500)
      }

      @Override
      long getRetryInterval() {
        return waitPeriod
      }
    }

    // HTTP Client Setup
    CloseableHttpClient httpClient = HttpClients.custom()
        .setDefaultRequestConfig(requestConfig)
        .setRetryHandler(requestRetryHandler)
        .setServiceUnavailableRetryStrategy(retryStrategy)
        .build()

    // Execute Request to Service
    try {
      CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)
      try {
        processHttpResponse(httpResponse)
      } finally {
        httpResponse.close()
      }
    } finally {
      httpClient.close()
    }
  }

  void processHttpResponse(CloseableHttpResponse httpResponse) throws BpmnError {
    // Read response
    StatusLine statusLine = httpResponse.getStatusLine()
    HttpEntity responseEntity = httpResponse.getEntity()
    responseCode = statusLine.getStatusCode()
    LOGGER.info(
        "Connection to service established with URL ${endPointUrl} and http status code ${responseCode}.")
    try {
      responseBody = EntityUtils.toString(responseEntity)
    } catch (IllegalArgumentException responseEntityError) {
      LOGGER.error("Response Entity null - FAILURE with error ${responseEntityError}")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Response Entity is null.",
          responseEntityError)
    } catch (ParseException | IOException responseParsingError) {
      LOGGER.error("Parsing response - FAILURE with error ${responseParsingError}")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Parsing response failure.",
          responseParsingError)
    } finally {
      consume(responseEntity)
    }
  }

  static ApacheHttpClientAgent initialize() {
    return new ApacheHttpClientAgent()
  }

  ApacheHttpClientAgent url(String url) throws BpmnError {
    if (url) {
      this.endPointUrl = url
    } else {
      LOGGER.error("EndPointUrl is either null or empty.")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "EndPointUrl is either null or empty.")
    }
    return this
  }

  ApacheHttpClientAgent setHttpRetryRequestCount(int httpRetryRequestCount)
      throws IllegalArgumentException {
    if (httpRetryRequestCount >= 0) {
      this.httpRetryRequestCount = httpRetryRequestCount
    } else {
      LOGGER.error("HTTP retry count is less than 0.")
      throw new IllegalArgumentException(
          "HTTP retry count is less than 0: ${httpRetryRequestCount}")
    }
    return this
  }

  void processPostRequest(String entity) throws BpmnError {
    if (entity) {
      HttpPost httpPost = new HttpPost(endPointUrl)
      httpPost.setEntity(new StringEntity(entity))
      httpPost.setHeader("Accept", "application/json")
      httpPost.setHeader("Content-type", "application/json")
      try {
        executeRequest(httpPost)
      } catch (IOException requestException) {
        LOGGER.error("Post connection is not possible.", requestException)
        throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Service connection is not possible.",
            requestException)
      }
    } else {
      LOGGER.error("The entity sent for the request is either null or empty.")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION,
          "The entity sent for the request is either null or empty.")
    }
  }

  void processPutRequest(String entity) throws BpmnError {
    if (entity) {
      HttpPut httpPut = new HttpPut(endPointUrl)
      httpPut.setEntity(new StringEntity(entity))
      httpPut.setHeader("Accept", "application/json")
      httpPut.setHeader("Content-type", "application/json")
      try {
        executeRequest(httpPut)
      } catch (IOException requestException) {
        LOGGER.error("Put connection is not possible.", requestException)
        throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Service connection is not possible.",
            requestException)
      }
    } else {
      LOGGER.error("The entity sent for the request is either null or empty.")
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION,
          "The entity sent for the request is either null or empty.")
    }
  }


  void processGetRequest() throws BpmnError {
    HttpGet httpGet = new HttpGet(endPointUrl)
    httpGet.setHeader("Accept", "application/json")
    httpGet.setHeader("Content-type", "application/json")
    try {
      executeRequest(httpGet)
    } catch (IOException requestException) {
      LOGGER.error("Get connection is not possible.", requestException)
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Service connection is not possible.",
          requestException)
    }
  }

  void processDeleteRequest() throws BpmnError {
    HttpDelete httpDelete = new HttpDelete(endPointUrl)
    httpDelete.setHeader("Accept", "application/json")
    httpDelete.setHeader("Content-type", "application/json")
    try {
      executeRequest(httpDelete)
    } catch (IOException requestException) {
      LOGGER.error("Delete connection is not possible.", requestException)
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Service connection is not possible.",
          requestException)
    }
  }

  String getResponseBody() {
    return responseBody
  }

  int getResponseCode() {
    return responseCode
  }

  private static void consume(HttpEntity responseEntity) {
    try {
      EntityUtils.consume(responseEntity)
    } catch (IOException closingException) {
      throw new BpmnError(ERROR_ARC_AUTOMATION_EXCEPTION, "Could not close response stream correctly.",
          closingException)
    }
  }


}
