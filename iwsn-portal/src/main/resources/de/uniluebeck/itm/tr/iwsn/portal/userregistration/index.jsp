<%@ page import="de.uniluebeck.itm.tr.iwsn.portal.UserRegistrationWebAppService" %>
<%@ page import="de.uniluebeck.itm.tr.common.Constants" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8"/>
	<title>User Registration</title>
	<link rel="stylesheet" href="lib/css/bootstrap.css"/>
	<link rel="stylesheet" href="css/app.css"/>
	<script src="lib/js/jquery-1.10.2.min.js"></script>
	<script src="lib/js/bootstrap-3.1.0.js"></script>
	<script src="lib/js/bootbox-4.1.0.js"></script>
	<script src="lib/js/handlebars-1.0.0.js"></script>
	<script src="lib/js/underscore-1.4.4.js"></script>
	<script src="lib/js/backbone-1.1.0.js"></script>
	<script src="lib/js/form2js.js"></script>
	<script src="js/routes.js"></script>
	<script src="js/models.js"></script>
	<script src="js/views.js"></script>
	<script src="js/templates.js"></script>
	<script>

		var app = app || {};

		app.rest_api_context_path =
				"<%= getServletConfig().getInitParameter(Constants.REST_API_V1.REST_API_CONTEXT_PATH_KEY) %>";

		$(document).ready(function() {
			'use strict';
			app.routes = new app.Router();
			Backbone.history.start();
		});

	</script>
</head>
<body>
<div class="container"></div>
</body>
</html>