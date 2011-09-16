//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	System.out.println(WiseMLHelper.prettyPrintWiseML(sessionManagement.getNetwork()));
