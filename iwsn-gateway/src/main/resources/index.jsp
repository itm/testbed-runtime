<%@ page import="de.uniluebeck.itm.tr.iwsn.gateway.GatewayDevice" %>
<%@ page import="de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceManager" %>
<%@ page import="java.util.Map" %>
<%
	@SuppressWarnings("unchecked")
	final Map<String, Object> model = (Map<String, Object>) pageContext.findAttribute("it");
	final GatewayDeviceManager gatewayDeviceManager = (GatewayDeviceManager) model.get("gatewayDeviceManager");
%>

<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8"/>
	<title>Gateway <%= request.getServerName() %>
	</title>
</head>
<body>

<h1>Gateway <%= request.getServerName() %></h1>

<h3>Currently connected devices</h3>
<%
	for (GatewayDevice device : gatewayDeviceManager.getDevices()) {
		out.write(device.getNodeUrn().toString());
		out.write("<br/>");
	}
%>

</body>
</html>