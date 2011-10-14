//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
    String listType                     = System.getProperty("testbed.listtype");

	Iterable nodeTypes                  = Splitter.on(",").trimResults().omitEmptyStrings().split(System.getProperty("testbed.nodetypes"));

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	log.debug("Calling SessionManagement.getNetwork() on {}", sessionManagementEndpointURL);
	String wiseML = sessionManagement.getNetwork();
	List nodes = WiseMLHelper.getNodeUrns(wiseML, nodeTypes);

	if (listType != null && "lines".equals(listType)) {
		System.out.println(Joiner.on("\n").join(nodes));
	} else {
		System.out.println(Joiner.on(",").join(nodes));
	}
