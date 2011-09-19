//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8089/controller";
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	// create a connection with the testbed

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

	WSN wsn = WSNServiceHelper.getWSNService(wsnEndpointURL);

    List channelHandlerDescriptions = wsn.getSupportedChannelHandlers();

	for (ChannelHandlerDescription chd : channelHandlerDescriptions) {

		System.out.println("ChannelHandler {");
		System.out.println("\tname=\"" + chd.getName() + "\"");
		System.out.println("\tdescription=\"" + chd.getDescription() + "\"");
		System.out.print("\tconfigurationOptions={");
		if (chd.getConfigurationOptions().size() > 0) {
			System.out.println();
			for (KeyValuePair keyValuePair : chd.getConfigurationOptions()) {
				System.out.println("\t\tkey=\"" +
						keyValuePair.getKey() +
						"\", description=\"" +
						keyValuePair.getValue() + "\""
				);
			}
			System.out.println("\t}");
		} else {
			System.out.println("}");
		}
		System.out.println("}");
	}

	System.exit(0);
