var app = app || {};

$(function () {
    'use strict';

    app.NodeModel = Backbone.Model.extend({
        idAttribute: 'nodeUrn',
        urlRoot: 'rest/deviceConfigs',
        defaults: {
            "nodeUrn" : "urn:wisebed:uzl1:"
            // TODO add sensible defaults
        }
    });

    var NodeCollection = Backbone.Collection.extend({
        model: app.NodeModel,
        url: "rest/deviceConfigs"
    });

    app.Nodes = new NodeCollection();

});