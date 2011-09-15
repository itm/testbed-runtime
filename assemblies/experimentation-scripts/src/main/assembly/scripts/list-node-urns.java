//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	Iterable nodeTypes = Splitter.on(",").trimResults().omitEmptyStrings().split(System.getProperty("testbed.nodetypes"));
    String listType = System.getProperty("testbed.listtype");

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
	List nodes = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork(), nodeTypes);

	if (listType != null && "lines".equals(listType)) {
		System.out.println(Joiner.on("\n").join(nodes));
	} else {
		System.out.println(Joiner.on(",").join(nodes));
	}
