var app = app || {};

$(function() {

	app.Router = Backbone.Router.extend({

		routes : {
			':email/thank_you' : 'thank_you',
			':email/change_pwd' : 'user_edit',
			'' : 'user_new'
		},

		initialize : function() {

		},

		thank_you : function(email) {
			var view = new app.ThankYouView({
				el : $("div.container"),
				model : new app.UserModel({email : email})
			});
			view.show();
		},

		user_new : function() {
			var view = new app.EditUserView({
				el : $("div.container"),
				model : new app.UserModel()
			});
			view.show();
		},

		user_edit : function(email) {
			var view = new app.EditUserView({
				el : $("div.container"),
				model : new app.UserModel({ email : email })
			});
			view.show();
		}
	});
});
