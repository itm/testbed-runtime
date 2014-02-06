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
			this.fetchUsers();
			this.fetchRoles();
			this.fetchActions();
		},

		users : function() {
			this.fetchUsers();
			app.usersView = app.usersView || new app.UsersView({
				el : $("div.tab-content div#users")
			});
		},

		roles : function() {
			this.fetchRoles();
			app.rolesView = app.rolesView || new app.RolesView({
				el : $("div.tab-content div#roles")
			});
		},

		actions : function() {
			this.fetchActions();
			app.actionsView = app.actionsView || new app.ActionsView({
				el : $("div.tab-content div#actions")
			});
		},

		userDetail : function(name) {

			var view = new app.EditUserView({
				el : $("#edit-view"),
				model : {
					user : app.Users.get(name),
					roles : app.Roles
				}
			});

			view.show();
		},

		fetchActions : function() {
			app.Actions.fetch({
				error : function() {
					alert('Error fetching actions list');
				}
			});
		},

		fetchRoles : function() {
			app.Roles.fetch({
				error : function() {
					alert('Error fetching roles list');
				}
			});
		},

		fetchUsers : function() {
			app.Users.fetch({
				error : function() {
					alert('Error fetching users list');
				}
			});
		}
	});
});
