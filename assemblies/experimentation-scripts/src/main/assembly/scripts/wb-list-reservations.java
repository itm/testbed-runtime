//--------------------------------------------------------------------------
// Configuration
//--------------------------------------------------------------------------

	String snaaEndpointURL 				= System.getProperty("testbed.snaa.endpointurl");
    String rsEndpointURL				= System.getProperty("testbed.rs.endpointurl");

	Splitter csvSplitter                = Splitter.on(",").trimResults().omitEmptyStrings();
	List urnPrefixes					= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.urnprefixes")));
    List usernames						= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.usernames")));
    List passwords						= Lists.newArrayList(csvSplitter.split(System.getProperty("testbed.passwords")));

	boolean csv                         = System.getProperty("testbed.listtype") != null && "csv".equals(System.getProperty("testbed.listtype"));

    String fromString					= System.getProperty("testbed.from");
	String untilString					= System.getProperty("testbed.until");

	DateTime from;
	DateTime until;

	if (fromString != null && !"now".equalsIgnoreCase(fromString)) {

		String[] fromArray				= fromString.trim().split("-");

		int fromYear					= Integer.parseInt(fromArray[0]);
		int fromMonth					= Integer.parseInt(fromArray[1]);
		int fromDay						= Integer.parseInt(fromArray[2]);
		int fromHour					= Integer.parseInt(fromArray[3]);
		int fromMinute					= Integer.parseInt(fromArray[4]);

		from							= new DateTime(fromYear, fromMonth, fromDay, fromHour, fromMinute, 0, 0);

	} else {
		from 							= new DateTime(System.currentTimeMillis());
	}

	if (untilString != null && !"".equals(untilString)) {

		String[] untilArray				= untilString.trim().split("-");


		int untilYear					= Integer.parseInt(untilArray[0]);
		int untilMonth					= Integer.parseInt(untilArray[1]);
		int untilDay					= Integer.parseInt(untilArray[2]);
		int untilHour					= Integer.parseInt(untilArray[3]);
		int untilMinute					= Integer.parseInt(untilArray[4]);

		until							= new DateTime(untilYear, untilMonth, untilDay, untilHour, untilMinute, 0, 0);

	} else {
		until							= from.plusWeeks(1);
	}

	SNAA authenticationSystem 			= SNAAServiceHelper.getSNAAService(snaaEndpointURL);
	RS reservationSystem				= RSServiceHelper.getRSService(rsEndpointURL);

//--------------------------------------------------------------------------
// Application logic
//--------------------------------------------------------------------------

	log.debug("Begin query interval: {}", from);
	log.debug("End query interval  : {}", until);

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

	GetReservations period = new GetReservations();
	period.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(from.toGregorianCalendar()));
	period.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(until.toGregorianCalendar()));

	List reservations = reservationSystem.getConfidentialReservations(
			BeanShellHelper.copySnaaToRs(secretAuthenticationKeys),
			period
	);

	Joiner joiner = csv ? Joiner.on(",") : Joiner.on(" | ");
    for (int i=0; i<reservations.size(); i++) {

		ConfidentialReservationData reservation = reservations.get(i);
		List reservationData = Lists.newArrayList(new String[] {
			new DateTime(reservation.getFrom().toGregorianCalendar(), DateTimeZone.getDefault()).toString(),
			new DateTime(reservation.getTo().toGregorianCalendar(), DateTimeZone.getDefault()).toString(),
			BeanShellHelper.toSecretReservationKeysString(reservation.getData()),
			Joiner.on(",").join(reservation.getNodeURNs())
		});

		System.out.println(joiner.join(reservationData));
	}

	System.out.println("REMINDER: Change CSV output to quote individual columns!");