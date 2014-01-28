var app = app || {};

$(function () {

	app.Router = Backbone.Router.extend({

		routes: {
			'users'       : 'users',
            'users/:name' : 'userDetail',
			'.*'          : 'users'
		},

		initialize: function() {
			console.log("app.initialize()");
			app.mainView = app.mainView || new app.MainView({
				el : $("body")
			});
		},

        users: function() {
			console.log("app.users()");
			app.table = app.table || new app.TableView({
				el: $("#table_configs")
			});
			app.table.show();
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