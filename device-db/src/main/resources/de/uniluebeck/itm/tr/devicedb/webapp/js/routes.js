var app = app || {};

$(function () {

	app.Router = Backbone.Router.extend({
		routes: {
			'home' : 'home'
		},

		initialize: function() {
			// match every url starting with 'urn'
			this.route(/^(urn.*)$/, "openDetail");
			// match the rest
			this.route(/^(?!urn).*/, 'home');
		},

		home: function() {
			app.table = app.table || new app.TableView({
				el: $("#table_configs")
			});
			app.table.show();
		},

		openDetail: function(nodeUrn) {
			if ( !app.detailView || app.detailView.model.id != nodeUrn) {
				app.detailView = new app.DetailView({
					el:     jQuery("#edit-view"),
					model:  app.Nodes.get(nodeUrn)
				});
			}
			if (app.table) app.table.hide();
			app.detailView.show();
		}
	});

});