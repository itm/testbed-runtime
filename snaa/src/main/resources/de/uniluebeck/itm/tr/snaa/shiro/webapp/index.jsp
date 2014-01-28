<%@ page import="de.uniluebeck.itm.tr.snaa.SNAAServiceConfig" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>SNAA Users Backend</title>
    <link rel="stylesheet" href="lib/css/bootstrap.css"/>
    <link rel="stylesheet" href="css/app.css"/>
    <script src="lib/js/jquery-1.10.2.min.js"></script>
    <script src="lib/js/bootstrap.js"></script>
    <script src="lib/js/bootbox-3.2.0.js"></script>
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

		app.shiroAdminRestApiContextPath = "<%= getServletConfig().getInitParameter(SNAAServiceConfig.SHIRO_ADMIN_REST_API_CONTEXTPATH) %>";
		app.shiroAdminWebappContextPath = "<%= getServletConfig().getInitParameter(SNAAServiceConfig.SHIRO_ADMIN_WEBAPP_CONTEXTPATH) %>";

		$(document).ready(function() {
			'use strict';
            app.routes = new app.Router();

            app.Users.fetch({
                success: function() {
                    Backbone.history.start();
                },
                error: function() {
                    console.error('Error fetching data');
                }
            });

            $('#btnUserAdd').click(function() {
                app.detail = new app.AddUserView({
                    el: jQuery("#edit-view"),
                    model: new app.UserModel()
                });
            });
        });
        
    </script>
</head>
<body>
<div id="edit-view"></div>
</body>
</html>