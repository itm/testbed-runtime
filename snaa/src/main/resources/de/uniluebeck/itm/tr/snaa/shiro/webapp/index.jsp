<%@ page import="static de.uniluebeck.itm.tr.snaa.SNAAServiceConfig.SHIRO_ADMIN_REST_API_CONTEXTPATH" %>
<%@ page import="static de.uniluebeck.itm.tr.snaa.SNAAServiceConfig.SHIRO_ADMIN_WEBAPP_CONTEXTPATH" %>
<%@ page import="static de.uniluebeck.itm.tr.snaa.SNAAServiceConfig.SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8"/>
	<title>SNAA Users Backend</title>
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

		app.rest_api_context_path = "<%= getServletConfig().getInitParameter(SHIRO_ADMIN_REST_API_CONTEXTPATH) %>";
		app.webapp_context_path   = "<%= getServletConfig().getInitParameter(SHIRO_ADMIN_WEBAPP_CONTEXTPATH) %>";
		app.device_db_rest_api_context_path = "<%= getServletConfig().getInitParameter(SHIRO_ADMIN_DEVICE_DB_REST_API_CONTEXTPATH) %>";

		$(document).ready(function() {
			'use strict';

			app.routes = new app.Router();

			// set up that clicks on bootstrap tabs will trigger routing in backbone.js
			$('#navigation a[data-toggle="tab"]').on('show.bs.tab', function(e) {
				app.routes.navigate(e.target.attributes['data-nav'].value, {trigger : true});
			});

			// set up that route events in backbone.js will trigger clicks in bootstrap tabs
			Backbone.history.on('route', function(router, event) {
				$('#navigation a[href="#'+event+'"]').click();
			});

			Backbone.history.start();
		});

	</script>
</head>
<body>
<div id="edit-view"></div>
<div class="container">
	<h1>SNAA Users Backend</h1>
	<ul class="nav nav-tabs" id="navigation">
		<li><a href="#users" data-toggle="tab" data-nav="users">Users</a></li>
		<li><a href="#roles" data-toggle="tab" data-nav="roles">Roles</a></li>
		<li><a href="#actions" data-toggle="tab" data-nav="actions">Actions</a></li>
		<li><a href="#resource_groups" data-toggle="tab" data-nav="resource_groups">Resource Groups</a></li>
		<li><a href="#permissions" data-toggle="tab" data-nav="permissions">Permissions</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane" id="users"></div>
		<div class="tab-pane" id="roles"></div>
		<div class="tab-pane" id="actions"></div>
		<div class="tab-pane" id="resource_groups"></div>
		<div class="tab-pane" id="permissions"></div>
	</div>
</div>
</body>
</html>