//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	Splitter csvSplitter                = Splitter.on(",").trimResults().omitEmptyStrings();

	List urnPrefixes					= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.urnprefixes")));
    List usernames						= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.usernames")));
    List passwords						= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.passwords")));

	Preconditions.checkArgument(
		urnPrefixes.size() == usernames.size() && usernames.size() == passwords.size(),
		"The list of URN prefixes must have the same length as the list of usernames and the list of passwords"
	);

    String snaaEndpointURL 				= System.getProperty("testbed.snaa.endpointurl");
    String rsEndpointURL				= System.getProperty("testbed.rs.endpointurl");
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");

	Integer offset						= System.getProperty("testbed.offset") == null || "".equals(System.getProperty("testbed.offset")) ? 0 : Integer.parseInt(System.getProperty("testbed.offset"));
	Integer duration					= Integer.parseInt(System.getProperty("testbed.duration"));
	String nodeURNs						= System.getProperty("testbed.nodeurns");

	SNAA authenticationSystem 			= SNAAServiceHelper.getSNAAService(snaaEndpointURL);
	RS reservationSystem				= RSServiceHelper.getRSService(rsEndpointURL);
	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL); 

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	List credentialsList = new ArrayList();
	for (int i=0; i<urnPrefixes.size(); i++) {
		
		AuthenticationTriple credentials = new AuthenticationTriple();
		
		credentials.setUrnPrefix(urnPrefixes.get(i));
		credentials.setUsername(usernames.get(i));
		credentials.setPassword(passwords.get(i));
		
		credentialsList.add(credentials);
	}

	// do the authentication
	log.debug("Authenticating...");
	List secretAuthenticationKeys = authenticationSystem.authenticate(credentialsList);
	log.debug("Successfully authenticated!");




    //--------------------------------------------------------------------------
    // 2nd step: wb-reserve some nodes (here: all nodes)
    //--------------------------------------------------------------------------

	// retrieve the node URNs of all iSense nodes
	String serializedWiseML = sessionManagement.getNetwork();
	List nodeURNsToReserve;
	if (nodeURNs == null || "".equals(nodeURNs)) {
		nodeURNsToReserve = WiseMLHelper.getNodeUrns(serializedWiseML, new String[] {});
	} else {
		nodeURNsToReserve = Lists.newArrayList(csvSplitter.split(nodeURNs));
	}
	log.debug("Retrieved the node URNs of all iSense nodes: {}", Joiner.on(", ").join(nodeURNsToReserve));

	// create reservation request data to wb-reserve all iSense nodes for 10 minutes
	ConfidentialReservationData reservationData = helper.generateConfidentialReservationData(
			nodeURNsToReserve,
			new Date(System.currentTimeMillis() + (offset*60*1000)), duration, TimeUnit.MINUTES,
			urnPrefixes, usernames
	);

	// do the reservation
	log.debug("Trying to reserve the following nodes: {}", nodeURNsToReserve);
    try {
    	
    	List secretReservationKeys = reservationSystem.makeReservation(
    			helper.copySnaaToRs(secretAuthenticationKeys),
    			reservationData
    	);
		log.debug("Successfully reserved nodes: {}", nodeURNsToReserve);
		log.debug("Reservation Key(s): {}", helper.toString(secretReservationKeys));
		
		System.out.println(helper.toString(secretReservationKeys));
		
    } catch (ReservervationConflictExceptionException e) {
    	System.err.println("" + e);
    	System.exit(1);
    }
