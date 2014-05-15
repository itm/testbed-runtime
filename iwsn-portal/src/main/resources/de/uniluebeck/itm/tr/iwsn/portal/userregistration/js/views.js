var app = app || {};

$(function() {
	'use strict';

	app.ThankYouView = Backbone.View.extend({

		template : Handlebars.getTemplate('thank-you'),

		events : {

		},

		initialize : function() {
			this.render();
		},

		render : function() {
			this.$el.html(this.template(this.model));
			return this;
		},

		show : function() {
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});

	app.EditUserView = Backbone.View.extend({

		template : Handlebars.getTemplate('user-edit'),

		events : {
			'click button.edit-user-save' : 'clickedSave',
			'keypress input' : 'saveIfEnter',
			'change input#email' : 'checkEmail',
			'input input#password' : 'checkPasswordMatch',
			'input input#repeat_password' : 'checkPasswordMatch'
		},

		initialize : function() {
			this.render();
			if (this.model.isNew()) {
				this.$el.find('input#email').attr("autofocus", "autofocus");
			} else {
				this.$el.find('input#old_password').attr("autofocus", "autofocus");
			}
		},

		render : function() {
			this.$el.html(this.template(this.model));
			return this;
		},

		saveIfEnter : function(e) {
			var code = e.keyCode || e.which;
			if(code == 13 && !this.formHasErrors()) {
				this.clickedSave();
			}
		},

		clickedSave : function() {

			var data = {
				email : this.$el.find('input#email').val(),
				password : this.$el.find('input#password').val()
			};

			if (!this.model.isNew()) {
				data.oldPassword = this.$el.find('input#old_password').val()
			}

			var self = this;
			this.model.save(data, {
				wait : true,
				success : function(model, response, options) {
					app.routes.navigate(data.email + '/thank_you', {trigger : true});
				},
				error : function(model, xhr, options) {
					if (xhr.status == 401) {
						self.$el.find('input#old_password').parent().toggleClass('has-error', true);
					} else {
						console.log(xhr);
						alert(xhr.responseText);
					}
				}
			});
		},

		checkEmail : function() {

			var emailValid = this.isValidEmailAddress();

			this.$el.find('input#email').parent().toggleClass('has-error', !emailValid);
			this.$el.find('button.edit-user-save').prop('disabled', !emailValid);
		},

		checkPasswordMatch : function() {

			var passwordMismatch = !this.passwordsMatch();

			this.$el.find('input#password').parent().toggleClass('has-error', passwordMismatch);
			this.$el.find('input#repeat_password').parent().toggleClass('has-error', passwordMismatch);
			this.$el.find('button.edit-user-save').prop('disabled', passwordMismatch);
		},

		formHasErrors : function() {
			return !this.isValidEmailAddress() || !this.passwordsMatch();
		},

		passwordsMatch : function() {
			return this.$el.find('input#password').val() == this.$el.find('input#repeat_password').val();
		},

		isValidEmailAddress : function() {
			var pattern = new RegExp(/^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i);
			return pattern.test(this.$el.find('input#email').val());
		},

		show : function() {
			this.$el.show();
		},

		hide : function() {
			this.$el.hide();
		}
	});
});
