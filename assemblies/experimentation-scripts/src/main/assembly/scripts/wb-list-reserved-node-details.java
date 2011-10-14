//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String nodeTypesString              = System.getProperty("testbed.nodetypes");
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");

	Iterable nodeTypes                  = Splitter.on(",").trimResults().omitEmptyStrings().split(nodeTypesString);

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

	List nodes = WiseMLHelper.getNodes(wiseML, nodeTypes);
	System.out.println(Joiner.on("\n").join(WiseMLHelper.FUNCTION_NODE_LIST_TO_STRING_LIST.apply(nodes)));
