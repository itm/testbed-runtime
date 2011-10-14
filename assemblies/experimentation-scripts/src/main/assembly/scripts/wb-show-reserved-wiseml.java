//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	System.out.println(secretReservationKeys);

	String wsnEndpointURL = null;
	try {
		wsnEndpointURL = sessionManagement.getInstance(
				helper.parseSecretReservationKeys(secretReservationKeys),
				"NONE"
		);

	} catch (UnknownReservationIdException_Exception e) {
		log.warn("There was no reservation found with the given secret reservation key. Exiting.");
		System.exit(1);
	}

	WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
	String wiseML = wsnService.getNetwork();

	System.out.println(WiseMLHelper.prettyPrintWiseML(wiseML));
