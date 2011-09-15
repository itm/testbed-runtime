//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
	sessionManagement.free(helper.parseSecretReservationKeys(secretReservationKeys));
