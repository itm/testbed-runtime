var app = app || {};

$(function () {
    'use strict';

    app.TableView = Backbone.View.extend({

        template: Handlebars.getTemplate('row'),

        events: {
            'click a[data-confirm]' : 'delClicked',
            'click a.node-edit'     : 'editClicked'
        },

        initialize: function() {
            app.Nodes.fetch();

            // changes in our collection will redraw view
            this.listenTo(app.Nodes, 'all', this.render);

            this.render();
        },

        render: function() {
            var seed = { configs: _.map(app.Nodes.models, function(el) {
                return el.attributes;
            }) };
            this.$el.html(this.template(seed));

            return this;
        },

        delClicked: function(e) {
            e.preventDefault();
            var id = $(e.target).parents('tr').data('id');
            var msg = $(e.target).data('confirm') || $(e.target).parent().data('confirm');
            bootbox.confirm(msg, function(sure) {
                if ( sure ) {
                    app.Nodes.get(id).destroy();
                }
            });
        },

        editClicked: function(e) {
            e.preventDefault();
            var id = $(e.target).parents('tr').data('id');
            new app.DetailView({
                el:     jQuery("#edit-view"),
                model:  app.Nodes.get(id)
            });
        }

    });


    app.DetailView = Backbone.View.extend({
        template: Handlebars.getTemplate('modal'),
        events: {
            'click #save' : 'save',
            'click #close': 'hide'
        },
        initialize: function() {
            this.render();
        },

        render: function() {
            this.$el.html(this.template(this.model.attributes));
            this.show();
            return this;
        },

        save: function() {
            var self = this;
            var nodeCallback = function(node) {
                if ( node.id && node.id=='gatewayNode' )  {
                    return { name: 'gatewayNode', value: $(node).is(':checked')};
                }
            };
            // serialize form
            var data = form2js('modal-form','.',true,nodeCallback,true);
            // send to server
            var isNew = this.model.id === this.model.defaults.nodeUrn;
            this.model.save(data, {
                wait: true,
                success: function(model, response, options) {
                    app.Nodes.add(self.model, {merge: true});
                    self.hide();
                },
                error: function(model, xhr, options) {
                    alert(xhr.responseText);
                },
                type: isNew ? 'post' : 'put'
            });
        },

        hide: function() {
            this.$el.find('.modal').modal('hide');
            this.undelegateEvents();
        },
        show: function() {
            this.$el.find('.modal').modal('show');
        }
    });

});