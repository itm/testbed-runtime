var app = app || {};

$(function () {
    'use strict';

    app.NodeModel = Backbone.Model.extend({
        idAttribute: 'nodeUrn',
        urlRoot: 'rest/deviceConfigs',
        defaults: {
            "nodeUrn" : "urn:wisebed:uzl1:"
            // TODO add sensible defaults
        },
        toJSON: function(options) {
          return { "deviceConfigDto" : _.clone( this.attributes ) };
        }
    });

    var NodeCollection = Backbone.Collection.extend({
        model: app.NodeModel,
        url: "rest/deviceConfigs",
        parse: function(response) {
            // array of deviceConfigs grouped under "configs" in JSON
            return response.deviceConfigListDto.configs;
        }
    });
    app.Nodes = new NodeCollection();

});