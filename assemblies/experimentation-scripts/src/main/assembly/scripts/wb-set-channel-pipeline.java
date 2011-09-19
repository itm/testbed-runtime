//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8089/controller";
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");
	String messageToSend                = System.getProperty("testbed.message");
	String selectedNodeUrns             = System.getProperty("testbed.nodeurns");
	boolean csv                         = System.getProperty("testbed.listtype") != null && "csv".equals(System.getProperty("testbed.listtype"));

	String configFileName               = System.getProperty("testbed.configfile");
	File configFile               = new File(configFileName);

	if (!configFile.exists()) {
		throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" does not exist!");
	} else if (configFile.isDirectory()) {
		throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" is a directory!");
	} else if (!configFile.canRead()) {
		throw new RuntimeException("Configuration file \"" + configFile.getAbsolutePath() + "\" can't be read!");
	}

	String protobufHost                 = System.getProperty("testbed.protobuf.hostname");
	String protobufPortString           = System.getProperty("testbed.protobuf.port");
	Integer protobufPort                = protobufPortString == null ? null : Integer.parseInt(protobufPortString);
	boolean useProtobuf                 = protobufHost != null && protobufPort != null;

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	// create a connection with the testbed

	WisebedClientBase client = null;
	WSNAsyncWrapper wsn = null;

	if (useProtobuf) {

		try {

			client = new WisebedProtobufClient(sessionManagementEndpointURL, protobufHost, protobufPort);
			wsn = client.connectToExperiment(helper.parseSecretReservationKeys(secretReservationKeys)).get();

		} catch (Exception e) {
			useProtobuf = false;
		}

	}

	if (!useProtobuf) {

		client = new WisebedClient(sessionManagementEndpointURL);
		wsn = client.connectToExperiment(helper.parseSecretReservationKeys(secretReservationKeys)).get();

	}

	// determine node set onto which to set channel pipeline configuration

	List nodeURNs;
	if (selectedNodeUrns != null && "portal".equalsIgnoreCase(selectedNodeUrns.trim())) {
		nodeURNs = Lists.newArrayList();
		log.debug("Selected the portal server");
	} else if (selectedNodeUrns != null && !"".equals(selectedNodeUrns.trim())) {
		nodeURNs = Lists.newArrayList(selectedNodeUrns.split(","));
		log.debug("Selected the following node URNs: {}", nodeURNs);
	} else {
		nodeURNs = WiseMLHelper.getNodeUrns(wsn.getNetwork().get(), new String[]{});
		log.debug("Retrieved the following node URNs: {}", nodeURNs);
	}

	// load channel pipeline configuration

	List channelPipelineConfiguration = ChannelPipelineHelper.loadConfiguration(configFile);

	// execute setting of the channel pipeline configuration

	Future jobResultFuture = wsn.setChannelPipeline(
			nodeURNs,
			channelPipelineConfiguration,
			30, TimeUnit.SECONDS
	);

	try {

		JobResult jobResult = jobResultFuture.get();
		jobResult.printResults(System.out, csv);

		client.shutdown();
		System.exit(0);

	} catch (ExecutionException e) {
		if (e.getCause() instanceof TimeoutException) {
			log.info("Call timed out. Exiting...");
		}
		System.exit(1);
	}