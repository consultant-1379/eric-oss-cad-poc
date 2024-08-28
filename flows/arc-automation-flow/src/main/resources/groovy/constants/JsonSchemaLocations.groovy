// COPYRIGHT Ericsson 2023
//
// The copyright to the computer program(s) herein is the property of
// Ericsson Inc. The programs may be used and/or copied only with written
// permission from Ericsson Inc. or in accordance with the terms and
// conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.

package groovy.constants

/**
 * This interface holds the paths for the JSON schemas used for validating responses.
 */
interface JsonSchemaLocations {
  static final String GNBDU_LIST_GET="resources/schemas/gnbduList-get.json"
  static final String COVERAGE_MEASUREMENT_PUT = "resources/schemas/coverageDataMeasurement-put.json"
  static final String KPI_DATA_POST = "resources/schemas/kpiData-post.json"
  static final String START_OPTIMIZATION_POST = "resources/schemas/startOptimization-post.json"
  static final String OPTIMIZATION_DATA_GET = "resources/schemas/OptimizationData-get.json"
  static final String ASSIGN_OPTIMIZATION_ID_POST = "resources/schemas/AssignOptimizationId.json"
  static final String STOP_OPTIMIZATION_DELETE = "resources/schemas/stopOptimization-delete.json"
}