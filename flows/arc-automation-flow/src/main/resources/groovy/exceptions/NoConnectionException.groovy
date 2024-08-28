// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy.exceptions

/**
 * Signals that a connection exception related to a HTTP request has occurred.
 */
class NoConnectionException extends Exception {
  NoConnectionException(String message) { super(message) }
}
