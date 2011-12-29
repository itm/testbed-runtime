//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8091/controller";
	String secretReservationKeys        = System.getProperty("testbed.secretreservationkeys");
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	boolean csv                         = System.getProperty("testbed.listtype") != null && "csv".equals(System.getProperty("testbed.listtype"));

	String protobufHost                 = System.getProperty("testbed.protobuf.hostname");
	String protobufPortString           = System.getProperty("testbed.protobuf.port");
	Integer protobufPort                = protobufPortString == null ? null : Integer.parseInt(protobufPortString);
	boolean useProtobuf                 = protobufHost != null && protobufPort != null;

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	Controller controller = new Controller() {
		public void receive(List msgs) {
			for (int i=0; i<msgs.size(); i++) {
				Message msg = (Message) msgs.get(i);
				synchronized(System.out) {
					
					String text = StringUtils.replaceNonPrintableAsciiCharacters(new String(msg.getBinaryData()));

					if (csv) {
						text = text.replaceAll(";", "\\;");
					}

					System.out.print(new org.joda.time.DateTime(msg.getTimestamp().toGregorianCalendar()));
					System.out.print(csv ? ";" : " | ");
					System.out.print(msg.getSourceNodeId());
					System.out.print(csv ? ";" : " | ");
					System.out.print(text);
					System.out.print(csv ? ";" : " | ");
					System.out.print(StringUtils.toHexString(msg.getBinaryData()));
					System.out.println();
            	}
			}
		}
		public void receiveStatus(List requestStatuses) {
			// nothing to do
		}
		public void receiveNotification(List msgs) {
			for (int i=0; i<msgs.size(); i++) {
				System.err.print(new org.joda.time.DateTime());
				System.err.print(csv ? ";" : " | ");
				System.err.print("Notification");
				System.err.print(csv ? ";" : " | ");
				System.err.print(msgs.get(i));
				System.err.println();
			}
		}
		public void experimentEnded() {
			System.err.println("Experiment ended");
			System.exit(0);
		}
	};

	// try to connect via unofficial protocol buffers API if hostname and port are set in the configuration
    if (useProtobuf) {

		ProtobufControllerClient pcc = ProtobufControllerClient.create(
				protobufHost,
				protobufPort,
				helper.parseSecretReservationKeys(secretReservationKeys)
		);
		pcc.addListener(new ProtobufControllerAdapter(controller));
		try {
			pcc.connect();
		} catch (Exception e) {
			useProtobuf = false;
		}
	}

	if (!useProtobuf) {

		DelegatingController delegator = new DelegatingController(controller);
		delegator.publish(localControllerEndpointURL);
		log.debug("Local controller published on url: {}", localControllerEndpointURL);

	}

	log.debug("Using the following parameters for calling getInstance(): {}, {}",
			StringUtils.jaxbMarshal(helper.parseSecretReservationKeys(secretReservationKeys)),
			localControllerEndpointURL
	);

	String wsnEndpointURL = null;
	try {
		wsnEndpointURL = sessionManagement.getInstance(
				helper.parseSecretReservationKeys(secretReservationKeys),
				(useProtobuf ? "NONE" : localControllerEndpointURL)
		);
	} catch (UnknownReservationIdException_Exception e) {
		log.warn("There was no reservation found with the given secret reservation key. Exiting.");
		System.exit(1);
	}

	log.debug("Got a WSN instance URL, endpoint is: {}", wsnEndpointURL);
	WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
	final WSNAsyncWrapper wsn = WSNAsyncWrapper.of(wsnService);

	while(true) {
		try {
			System.in.read();
		} catch (Exception e) {
			System.err.println(e);
		}
	}
