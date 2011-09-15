//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
	System.out.println(WiseMLHelper.prettyPrintWiseML(sessionManagement.getNetwork()));
