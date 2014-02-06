var app = app || {};

$(function() {
	'use strict';

	app.RolesView = Backbone.View.extend({

		template : Handlebars.getTemplate('roles'),

		events : {
			'click #button-role-add' : 'clickedAddRole',
			'click a.role-remove' : 'clickedRemoveRole'
		},

		initialize : function() {
			this.listenTo(app.Roles, 'sync', this.render);
			this.listenTo(app.Roles, 'destroy', this.render);
			this.render();
		},

		render : function() {
			var model = {
				roles : _.map(app.Roles.models, function(el) {
					return el.attributes;
				})
			};
			this.$el.html(this.template(model));
			return this;
		},

		clickedAddRole : function(e) {
			e.preventDefault();
			new app.AddRoleView({
				el : $("#edit-view"),
				model : new app.RoleModel()
			}).show();
		},

		clickedRemoveRole : function(e) {
			e.preventDefault();
			var id = $(e.target).parents('tr').data('id');
			var msg = $(e.target).data('confirm') || $(e.target).parent().data('confirm');
			bootbox.confirm(msg, function(sure) {
				if (sure) {
					app.Roles.get(id).destroy();
				}
			});
		},

		show : function() {
			app.routes.navigate('roles');
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
			this.listenTo(app.Actions, 'sync', this.render);
			this.render();
		},

		render : function() {
			var model = {
				actions : _.map(app.Actions.models, function(el) {
					return el.attributes;
				})
			};
			this.$el.html(this.template(model));
			return this;
		},

		show : function() {
			app.routes.navigate('actions');
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.UsersView = Backbone.View.extend({

		template : Handlebars.getTemplate('users'),

		events : {
			'click p#button-user-add' : 'clickedAddUser',
			'click a.user-edit' : 'clickedEditUser',
			'click a.user-remove' : 'clickedRemoveUser'
		},

		initialize : function() {
			this.listenTo(app.Users, 'sync', this.render);
			this.listenTo(app.Users, 'destroy', this.render);
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

	app.AddRoleView = Backbone.View.extend({

		template : Handlebars.getTemplate('role-edit'),

		events : {
			'click #save' : 'save',
			'click #close' : 'close',
			'hidden' : 'hidden'
		},

		initialize : function() {
			this.render();
			this.$el.find('input#name').attr("autofocus", "autofocus");
		},

		render : function() {
			this.$el.html(this.template(this.model));
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

			// send to server
			this.model.save(data, {
				wait : true,
				success : function(model, response, options) {
					app.Roles.add(self.model, {merge : true});
					app.Roles.sort();
					self.close(e);
				},
				error : function(model, xhr, options) {
					console.log(xhr);
					alert(xhr.responseText);
				}
			});
		},

		close : function(e) {
			e.preventDefault();
			this.$el.find('.modal').modal('hide');
		},

		hidden : function() {
			this.undelegateEvents();
			app.routes.navigate('roles', {trigger : true});
		},

		show : function() {
			this.$el.find('.modal').modal('show');
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
			if (this.model.user.attributes.name) {
				this.$el.find('input#password').attr("autofocus", "autofocus");
			} else {
				this.$el.find('input#name').attr("autofocus", "autofocus");
			}
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

			if (!data.roles) {
				this.model.user.attributes.roles = [];
			}

			// send to server
			this.model.user.save(data, {
				wait : true,
				success : function(model, response, options) {
					app.Users.add(self.model.user, {merge : true});
					app.Users.sort();
					self.close(e);
				},
				error : function(model, xhr, options) {
					console.log(xhr);
					alert(xhr.responseText);
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