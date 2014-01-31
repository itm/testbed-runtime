var app = app || {};

$(function () {

	app.Router = Backbone.Router.extend({

		routes: {
			'users'       : 'users',
            'users/:name' : 'userDetail',
			'roles'       : 'roles',
			'actions'     : 'actions',
			'.*'          : 'users',
			''            : 'users'
		},

		initialize: function() {
			console.log("app.initialize()");
		},

		users: function() {
			console.log("app.users()");
			app.usersView = app.usersView || new app.UsersView({
				el: $("div.tab-content div#users")
			});
		},

		roles: function() {
			console.log("app.roles()");
			app.rolesView = app.rolesView || new app.RolesView({
				el: $("div.tab-content div#roles")
			});
		},

		actions: function() {
			console.log("app.actions()");
			app.actionsView = app.actionsView || new app.ActionsView({
				el: $("div.tab-content div#actions")
			});
		},

        userDetail: function(name) {
			console.log("app.userDetail()");
            if ( !app.userDetailView || app.userDetailView.model.id != name) {
                app.userDetailView = new app.AddUserView({
                    el:     $("#edit-view"),
                    model:  app.Users.get(name)
                });
            }
            app.userDetailView.show();
        }

	});
});