//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String nodeTypesString = System.getProperty("testbed.nodetypes");
	Iterable nodeTypes = Splitter.on(",").trimResults().omitEmptyStrings().split(nodeTypesString);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
	List nodes = WiseMLHelper.getNodes(sessionManagement.getNetwork(), nodeTypes);

	System.out.println(Joiner.on("\n").join(WiseMLHelper.FUNCTION_NODE_LIST_TO_STRING_LIST.apply(nodes)));
