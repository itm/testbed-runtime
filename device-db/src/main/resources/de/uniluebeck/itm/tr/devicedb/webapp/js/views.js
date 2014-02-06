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
            app.routes.navigate(id,{trigger:true});
        },

        show: function() {
            app.routes.navigate('home');
            this.$el.show();
        },
        hide: function() {
            this.$el.hide();
        }

    });


    app.DetailView = Backbone.View.extend({

        template: Handlebars.getTemplate('modal'),
        paramTpl: Handlebars.getTemplate('param-text-input'),
        capTpl:   Handlebars.getTemplate('cap-text-inputs'),
        pipeTpl:  Handlebars.getTemplate('pipeline-text-inputs'),
        pipeParamTpl:  Handlebars.getTemplate('pipeline-param-input'),

        events: {
            'click #save'           : 'save',
            'click #close'          : 'close',
            'click #add-param'      : 'addParam',
            'click #add-cap'        : 'addCapability',
            'click #add-pipe-param' : 'addPipeParam',
            'click .rm-param'       : 'rmParam',
            'click .rm-pipe-param'  : 'rmPipeParam',
            'click .rm-pipe'        : 'rmPipeElem',
            'click #add-pipe'       : 'addPipeElem',
            'click .up-pipe'        : 'pipeElemUp',
            'click .down-pipe'      : 'pipeElemDown',
            'click .rm-cap'         : 'rmCapability',
            'hidden'                : 'hidden'
        },

        initialize: function() {
            Handlebars.registerExternalPartial('param-text-input');
            Handlebars.registerExternalPartial('cap-text-inputs');
            Handlebars.registerExternalPartial('pipeline-text-inputs');
            Handlebars.registerExternalPartial('pipeline-param-input');
            this.render();
        },

        render: function() {
            this.$el.html(this.template(this.model.attributes));
            this.show();
            app.routes.navigate(this.model.get('nodeUrn'));
            return this;
        },

        save: function(e) {
            e.preventDefault();
            var self = this;
            var nodeCallback = function(node) {
                if ( node.id && node.id=='gatewayNode' )  {
                    return { name: 'gatewayNode', value: $(node).is(':checked')};
                }
            };
            // serialize form
            var data = form2js('modal-form','.',true,nodeCallback,true);
            // init arrays if they are empty
            data.defaultChannelPipeline = data.defaultChannelPipeline || [];
            data.capabilities = data.capabilities || [];
            data.nodeConfiguration = data.nodeConfiguration || [];

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
                    self.close(e);
                },
                error: function(model, xhr, options) {
					console.log(model);
					console.log(xhr);
					console.log(options);
                    alert(xhr.responseText);
                },
                type: isNew ? 'post' : 'put'
            });
        },

        _rmParentClass: function(e, element) {
            e.preventDefault();
            $(e.target).parents(element).remove();
        },

        _getMaxParamIdx: function() {
            var max = -1;
            $('#params [name]').each(function(idx,val){
                // parse number e.g. '1' from 'nodeConfiguration[1]'
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
            this._rmParentClass(e, '.parent');
        },

        _getMaxCapIdx: function() {
            var max = -1;
            $('#capabilities [name]').each(function(idx,val){
                // parse number e.g. '1' from 'capabilities[1]'
                $(val).attr('name').match(/capabilities\[(\d+)\]/);
                var thisIdx = parseInt(RegExp.$1, 10);
                max =  thisIdx > max ? thisIdx : max;
            });
            return max;
        },
        addCapability: function(e) {
            e.preventDefault();
            var cap = this.capTpl({
                'name' : '',
                'defaultValue': '',
                'unit': '',
                'datatype': ''
            }, {
                data: {
                    index: this._getMaxCapIdx()+1
                }
            });
            $('#capabilities').append(cap);
        },
        rmCapability: function(e) {
            this._rmParentClass(e, '.parent');
        },


        _getMaxPipeParamIdx: function(handlerIdx) {
            var max = -1;
            $('#pipeline .parent[data-idx="'+handlerIdx+'"] [name]').each(function(idx,val){
                // parse number e.g. '1' from 'defaultChannelPipeline[1]'
                $(val).attr('name').match(/configuration\[(\d+)\]/);
                var thisIdx = parseInt(RegExp.$1, 10);
                max =  thisIdx > max ? thisIdx : max;
            });
            return max;
        },
        addPipeParam: function(e) {
            e.preventDefault();
            var curHandler = Number($(e.target).parents('.parent').data('idx'));
            var param = this.pipeParamTpl({
                'outerIndex': curHandler,
                'key' : '',
                'value': ''
            }, {
                data: {
                    index: this._getMaxPipeParamIdx(curHandler)+1
                }
            });
            $('#pipeline .parent[data-idx="'+curHandler+'"] .pipeline-params').append(param);
        },

        rmPipeParam: function(e) {
            this._rmParentClass(e, '.control-group');
        },

        _getMaxPipeElemIdx: function() {
            var max = -1;
            $('#pipeline .parent').each(function(idx,val){
                var thisIdx = Number($(val).data('idx'));
                max =  thisIdx > max ? thisIdx : max;
            });
            return max;
        },
        addPipeElem: function(e) {
            e.preventDefault();
            var handler = this.pipeTpl({
                'handlerName'  : '',
                'instanceName' : '',
                'configuration': []
            }, {
                data: {
                    index: this._getMaxPipeElemIdx()+1
                }
            });
            $('#pipeline').append(handler);
        },
        rmPipeElem: function(e) {
            this._rmParentClass(e, '.parent');
        },

        pipeElemUp: function(e) {
            e.preventDefault();
            var elem = $(e.target).parents('.parent');
            elem.after(elem.prev());
        },

        pipeElemDown: function(e) {
            e.preventDefault();
            var elem = $(e.target).parents('.parent');
            elem.after(elem.next());
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
            this.delegateEvents();
            this.$el.find('.modal').modal('show');
        }
    });

});