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

	app.Roles = new RoleCollection();
	app.Users = new UserCollection();
	app.Actions = new ActionCollection();
});