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
        paramTpl: Handlebars.getTemplate('text-input'),
        events: {
            'click #save'       : 'save',
            'click #close'      : 'hide',
            'click #add-param'  : 'addParam',
            'click #rm-param'   : 'rmParam',
            'hidden'            : 'undelegateEvents'
        },
        initialize: function() {
            Handlebars.registerExternalPartial('text-input');
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
            var isNew = this.model.id === this.model.defaults.nodeUrn;
            // directly set URN if new
            if ( isNew ) {
                this.model.set("nodeUrn", data.nodeUrn);
            }
            // send to server
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

        _getMaxParamIdx: function() {
            var max = -1;
            $('#params [name]').each(function(idx,val){
                $(val).attr('name').match(/nodeConfiguration\[(\d+)\]/);
                var thisIdx = parseInt(RegExp.$1, 10);
                max =  thisIdx > max ? thisIdx : max;
            });
            return max;
        },

        addParam: function(e) {
            var param = this.paramTpl({
                'key' : '',
                'value': ''
            }, {
                data: {
                    index: this._getMaxParamIdx()+1
                }
            });
            $('#params').append(param);
        },
        rmParam: function(e) {
            $(e.target).parents('.control-group').remove();
        },

        hide: function() {
            this.$el.find('.modal').modal('hide');
        },
        show: function() {
            this.$el.find('.modal').modal('show');
        }
    });

});