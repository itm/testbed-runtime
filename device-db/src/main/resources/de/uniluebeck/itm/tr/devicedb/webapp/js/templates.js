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