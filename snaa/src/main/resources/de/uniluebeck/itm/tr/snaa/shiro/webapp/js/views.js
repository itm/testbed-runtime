var app = app || {};

$(function() {
	'use strict';

	app.PermissionsView = Backbone.View.extend({

		template : Handlebars.getTemplate('permissions'),

		events : {
			'change select#role' : 'onRoleSelectionChanged',
			'change select#resourceGroup' : 'onResourceGroupSelectionChanged',
			'change select#permissionsGranted' : 'onPermissionsGrantedSelectionChanged',
			'change select#permissionsAvailable' : 'onPermissionsAvailableSelectionChanged',
			'click button.permission-add' : 'onPermissionAddClicked',
			'click button.permission-remove' : 'onPermissionRemovedClicked'
		},

		initialize : function() {

			this.listenTo(app.Roles, 'sync', this.render);
			this.listenTo(app.Roles, 'destroy', this.render);

			this.listenTo(app.ResourceGroups, 'sync', this.render);
			this.listenTo(app.ResourceGroups, 'destroy', this.render);

			app.Permissions.fetch({wait : true});

			this.render();

			this.selectedRole = null;
			this.selectedResourceGroup = null;
		},

		render : function() {
			var model = {
				roles : _.map(app.Roles.models, function(el) {
					return el.attributes;
				}),
				resourceGroups : _.map(app.ResourceGroups.models, function(el) {
					return el.attributes;
				})
			};
			this.$el.html(this.template(model));
			return this;
		},

		onRoleSelectionChanged : function() {
			this.selectedRole = this.$el.find('select#role').val();
			this.updatePermissions();
		},

		onResourceGroupSelectionChanged : function() {
			this.selectedResourceGroup = this.$el.find('select#resourceGroup').val();
			this.updatePermissions();
		},

		onPermissionsGrantedSelectionChanged : function() {
			var selection = this.$el.find('select#permissionsGranted').val();
			this.$el.find('button.permission-remove').toggleClass('disabled',
					!selection || selection.length == 0);
		},

		onPermissionsAvailableSelectionChanged : function() {
			var selection = this.$el.find('select#permissionsAvailable').val();
			this.$el.find('button.permission-add').toggleClass('disabled', !selection || selection.length == 0);
		},

		onPermissionAddClicked : function() {
			var self = this;
			var permissionsToAdd = this.$el.find('select#permissionsAvailable').val();
			permissionsToAdd.forEach(function(permission) {
				var model = new app.PermissionModel();
				var data = {
					roleName : self.selectedRole,
					actionName : permission,
					resourceGroupName : self.selectedResourceGroup
				};
				var options = {
					wait : true,
					success : function(model, response, options) {
						app.Permissions.add(model, {merge : true});
						self.updatePermissions();
					},
					error : function(model, xhr, options) {
						console.log(xhr);
						alert(xhr.responseText);
					}
				};
				model.save(data, options);
			});
		},

		onPermissionRemovedClicked : function(e) {
			var self = this;
			var permissionsToRemove = this.$el.find('select#permissionsGranted').val();
			permissionsToRemove.forEach(function(permissionToRemove) {
				app.Permissions.models.forEach(function(permission) {
					var match = permission.attributes.roleName == self.selectedRole &&
							permission.attributes.resourceGroupName == self.selectedResourceGroup &&
							permission.attributes.actionName == permissionToRemove;
					if (match) {
						permission.destroy({
							wait : true,
							success : function() {
								app.Permissions.remove(permission);
								self.updatePermissions();
							},
							error : function(model, xhr, options) {
								console.log(xhr);
								if (xhr.responseText && "" != xhr.responseText) {
									alert(xhr.responseText);
								} else {
									alert('An error occurred during request processing!');
								}
							}
						});
					}
				});
			});
		},

		updatePermissions : function() {

			var self = this;
			var selectPermissionsGranted = this.$el.find('select#permissionsGranted');
			var selectPermissionsAvailable = this.$el.find('select#permissionsAvailable');

			selectPermissionsGranted.empty();
			selectPermissionsAvailable.empty();

			var editable = this.selectedRole && this.selectedResourceGroup;

			selectPermissionsGranted.toggleClass('disabled', !editable);
			selectPermissionsAvailable.toggleClass('disabled', !editable);

			if (editable) {

				var permissionsGranted = [];

				app.Permissions.models.forEach(function(permission) {
					var match = permission.attributes.roleName == self.selectedRole &&
							permission.attributes.resourceGroupName == self.selectedResourceGroup;

					if (match) {
						var option = $("<option />")
								.val(permission.attributes.actionName)
								.text(permission.attributes.actionName);
						selectPermissionsGranted.append(option);
						permissionsGranted.push(permission.attributes.actionName);
					}
				});

				app.Actions.models.forEach(function(action) {
					if (permissionsGranted.indexOf(action.attributes.name) == -1) {
						var option = $("<option />").val(action.attributes.name).text(action.attributes.name);
						selectPermissionsAvailable.append(option);
					}
				});
			}

			self.onPermissionsAvailableSelectionChanged();
			self.onPermissionsGrantedSelectionChanged();
		},

		show : function() {
			app.routes.navigate('permissions');
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.RolesView = Backbone.View.extend({

		template : Handlebars.getTemplate('roles'),

		events : {
			'click button.role-add' : 'clickedAddRole',
			'click button.role-remove' : 'clickedRemoveRole'
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
					app.Roles.get(id).destroy({
						wait : true,
						error : function(model, xhr, options) {
							console.log(xhr);
							if (xhr.responseText && "" != xhr.responseText) {
								alert(xhr.responseText);
							} else {
								alert('An error occurred during request processing!');
							}
						}
					});
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

	app.ResourceGroupsView = Backbone.View.extend({

		template : Handlebars.getTemplate('resource_groups'),

		events : {
			'click button.resource_group-add' : 'clickedAddResourceGroup',
			'click button.resource_group-edit' : 'clickedEditResourceGroup',
			'click button.resource_group-remove' : 'clickedRemoveResourceGroup'
		},

		initialize : function() {
			this.listenTo(app.ResourceGroups, 'sync', this.render);
			this.listenTo(app.ResourceGroups, 'destroy', this.render);
			this.render();
		},

		render : function() {
			var model = {
				resourceGroups : _.map(app.ResourceGroups.models, function(el) {
					return el.attributes;
				})
			};
			this.$el.html(this.template(model));
			return this;
		},

		clickedAddResourceGroup : function(e) {
			e.preventDefault();
			new app.EditResourceGroupView({
				el : $("#edit-view"),
				model : {
					resourceGroup : new app.ResourceGroupModel(),
					availableNodes : app.Nodes
				}
			}).show();
		},

		clickedEditResourceGroup : function(e) {
			e.preventDefault();
			var id = $(e.target).parents('tr').data('id');

			// navigate to modal dialog but don't add history entry as it is a modal dialog
			app.routes.navigate('resource_groups/' + id, {trigger : true, replace : true});
		},

		clickedRemoveResourceGroup : function(e) {
			e.preventDefault();
			var id = $(e.target).parents('tr').data('id');
			var msg = $(e.target).data('confirm') || $(e.target).parent().data('confirm');
			bootbox.confirm(msg, function(sure) {
				if (sure) {
					app.ResourceGroups.get(id).destroy({
						wait : true,
						error : function(model, xhr, options) {
							console.log(xhr);
							if (xhr.responseText && "" != xhr.responseText) {
								alert(xhr.responseText);
							} else {
								alert('An error occurred during request processing!');
							}
						}
					});
				}
			});
		},

		show : function() {
			app.routes.navigate('resource_groups');
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.EditResourceGroupView = Backbone.View.extend({

		template : Handlebars.getTemplate('resource_group-edit'),

		events : {
			'click button.edit-resource_group-save' : 'save',
			'click button.resource_group-add-node-urns' : 'onAddNodeUrns',
			'click button.resource_group-remove-node-urns' : 'onRemoveNodeUrns',
			'change select#nodeUrnsAvailable' : 'onNodeUrnsAvailableSelectionChange',
			'change select#nodeUrns' : 'onNodeUrnsSelectionChange',
			'hidden.bs.modal' : 'hidden'
		},

		initialize : function() {

			this.render();

			this.onNodeUrnsAvailableSelectionChange();
			this.onNodeUrnsSelectionChange();

			if (this.model.resourceGroup.attributes.name) {
				this.$el.find('select#nodeUrns').attr("autofocus", "autofocus");
			} else {
				this.$el.find('input#name').attr("autofocus", "autofocus");
			}
		},

		render : function() {
			var self = this;
			this.$el.html(this.template({
				resourceGroup : this.model.resourceGroup.attributes,
				availableNodeUrns : this.model.availableNodes.filter(function(node) {
					return self.model.resourceGroup.attributes.nodeUrns.indexOf(node.attributes.nodeUrn) == -1;
				}).map(function(node) {
					return node.attributes;
				})
			}));
			this.show();
			return this;
		},

		save : function(e) {
			e.preventDefault();
			var nodeUrns = [];
			this.$el.find('select#nodeUrns option').each(function(idx, option) {
				nodeUrns.push(option.value);
			});
			var data = {
				name : this.$el.find('input[name="name"]').val(),
				nodeUrns : nodeUrns
			};

			var self = this;

			// send to server
			this.model.resourceGroup.save(data, {
				wait : true,
				success : function(model, response, options) {
					app.ResourceGroups.add(self.model.resourceGroup, {merge : true});
					app.ResourceGroups.sort();
					self.close(e);
				},
				error : function(model, xhr, options) {
					console.log(xhr);
					alert(xhr.responseText);
				}
			});
		},

		onAddNodeUrns : function(e) {
			var select = this.$el.find('select#nodeUrns');
			this.$el.find('select#nodeUrnsAvailable option:selected').each(function(idx, option) {
				select.append($("<option />").val(option.value).text(option.text));
				option.remove();
			});
			this.onNodeUrnsAvailableSelectionChange();
		},

		onRemoveNodeUrns : function(e) {
			var selectNodeUrnsAvailable = this.$el.find('select#nodeUrnsAvailable');
			this.$el.find('select#nodeUrns').find('option:selected').each(function(idx, option) {
				selectNodeUrnsAvailable.append($("<option />").val(option.value).text(option.text));
				option.remove();
			});
			this.onNodeUrnsSelectionChange();
		},

		onNodeUrnsAvailableSelectionChange : function() {
			var disable = this.$el.find('select#nodeUrnsAvailable option:selected').size() == 0;
			this.$el.find('button.resource_group-add-node-urns').toggleClass('disabled', disable);
		},

		onNodeUrnsSelectionChange : function() {
			var disable = this.$el.find('select#nodeUrns option:selected').size() == 0;
			this.$el.find('button.resource_group-remove-node-urns').toggleClass('disabled', disable);
		},

		close : function(e) {
			this.$el.find('.modal').modal('hide');
		},

		hidden : function() {
			this.undelegateEvents();
			app.routes.navigate('resource_groups', {trigger : true});
		},

		show : function() {
			this.$el.find('.modal').modal('show');
		}
	});

	app.UsersView = Backbone.View.extend({

		template : Handlebars.getTemplate('users'),

		events : {
			'click button.user-add' : 'clickedAddUser',
			'click button.user-edit' : 'clickedEditUser',
			'click button.user-remove' : 'clickedRemoveUser'
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
					app.Users.get(id).destroy({
						wait : true,
						error : function(model, xhr, options) {
							console.log(xhr);
							if (xhr.responseText && "" != xhr.responseText) {
								alert(xhr.responseText);
							} else {
								alert('An error occurred during request processing!');
							}
						}
					});
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
			'click button.edit-role-save' : 'save',
			'hidden.bs.modal' : 'hidden'
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

			var data = form2js('role-edit-form', '.', true, nodeCallback, true);

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

		close : function() {
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
			'click button.edit-user-save' : 'save',
			'hidden.bs.modal' : 'hidden'
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
			var model = {
				user : {
					name : this.model.user.attributes.name,
					roles : this.model.user.attributes.roles.map(function(role) {
						return role.name
					})
				},
				roles : this.model.roles.map(function(roleModel) {
					return roleModel.attributes.name;
				})
			};
			this.$el.html(this.template(model));
			this.show();
			return this;
		},

		save : function(e) {
			e.preventDefault();
			var self = this;

			// serialize form
			var nodeCallback = function(node) {

			};

			var data = form2js('user-edit-form', '.', true, nodeCallback, true);

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
					self.close();
				},
				error : function(model, xhr, options) {
					console.log(xhr);
					alert(xhr.responseText);
				}
			});
		},

		close : function() {
			this.$el.find('.modal').modal('hide');
		},

		hidden : function() {
			this.undelegateEvents();
			app.routes.navigate('users', {trigger : true});
		},

		show : function() {
			this.$el.find('.modal').modal('show');
		}
	})
});
