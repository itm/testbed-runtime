//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String secretReservationKeys = System.getProperty("testbed.secretreservationkeys");
	String pccHost = System.getProperty("testbed.protobuf.hostname");
	Integer pccPort = Integer.parseInt(System.getProperty("testbed.protobuf.port"));

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	ProtobufControllerClient pcc = ProtobufControllerClient.create(pccHost, pccPort, helper.parseSecretReservationKeys(secretReservationKeys));
	pcc.addListener(new ProtobufController() {
		public void receive(List msgs) {
			for (int i=0; i<msgs.size(); i++) {
				Message msg = (Message) msgs.get(i);
				synchronized(System.out) {
					
					String text = StringUtils.replaceNonPrintableAsciiCharacters(new String(msg.getBinaryData()));
					
					System.out.print(msg.getTimestamp() + " | ");
					System.out.print(msg.getSourceNodeId() + " | ");
					System.out.print(text + " | ");
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
				log.info(msgs.get(i));
			}
		}
		public void experimentEnded() {
			log.info("Experiment ended");
			System.exit(0);
		}
		public void onConnectionEstablished() {
			log.debug("Connection established.");
		}
		public void onConnectionClosed() {
			log.debug("Connection closed.");
		}
	});
	pcc.connect();
	