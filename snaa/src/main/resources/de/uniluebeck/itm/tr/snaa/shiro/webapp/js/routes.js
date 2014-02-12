var app = app || {};

$(function() {

	app.Router = Backbone.Router.extend({

		routes : {
			'users' : 'users',
			'users/:name' : 'edit_user',
			'roles' : 'roles',
			'actions' : 'actions',
			'resource_groups' : 'resource_groups',
			'resource_groups/:name' : 'edit_resource_group',
			'permissions' : 'permissions',
			'.*' : 'users'
		},

		initialize : function() {

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

		resource_groups : function() {
			var self = this;
			self.fetchResourceGroups({
				success : function() {
					self.fetchNodes({
						success : function() {
							app.resourceGroupsView = app.resourceGroupsView || new app.ResourceGroupsView({
								el : $("div.tab-content div#resource_groups")
							});
						}
					});
				}
			});
		},

		edit_resource_group : function(name) {
			var self = this;
			self.fetchResourceGroups({
				success : function() {
					self.fetchNodes({
						success : function() {
							var view = new app.EditResourceGroupView({
								el : $("#edit-view"),
								model : {
									resourceGroup : app.ResourceGroups.get(name),
									availableNodes : app.Nodes
								}
							});
							view.show();
						}
					});
				}
			});
		},

		permissions : function() {
			this.fetchRoles();
			this.fetchResourceGroups();
			this.fetchActions();
			this.fetchPermissions();
			app.permissionsView = app.permissionsView || new app.PermissionsView({
				el : $("div.tab-content div#permissions")
			});
		},

		edit_user : function(name) {
			var view = new app.EditUserView({
				el : $("#edit-view"),
				model : {
					user : app.Users.get(name),
					roles : app.Roles
				}
			});
			view.show();
		},

		fetchActions : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching actions list');
			};
			app.Actions.fetch(options);
		},

		fetchPermissions : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching permissions');
			};
			app.Permissions.fetch(options);
		},

		fetchRoles : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching roles list');
			};
			app.Roles.fetch(options);
		},

		fetchUsers : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching users list');
			};
			app.Users.fetch(options);
		},

		fetchResourceGroups : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching resource groups');
			};
			app.ResourceGroups.fetch(options);
		},

		fetchNodes : function(options) {
			options = options || {};
			options.error = function() {
				alert('Error fetching nodes from DeviceDB');
			};
			app.Nodes.fetch(options);
		}
	});
});
