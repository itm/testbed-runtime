var app = app || {};

$(function () {
    'use strict';

    app.MainView = Backbone.View.extend({

        template: Handlebars.getTemplate('main'),

        events: {

        },

        initialize: function() {
            this.render();
        },

        render: function() {
            this.$el.append(this.template());
            return this;
        }

    });

    app.TableView = Backbone.View.extend({

        template: Handlebars.getTemplate('row'),

        events: {
            'click a.user-edit'     : 'editClicked',
			'click a.user-remove'   : 'delClicked'
		},

        initialize: function() {
            // changes in our collection will redraw view
            this.listenTo(app.Users, 'all', this.render);

            this.render();
        },

        render: function() {
            var seed = { users: _.map(
					app.Users.models,
					function(el) { return el.attributes; })
			};
            this.$el.html(this.template(seed));
            return this;
        },

        delClicked: function(e) {
            e.preventDefault();
            var id = $(e.target).parents('tr').data('id');
            var msg = $(e.target).data('confirm') || $(e.target).parent().data('confirm');
            bootbox.confirm(msg, function(sure) {
                if ( sure ) {
                    app.Users.get(id).destroy();
                }
            });
        },

        editClicked: function(e) {
            e.preventDefault();
            var id = $(e.target).parents('tr').data('id');
            app.routes.navigate('users/'+id, {trigger:true});
        },

        show: function() {
            app.routes.navigate('users');
            this.$el.show();
        },

        hide: function() {
            this.$el.hide();
        }

    });

    app.AddUserView = Backbone.View.extend({

        template: Handlebars.getTemplate('add-user'),

        events: {
            'click #save'           : 'save',
            'click #close'          : 'close',
            'hidden'                : 'hidden'
        },

        initialize: function() {
            this.render();
        },

        render: function() {
            this.$el.html(this.template(this.model.attributes));
            this.show();
            app.routes.navigate(this.model.get('name'));
            return this;
        },

        save: function(e) {
            e.preventDefault();
            var self = this;

            // serialize form
            var nodeCallback = function(node) {

            };
            var data = form2js('modal-form', '.', true, nodeCallback, true);

            // send to server
            this.model.save(data, {
                wait: true,
                success: function(model, response, options) {
                    app.Users.add(self.model, {merge: true});
                    self.close(e);
                },
                error: function(model, xhr, options) {
                    alert(xhr.responseText);
                }
            });
        },

        close: function(e) {
            e.preventDefault();
            this.$el.find('.modal').modal('hide');
        },

        hidden: function() {
            this.undelegateEvents();
            app.routes.navigate('home',{trigger:true});
        },

        show: function() {
            this.$el.find('.modal').modal('show');
        }

    });

});