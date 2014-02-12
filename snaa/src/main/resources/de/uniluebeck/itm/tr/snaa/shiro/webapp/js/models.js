var app = app || {};

$(function() {
	'use strict';

	app.ActionModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/actions'
	});

	var ActionCollection = Backbone.Collection.extend({
		model : app.ActionModel,
		url : app.rest_api_context_path + '/actions',
		comparator : 'name'
	});

	app.RoleModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/roles'
	});

	var RoleCollection = Backbone.Collection.extend({
		model : app.RoleModel,
		url : app.rest_api_context_path + '/roles',
		comparator : 'name'
	});

	app.UserModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/users'
	});

	var UserCollection = Backbone.Collection.extend({
		model : app.UserModel,
		url : app.rest_api_context_path + '/users',
		comparator : 'name'
	});

	app.ResourceGroupModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/resource_groups'
	});

	var ResourceGroupCollection = Backbone.Collection.extend({
		model : app.ResourceGroupModel,
		url : app.rest_api_context_path + '/resource_groups',
		comparator : 'name'
	});

	app.NodeModel = Backbone.Model.extend({
		idAttribute: 'nodeUrn',
		urlRoot: app.device_db_rest_api_context_path + '/deviceConfigs'
	});

	var NodeCollection = Backbone.Collection.extend({
		model: app.NodeModel,
		url: app.device_db_rest_api_context_path + '/deviceConfigs',
		toJSON: function(list) {
			return { "configs" : list };
		},
		parse: function(response) {
			return response.configs;
		},
		comparator : 'nodeUrn'
	});

	app.Roles = new RoleCollection();
	app.Users = new UserCollection();
	app.Actions = new ActionCollection();
	app.ResourceGroups = new ResourceGroupCollection();
	app.Nodes = new NodeCollection();
});