//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String localControllerEndpointURL	= "http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":8090/controller";
	String sessionManagementEndpointURL	= System.getProperty("testbed.sm.endpointurl");
	String nodeUrnsToCheck              = System.getProperty("testbed.nodeurns");
	boolean csv                         = System.getProperty("testbed.listtype") != null && "csv".equals(System.getProperty("testbed.listtype"));

	SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	AsyncJobObserver jobs = new AsyncJobObserver(10, TimeUnit.SECONDS);

	Controller controller = new Controller() {
		public void receive(List msg) {
			// nothing to do
		}
		public void receiveStatus(List requestStatuses) {
			jobs.receive(requestStatuses);
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
	};

	DelegatingController delegator = new DelegatingController(controller);
	delegator.publish(localControllerEndpointURL);

	log.debug("Local controller published on url: {}", localControllerEndpointURL);

	// retrieve reserved node URNs from testbed
	List nodeURNs;
	if (nodeUrnsToCheck != null && !"".equals(nodeUrnsToCheck)) {
		nodeURNs = Lists.newArrayList(nodeUrnsToCheck.split(","));
	} else {
		nodeURNs = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork(), new String[]{});
	}

	log.debug("Retrieved the following (selected) node URNs: {}", nodeURNs);
	log.debug("Checking if nodes are alive...");

	String requestId = sessionManagement.areNodesAlive(nodeURNs, localControllerEndpointURL);

	Job job = new Job("areNodesAlive", requestId, nodeURNs, Job.JobType.areNodesAlive);
	job.addListener(new JobResultListener() {
		public void receiveJobResult(JobResult result) {
			result.printResults(System.out, false);
			System.exit(0);
		}
		public void receiveMessage(Message msg) throws IOException {
			System.out.println(msg);
		}
		public void timeout() {
			System.out.println("Timed out!");
			System.exit(1);
		}
	});
	jobs.submit(job, 10, TimeUnit.SECONDS);
