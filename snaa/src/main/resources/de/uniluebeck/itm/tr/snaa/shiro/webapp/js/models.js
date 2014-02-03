var app = app || {};

$(function() {
	'use strict';

	app.RoleModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/roles'
	});

	var RoleCollection = Backbone.Collection.extend({
		model : app.RoleModel,
		url : app.rest_api_context_path + '/roles'
	});

	app.UserModel = Backbone.Model.extend({
		idAttribute : 'name',
		urlRoot : app.rest_api_context_path + '/users'
	});

	var UserCollection = Backbone.Collection.extend({
		model : app.UserModel,
		url : app.rest_api_context_path + '/users'
	});

	app.Users = new UserCollection();
	app.Roles = new RoleCollection();
});