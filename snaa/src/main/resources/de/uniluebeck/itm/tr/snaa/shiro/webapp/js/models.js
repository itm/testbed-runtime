var app = app || {};

$(function () {
    'use strict';

    app.UserModel = Backbone.Model.extend({
        idAttribute: 'name',
        urlRoot: app.shiroAdminRestApiContextPath + '/users'
    });

    var UserCollection = Backbone.Collection.extend({
        model: app.UserModel,
        url: app.shiroAdminRestApiContextPath + '/users',
		toJSON: function(list) {
			return { "users" : list };
		},
		parse: function(response) {
			return response.users;
		}
    });

    app.Users = new UserCollection();

});