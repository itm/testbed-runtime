DeviceDB
=========

Contains configuration options and properties of all nodes in a testbed.

## REST API

There is a RESTful HTTP Webservice which consumes/produces JSON
 - **create**: ``POST`` on ``/rest/deviceConfigs/_[nodeURN]_`` with JSON representation as payload
 - **update**: ``PUT`` on ``/rest/deviceConfigs/_[nodeURN]_``   with JSON representation as payload
 - **remove**: ``DELETE`` on ``/rest/deviceConfigs/_[nodeURN]_`` 

**Note:** replace ``_[nodeURN]_`` with your URN (e.g. ``urn:wisebed:uzl1:0x2038``)

## JSON

 Example structure:

	{
		"deviceConfigDto": {
			"nodeUrn" : "urn:wisebed:uzl1:0x2038",
			"nodeType" : "isense39",
			"gatewayNode" : false,
			"description" : "this is the device description",
			"nodeUsbChipId" : "0123456789",
			"position" : {
				"x" : 123,
				"y" : 234,
				"z" : 345,
				"phi" : 12,
				"theta" : 23
			},
			"nodeConfiguration" : [
				{
					"key" : "bla",
					"value" : "blub"
				},
				{
					"key" : "bli",
					"value" : "blib"
				}
			],
			"defaultChannelPipeline" : [
				{
					"handlerName" : "dlestxetx",
					"instanceName" : "dlestxetx",
					"configuration" : []
				}
			],
			"timeoutNodeApiMillis" : 100,
			"timeoutResetMillis" : 1000,
			"timeoutFlashMillis" : 120000,
			"timeoutCheckAliveMillis" : 1000
		}
	}