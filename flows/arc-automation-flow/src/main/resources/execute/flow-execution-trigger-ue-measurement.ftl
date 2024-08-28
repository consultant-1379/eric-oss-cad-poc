{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "name": "Check Results",
  "type": "object",
  "properties": {
    "triggerUEMeasurement": {
      "name": "Trigger UE Measurement",
      "type": "object",
      "properties": {
        "ListofEnabledgNB": {
          "name": "Trigger optimization",
          "type": "array",
          "info": "TriggerOptimization",
          "format": "list",
          "readOnly": true,
          "items": {
            "type": "integer",
            "schemaGen": {
              "type": "default",
              "binding": "variable:local:gnbIdList"
            }
          }
        }
        <#if gnbNotFoundListIsEmpty == false>
        ,
        "ListofNotFoundgNB": {
          "name": "List of Not found gNB",
          "type": "array",
          "info": "This is the list of not found gNB selected by user.",
          "format": "list",
          "readOnly": true,
          "items": {
            "type": "integer",
            "schemaGen": {
              "type": "default",
              "binding": "variable:local:gnbIdNotFoundList"
            }
          }
        }
        </#if>
      },
      "additionalProperties": false
    }
  }
}