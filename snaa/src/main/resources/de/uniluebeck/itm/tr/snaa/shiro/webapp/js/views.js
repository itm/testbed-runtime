var app = app || {};

$(function() {
	'use strict';

	app.RolesView = Backbone.View.extend({

		template : Handlebars.getTemplate('roles'),

		events : {

		},

		initialize : function() {
			this.render();
		},

		render : function() {
			this.$el.append(this.template());
			return this;
		},

		show : function() {
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.ActionsView = Backbone.View.extend({

		template : Handlebars.getTemplate('actions'),

		events : {

		},

		initialize : function() {
			this.render();
		},

		render : function() {
			this.$el.append(this.template());
			return this;
		},

		show : function() {
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.UsersView = Backbone.View.extend({

		template : Handlebars.getTemplate('users'),

		events : {
			'click p#btnUserAdd' : 'clickedAddUser',
			'click a.user-edit' : 'clickedEditUser',
			'click a.user-remove' : 'clickedRemoveUser'
		},

		initialize : function() {
			// changes in our collection will redraw view
			this.listenTo(app.Users, 'all', this.render);
			this.render();
		},

		render : function() {
			var seed = { users : _.map(
					app.Users.models,
					function(el) {
						return el.attributes;
					})
			};
			this.$el.html(this.template(seed));
			return this;
		},

		clickedRemoveUser : function(e) {
			e.preventDefault();
			var id = $(e.target).parents('tr').data('id');
			var msg = $(e.target).data('confirm') || $(e.target).parent().data('confirm');
			bootbox.confirm(msg, function(sure) {
				if (sure) {
					app.Users.get(id).destroy();
				}
			});
		},

		clickedAddUser : function(e) {
			e.preventDefault();
			new app.EditUserView({
				el : $("#edit-view"),
				model : {
					user : new app.UserModel(),
					roles : app.Roles
				}
			}).show();
		},

		clickedEditUser : function(e) {
			e.preventDefault();
			var id = $(e.target).parents('tr').data('id');

			// navigate to modal dialog but don't add history entry as it is a modal dialog
			app.routes.navigate('users/' + id, {trigger : true, replace : true});
		},

		show : function() {
			app.routes.navigate('users');
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.EditUserView = Backbone.View.extend({

		template : Handlebars.getTemplate('user-edit'),

		events : {
			'click #save' : 'save',
			'click #close' : 'close',
			'hidden' : 'hidden'
		},

		initialize : function() {
			this.render();
		},

		render : function() {
			this.$el.html(this.template({
				user : this.model.user.attributes,
				roles : this.model.roles.models
			}));
			this.show();
			return this;
		},

		save : function(e) {
			e.preventDefault();
			var self = this;

			// serialize form
			var nodeCallback = function(node) {

			};

			var data = form2js('modal-form', '.', true, nodeCallback, true);

			if (data.password == '********') {
				data.password = null;
			}

			// send to server
			this.model.user.save(data, {
				wait : true,
				success : function(model, response, options) {
					app.Users.add(self.model.user, {merge : true});
					self.close(e);
				},
				error : function(model, xhr, options) {
					if (xhr.status == 201) {
						app.Users.add(self.model.user, {merge : true});
						self.close(e);
					} else {
						console.log(xhr);
						alert(xhr.responseText);
					}
				}
			});
		},

		close : function(e) {
			e.preventDefault();
			this.$el.find('.modal').modal('hide');
		},

		hidden : function() {
			this.undelegateEvents();
			app.routes.navigate('users', {trigger : true});
		},

		show : function() {
			this.$el.find('.modal').modal('show');
		}
	});

});