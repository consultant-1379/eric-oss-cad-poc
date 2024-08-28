{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "name": "Confirm the configuration of new BB pairs",
  "type": "object",
  "properties": {
    <#if optimizationResultTableIsEmpty == false>
    "ConfigureBBpairs": {
      "name": "Apply the new BB pairs links?",
      "type": "object",
      "format": "radio",
      "oneOf": [
        {
          "properties": {
            "YesRadioButton": {
              "name": "Yes",
              "type": "boolean"
            }
          },
          "required": [
            "YesRadioButton"
          ],
          "additionalProperties": false
        },
        {
          "properties": {
            "NoRadioButton": {
              "name": "No",
              "type": "boolean"
            }
          },
          "required": [
            "NoRadioButton"
          ],
          "additionalProperties": false
        }
      ]
    },
    "table": {
      "name": "Results table",
      "type": "array",
      "format": "select-table",
      "info": "This is the list of links to be applied",
      "items": {
        "type": "object",
        "properties": {
          "Id": {
            "type": "integer",
            "name": "Id"
          },
          "pGnbId": {
            "type": "integer",
            "name": "Primary gNB"
          },
          "sGnbId": {
            "type": "integer",
            "name": "Secondary gNB"
          },
          "usability": {
            "type": "string",
            "name": "usability"
          }
        },
        "required": [
          "pGnbId"
        ],
        "additionalProperties": {
          "anyOf": [{
            "pCmHandle": {
               "type": "string",
               "name": "pCmHandle"
            },
            "sCmHandle": {
               "type": "string",
               "name": "sCmHandle"
            }
        }]
        }
      },
      "schemaGen": [{
        "type": "default",
        "binding": "variable:local:optimizationResultTable"
      },
        {
          "type": "selectableItems",
          "binding": "variable:local:optimizationResultTable"
        }
      ]
    }
    </#if>
    <#if optimizationResultTableIsEmpty == true>
    "stop": {
      "name": "WARNING!",
      "type": "string",
      "readOnly": true,
      "default": "No target optimization partners found. Moving onto next available optimization."
    }
    </#if>
  },
  <#if optimizationResultTableIsEmpty == false>
  "required": [
    "ConfigureBBpairs", "table"
  ],
  </#if>
  "additionalProperties": false
}