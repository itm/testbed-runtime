var app = app || {};

$(function() {
	'use strict';

	app.NodeModel = Backbone.Model.extend({
		idAttribute : 'nodeUrn',
		urlRoot : deviceDBRestApiContextPath + '/deviceConfigs',
		defaults : {
			"nodeUrn" : nodeUrnPrefix
		},
		methodToURL : function(method, id) {

			var baseUri = deviceDBRestApiContextPath.indexOf('/', this.length - 1) !== -1 ?
					deviceDBRestApiContextPath + '/' :
					deviceDBRestApiContextPath;

			if (method == 'create' || method == 'update' || method == 'delete') {
				return baseUri + 'admin/deviceConfigs/' + encodeURIComponent(id);
			}
			return baseUri + 'deviceConfigs/' + encodeURIComponent(id);
		},
		sync : function(method, model, options) {
			options = options || {};
			options.url = model.methodToURL(method, model.id);

			return Backbone.sync.apply(this, arguments);
		}
	});

	var NodeCollection = Backbone.Collection.extend({
		model : app.NodeModel,
		url : deviceDBRestApiContextPath + '/deviceConfigs',
		toJSON : function(list) {
			return { "configs" : list };
		},
		parse : function(response) {
			return response.configs;
		}
	});

	app.Nodes = new NodeCollection();

});