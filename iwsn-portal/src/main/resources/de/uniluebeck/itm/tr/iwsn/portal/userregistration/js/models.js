var app = app || {};

$(function() {
	'use strict';

	app.UserModel = Backbone.Model.extend({
		idAttribute : 'email',
		urlRoot : app.rest_api_context_path + '/users'
	});

	var UserCollection = Backbone.Collection.extend({
		model : app.UserModel,
		url : app.rest_api_context_path + '/users',
		comparator : 'email'
	});

	app.Users = new UserCollection();
});