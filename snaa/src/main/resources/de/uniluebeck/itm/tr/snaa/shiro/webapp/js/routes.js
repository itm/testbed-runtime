var app = app || {};

$(function() {

	app.Router = Backbone.Router.extend({

		routes : {
			'users' : 'users',
			'users/:name' : 'userDetail',
			'roles' : 'roles',
			'actions' : 'actions',
			'.*' : 'users'
		},

		initialize : function() {
		},

		users : function() {
			app.usersView = app.usersView || new app.UsersView({
				el : $("div.tab-content div#users")
			});
		},

		roles : function() {
			app.rolesView = app.rolesView || new app.RolesView({
				el : $("div.tab-content div#roles")
			});
		},

		actions : function() {
			app.actionsView = app.actionsView || new app.ActionsView({
				el : $("div.tab-content div#actions")
			});
		},

		userDetail : function(name) {
			new app.EditUserView({
				el : $("#edit-view"),
				model : {
					user : app.Users.get(name),
					roles : app.Roles
				}
			}).show();
		}

	});
});