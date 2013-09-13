var app = app || {};

$(function () {
    'use strict';

    app.NodeModel = Backbone.Model.extend({
        idAttribute: 'nodeUrn',
        urlRoot: deviceDBRestApiContextPath + '/deviceConfigs',
        defaults: {
            "nodeUrn" : urnPrefix
        }
    });

    var NodeCollection = Backbone.Collection.extend({
        model: app.NodeModel,
        url: deviceDBRestApiContextPath + '/deviceConfigs',
		toJSON: function(list) {
			return { "configs" : list };
		},
		parse: function(response) {
			return response.configs;
		}
    });

    app.Nodes = new NodeCollection();

});