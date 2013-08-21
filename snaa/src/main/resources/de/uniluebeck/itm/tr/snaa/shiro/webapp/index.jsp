<%@ page import="de.uniluebeck.itm.tr.snaa.SNAAServiceConfig" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>Device DB</title>
    <link rel="stylesheet" href="lib/css/bootstrap.css"/>
    <link rel="stylesheet" href="css/app.css"/>
    <script src="lib/js/jquery-1.9.1.js"></script>
    <script src="lib/js/bootstrap.js"></script>
    <script src="lib/js/bootbox-3.2.0.js"></script>
    <script src="lib/js/handlebars-1.0.0-rc.3.js"></script>
    <script src="lib/js/underscore-1.4.4.js"></script>
    <script src="lib/js/backbone-0.9.10.js"></script>
    <script src="lib/js/form2js.js"></script>
    <script src="js/routes.js"></script>
    <script src="js/models.js"></script>
    <script src="js/views.js"></script>
    <script src="js/templates.js"></script>   
    <script>

		var deviceDBRestApiContextPath = "<%= getServletConfig().getInitParameter(SNAAServiceConfig.SHIRO_ADMIN_REST_API_CONTEXTPATH) %>";
		var deviceDBWebappContextPath = "<%= getServletConfig().getInitParameter(SNAAServiceConfig.SHIRO_ADMIN_WEBAPP_CONTEXTPATH) %>";

		$(document).ready(function() {
			'use strict';
            app.routes = new app.Router();

            app.Nodes.fetch({
                success: function() {
                    Backbone.history.start();
                },
                error: function() {
                    console.error('Error fetching data');
                }
            });

            $('#btnAdd').click(function() {
                app.detail = new app.DetailView({
                    el:     jQuery("#edit-view"),
                    model: new app.NodeModel()
                });
            });
        });
        
    </script>
</head>
<body>
<div id="edit-view"></div>
<div class="container">
    <h1>Device DB</h1>
    <p id="btnAdd" class="pull-right">
        <a class="btn btn-info btn-small">
            <i class="icon-plus-sign icon-white"></i>
             Add
        </a>
    </p>
    <table class="table table-striped table-bordered table-hover table-condensed">
        <thead>
        <tr>
            <th>URN</th>
            <th>Type</th>
            <th>Gateway</th>
            <th>Serial Port</th>
            <th>USB Chip ID</th>
            <th>Timeouts</th>
            <th></th>
        </tr>
        </thead>
        <tbody id="table_configs"></tbody>
    </table>
</div>
</body>
</html>