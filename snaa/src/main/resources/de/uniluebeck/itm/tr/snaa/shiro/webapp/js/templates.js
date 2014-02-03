// lazy load and compile templates via ajax
Handlebars.getTemplate = function(name) {
	if (Handlebars.templates === undefined || Handlebars.templates[name] === undefined) {
		$.ajax({
			url : 'tpl/' + name + '.handlebars',
			success : function(data) {
				if (Handlebars.templates === undefined) {
					Handlebars.templates = {};
				}
				Handlebars.templates[name] = Handlebars.compile(data);
			},
			async : false
		});
	}
	return Handlebars.templates[name];
};

// ajax external partial
Handlebars.registerExternalPartial = function(name) {
	if (Handlebars.partials === undefined || Handlebars.partials[name] === undefined) {
		$.ajax({
			url : 'tpl/' + name + '.handlebars',
			success : function(data) {
				Handlebars.registerPartial(name, Handlebars.compile(data));
			},
			async : false
		});
	}
	return Handlebars.partials[name];
};

// use before nested loops.
Handlebars.registerHelper('setOuterIndex', function(value) {
	this.outerIndex = Number(value);
});
// Use {{outerIndex}} to acces outer index
Handlebars.registerHelper('eachWithOuter', function(context, outerIndex, options) {
	if (context) {
		$.each(context, function(idx, val) {
			val.outerIndex = outerIndex;
		});
	}

	return Handlebars.helpers['each'].call(this, context, options);
});

Handlebars.registerHelper('selected', function(foo, bar) {
	return foo == bar ? ' selected' : '';
});

Handlebars.registerHelper('hasRole', function(user, role, options) {
	if (user && user.roles) {
		for (var i=0; i<user.roles.length; i++) {
			if (user.roles[i].name == role.attributes.name) {
				return options.fn(this);
			}
		}
	}
	return options.inverse(this);
});