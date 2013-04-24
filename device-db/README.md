DeviceDB
=========

Contains configuration options and properties of all nodes in a testbed.

## REST API

There is a RESTful HTTP Webservice which consumes/produces JSON
 - **create**: ``POST`` on ``/rest/deviceConfigs/_[nodeUrn]_`` with JSON representation as payload
 - **update**: ``PUT`` on ``/rest/deviceConfigs/_[nodeUrn]_``   with JSON representation as payload
 - **remove**: ``DELETE`` on ``/rest/deviceConfigs/_[nodeUrn]_``

**Note:** replace ``_[nodeUrn]_`` with your node URN (e.g. ``urn:wisebed:uzl1:0x2038``)

## JSON

 Example structure:

	{
		"deviceConfigDto": {
			"nodeType": "isense39",
            "nodeUrn": "urn:wisebed:uzl1:0x2038",
            "position": {
                "phi": 12,
                "theta": 23,
                "x": 123,
                "y": 234,
                "z": 345
            },
            "description": "this is the device description",
            "gatewayNode": false,
			"capabilities": [
                {
                    "datatype": "integer",
                    "defaultValue": 0,
                    "name": "urn:wisebed:node:capability:light",
                    "unit": "lux"
                },
                {
                    "datatype": "integer",
                    "defaultValue": 0,
                    "name": "urn:wisebed:node:capability:humidity",
                    "unit": "raw"
                }
            ],
            "defaultChannelPipeline": {
                "configuration": [
                    {
                        "key": "param1",
                        "value": "val1"
                    },
                    {
                        "key": "param1",
                        "value": "val2"
                    },
                    {
                        "key": "param2",
                        "value": "val3"
                    }
                ],
                "handlerName": "dlestxetx",
                "instanceName": "dlestxetx"
            },
            "nodeConfiguration": [
                {
                    "key": "bli",
                    "value": "blib"
                },
                {
                    "key": "bla",
                    "value": "blub"
                }
            ],
            "timeoutCheckAliveMillis": 1000,
            "timeoutFlashMillis": 120000,
            "timeoutNodeApiMillis": 100,
            "timeoutResetMillis": 1000
		}
	}