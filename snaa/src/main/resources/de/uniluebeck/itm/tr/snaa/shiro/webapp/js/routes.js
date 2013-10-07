var app = app || {};

$(function () {

	app.Router = Backbone.Router.extend({
		routes: {
			'users' : 'users',
            'users/:name': 'userDetail'
		},

		initialize: function() {

		},

        users: function() {
			app.table = app.table || new app.TableView({
				el: $("#table_configs")
			});
			app.table.show();
		},

        userDetail: function(name) {
            if ( !app.userDetailView || app.userDetailView.model.id != name) {
                app.userDetailView = new app.AddUserView({
                    el:     jQuery("#edit-view"),
                    model:  app.Users.get(name)
                });
            }
            app.userDetailView.show();
        }

	});

});