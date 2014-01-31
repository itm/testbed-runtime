var app = app || {};

$(function () {
    'use strict';

    app.UserModel = Backbone.Model.extend({
        idAttribute: 'name',
        urlRoot: app.rest_api_context_path + '/users'
    });

    var UserCollection = Backbone.Collection.extend({
        model: app.UserModel,
        url: app.rest_api_context_path + '/users',
		toJSON: function(list) {
			return { "users" : list };
		},
		parse: function(response) {
			return response.users;
		}
    });

    app.Users = new UserCollection();

});