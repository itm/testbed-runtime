//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
    String listType                     = System.getProperty("testbed.listtype");
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");

	Iterable nodeTypes                  = Splitter.on(",").trimResults().omitEmptyStrings().split(System.getProperty("testbed.nodetypes"));

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

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

	List nodes = WiseMLHelper.getNodeUrns(wiseML, nodeTypes);

	if (listType != null && "lines".equals(listType)) {
		System.out.println(Joiner.on("\n").join(nodes));
	} else {
		System.out.println(Joiner.on(",").join(nodes));
	}
