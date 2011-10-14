//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	sessionManagement.free(helper.parseSecretReservationKeys(secretReservationKeys));
