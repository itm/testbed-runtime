package de.uniluebeck.itm.tr.iwsn.csv2config;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.uniluebeck.itm.tr.runtime.portalapp.PortalServerFactory;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.WebService;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import de.uniluebeck.itm.tr.xml.*;
import eu.wisebed.ns.wiseml._1.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.*;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.*;


public class CSV2Config {

	public static final String HOSTNAME = "hostname";

	public static final String NODE_ID = "node.id";

	public static final String NODE_PORT = "node.port";

	public static final String NODE_REFERENCE = "node.reference";

	public static final String NODE_TYPE = "node.type";

	public static final String NODE_GATEWAY = "node.gateway";

	public static final String NODE_DESCRIPTION = "node.description";

	public static final String URN_TESTBED_PREFIX = "urn.testbed.prefix";

	public static final String NODE_POSITION_X = "node.position.x";

	public static final String NODE_POSITION_Y = "node.position.y";

	public static final String NODE_POSITION_Z = "node.position.z";

	public static final String META_ROOM_NAME = "meta.room.name";

	public static final String META_ROOM_NUMBER = "meta.room.number";

	public static final String META_NODE_CAPABILITY_PREFIX = "node.capability.";

	public static final String META_NODE_CAPABILITY_SUFFIX_URN = ".urn";

	public static final String META_NODE_CAPABILITY_SUFFIX_DATATYPE = ".datatype";

	public static final String META_NODE_CAPABILITY_SUFFIX_UNIT = ".unit";

	public static final String META_NODE_CAPABILITY_SUFFIX_DEFAULT = ".default";

	private void createWiseMLFile() throws JAXBException, IOException {

		// ====== create WiseML file ======

		Wiseml wiseml = new Wiseml();
		wiseml.setVersion("1.0");

		Setup setup = new Setup();
		wiseml.setSetup(setup);

		if (cmdLineParameters.setupCoordinateType != null && !"".equals(cmdLineParameters.setupCoordinateType)) {
			setup.setCoordinateType(cmdLineParameters.setupCoordinateType);
		}
		if (cmdLineParameters.setupDescription != null && !"".equals(cmdLineParameters.setupDescription)) {
			setup.setDescription(cmdLineParameters.setupDescription);
		}

		boolean hasOriginX = cmdLineParameters.setupOriginX != null && !"".equals(cmdLineParameters.setupOriginX);
		boolean hasOriginY = cmdLineParameters.setupOriginY != null && !"".equals(cmdLineParameters.setupOriginY);
		boolean hasOriginZ = cmdLineParameters.setupOriginZ != null && !"".equals(cmdLineParameters.setupOriginZ);
		boolean hasOriginPhi = cmdLineParameters.setupOriginPhi != null && !"".equals(cmdLineParameters.setupOriginPhi);
		boolean hasOriginTheta = cmdLineParameters.setupOriginTheta != null && !"".equals(cmdLineParameters.setupOriginTheta);

		boolean hasOrigin = hasOriginX || hasOriginY || hasOriginZ || hasOriginPhi || hasOriginTheta;

		if (hasOrigin) {
			Coordinate coordinate = new Coordinate();
			if (hasOriginX) {
				coordinate.setX(Double.parseDouble(cmdLineParameters.setupOriginX));
			}
			if (hasOriginY) {
				coordinate.setY(Double.parseDouble(cmdLineParameters.setupOriginY));
			}
			if (hasOriginZ) {
				coordinate.setZ(Double.parseDouble(cmdLineParameters.setupOriginZ));
			}
			if (hasOriginPhi) {
				coordinate.setPhi(Double.parseDouble(cmdLineParameters.setupOriginPhi));
			}
			if (hasOriginTheta) {
				coordinate.setTheta(Double.parseDouble(cmdLineParameters.setupOriginTheta));
			}
			setup.setOrigin(coordinate);
		}

		List<Setup.Node> nodes = setup.getNode();

		CSVReader reader = new CSVReader(
				new FileReader(cmdLineParameters.csvFile),
				CSVReader.DEFAULT_SEPARATOR,
				CSVReader.DEFAULT_QUOTE_CHARACTER,
				0
		);

		String[] nextLine;

		// detect column by looking for property names in the first line
		BiMap<Integer, String> columnMap = HashBiMap.create();
		if ((nextLine = reader.readNext()) != null) {

			for (int column = 0; column < nextLine.length; column++) {
				columnMap.put(column, nextLine[column]);
			}

			Preconditions.checkNotNull(columnMap.containsValue(HOSTNAME));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_ID));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_REFERENCE));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_PORT));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_TYPE));
			Preconditions.checkNotNull(columnMap.containsValue(URN_TESTBED_PREFIX));

		} else {
			throw new RuntimeException(
					"CSV file must have at least one file containing one line that defines property names for the columns"
			);
		}

		Setup.Node node;
		BiMap<String, Integer> columns = columnMap.inverse();
		while ((nextLine = reader.readNext()) != null) {

			node = new Setup.Node();
			nodes.add(node);

			node.setId(cmdLineParameters.testbedPrefix + nextLine[columns.get(NODE_ID)].toLowerCase());
			node.setDescription(nextLine[columns.get(NODE_DESCRIPTION)]);
			node.setGateway(Boolean.parseBoolean(nextLine[columns.get(NODE_GATEWAY)].toLowerCase()));
			node.setNodeType(nextLine[columns.get(NODE_TYPE)]);
			Coordinate position = new Coordinate();
			position.setX(Double.parseDouble(nextLine[columns.get(NODE_POSITION_X)].replace(',', '.')));
			position.setY(Double.parseDouble(nextLine[columns.get(NODE_POSITION_Y)].replace(',', '.')));
			position.setZ(Double.parseDouble(nextLine[columns.get(NODE_POSITION_Z)].replace(',', '.')));
			node.setPosition(position);

			Set<String> columnSet = columns.keySet();
			Map<Integer, Capability> capabilityMap = new HashMap<Integer, Capability>();
			for (String column : columnSet) {

				if (column.startsWith(META_NODE_CAPABILITY_PREFIX)) {

					int capabilityNum = Integer.parseInt(
							column.substring(
									META_NODE_CAPABILITY_PREFIX.length(),
									column.indexOf(".", META_NODE_CAPABILITY_PREFIX.length()
									)
							)
					);

					if (nextLine.length > columns.get(column)) {

						Capability capability = capabilityMap.get(capabilityNum);
						boolean hadData = false;
						if (capability == null) {
							capability = new Capability();
						}

						if (column.endsWith(META_NODE_CAPABILITY_SUFFIX_URN)) {
							String value = nextLine[columns.get(column)];
							if (value != null && !"".equals(value)) {
								capability.setName(nextLine[columns.get(column)]);
								hadData = true;
							}
						} else if (column.endsWith(META_NODE_CAPABILITY_SUFFIX_DATATYPE)) {
							String value = nextLine[columns.get(column)];
							if (value != null && !"".equals(value)) {
								capability.setDatatype(Dtypes.fromValue(value));
								hadData = true;
							}
						} else if (column.endsWith(META_NODE_CAPABILITY_SUFFIX_DEFAULT)) {
							String value = nextLine[columns.get(column)];
							if (value != null && !"".equals(value)) {
								capability.setDefault(value);
								hadData = true;
							}
						} else if (column.endsWith(META_NODE_CAPABILITY_SUFFIX_UNIT)) {
							String value = nextLine[columns.get(column)];
							if (value != null && !"".equals(value)) {
								capability.setUnit(Units.fromValue(value));
								hadData = true;
							}
						}

						if (hadData) {
							capabilityMap.put(capabilityNum, capability);
						}
					}

				}
			}
			for (Capability capability : capabilityMap.values()) {
				node.getCapability().add(capability);
			}
		}

		JAXBContext jc = JAXBContext.newInstance(Wiseml.class);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		FileWriter writer = new FileWriter(cmdLineParameters.wisemlOutputFile.getAbsolutePath());
		marshaller.marshal(wiseml, writer);

	}

	private PosixParser cmdLineParser;

	private org.slf4j.Logger log;

	private Options cmdLineOptions;

	private class CmdLineParameters {

		public File csvFile = null;

		public String testbedPrefix = null;

		public File outputFile = null;

		public File wisemlOutputFile = null;

		public String portalInternalAddress = null;

		public String sessionmanagementendpointurl = null;

		public String wsninstancebaseurl = null;

		public String wisemlfilename = null;

		public String portalHostname = null;

		public boolean useHexValues = false;

		public boolean useAutodetection = false;

		public String reservationSystemEndpointURL;

		public String snaaEndpointUrl = null;

		public String setupOriginX = null;

		public String setupOriginY = null;

		public String setupOriginZ = null;

		public String setupOriginPhi = null;

		public String setupOriginTheta = null;

		public String setupCoordinateType = null;

		public String setupDescription = null;

		public String protobufIp = null;

		public String protobufHostname = null;

		public Integer protobufPort = null;

		public Integer maximummessagerate = null;
	}

	private CmdLineParameters cmdLineParameters = new CmdLineParameters();

	public static void main(String[] args) throws IOException, JAXBException {

		CSV2Config csv2Config = new CSV2Config();
		csv2Config.configureLoggingDefaults();
		csv2Config.setUpCmdLineParameters();
		csv2Config.parseCmdLineParameters(args);
		csv2Config.createTRConfig();
		csv2Config.createWiseMLFile();


	}

	private void createTRConfig() throws IOException, JAXBException {

		// ====== parse CSV file ======

		CSVReader reader = new CSVReader(
				new FileReader(cmdLineParameters.csvFile),
				CSVReader.DEFAULT_SEPARATOR,
				CSVReader.DEFAULT_QUOTE_CHARACTER,
				0
		);

		String[] nextLine;

		// detect column by looking for property names in the first line
		BiMap<Integer, String> columnMap = HashBiMap.create();
		if ((nextLine = reader.readNext()) != null) {

			for (int column = 0; column < nextLine.length; column++) {
				columnMap.put(column, nextLine[column]);
			}

			Preconditions.checkNotNull(columnMap.containsValue(HOSTNAME));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_ID));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_REFERENCE));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_PORT));
			Preconditions.checkNotNull(columnMap.containsValue(NODE_TYPE));
			Preconditions.checkNotNull(columnMap.containsValue(URN_TESTBED_PREFIX));

		} else {
			throw new RuntimeException(
					"CSV file must have at least one file containing one line that defines property names for the columns"
			);
		}

		Testbed testbed = new Testbed();

		// ====== create configuration for portal server ======

		Node portalNode = new Node();
		portalNode.setId(cmdLineParameters.portalHostname);
		NodeNames portalNodeNames = new NodeNames();
		NodeName portalNodeName = new NodeName();
		portalNodeName.setName(cmdLineParameters.portalHostname.toLowerCase());
		portalNodeNames.getNodename().add(portalNodeName);
		portalNode.setNames(portalNodeNames);
		ServerConnections portalServerConnections = new ServerConnections();
		ServerConnection portalServerConnection = new ServerConnection();
		portalServerConnection.setAddress(cmdLineParameters.portalInternalAddress);
		portalServerConnection.setType("tcp");
		portalServerConnections.getServerconnection().add(portalServerConnection);
		portalNode.setServerconnections(portalServerConnections);
		Applications portalApplications = new Applications();
		Application portalApplication = new Application();
		portalApplication.setName("Portal");
		portalApplication.setFactoryclass(PortalServerFactory.class.getCanonicalName());
		Portalapp portalapp = new Portalapp();
		WebService webservice = new WebService();
		webservice.setReservationendpointurl(cmdLineParameters.reservationSystemEndpointURL);
		webservice.setSessionmanagementendpointurl(cmdLineParameters.sessionmanagementendpointurl);
		webservice.setSnaaendpointurl(cmdLineParameters.snaaEndpointUrl);
		webservice.setUrnprefix(cmdLineParameters.testbedPrefix);
		webservice.setWisemlfilename(cmdLineParameters.wisemlfilename);
		webservice.setWsninstancebaseurl(cmdLineParameters.wsninstancebaseurl);
		if (cmdLineParameters.protobufPort != null) {
			ProtobufInterface protobufInterface = new ProtobufInterface();
			protobufInterface.setPort(cmdLineParameters.protobufPort);
			if (cmdLineParameters.protobufHostname != null) {
				protobufInterface.setHostname(cmdLineParameters.protobufHostname);
			}
			if (cmdLineParameters.protobufIp != null) {
				protobufInterface.setIp(cmdLineParameters.protobufIp);
			}
			webservice.setProtobufinterface(protobufInterface);
		}
		portalapp.setWebservice(webservice);
		portalApplication.setAny(portalapp);
		portalApplications.getApplication().add(portalApplication);
		portalNode.setApplications(portalApplications);
		testbed.getNodes().add(portalNode);

		// ====== create configuration for overlay nodeMap ======

		Map<String, Node> nodeMap = new HashMap<String, Node>();
		BiMap<String, Integer> columns = columnMap.inverse();

		while ((nextLine = reader.readNext()) != null) {

			// add overlay node name if not yet existing
			String hostname = nextLine[columns.get(HOSTNAME)].toLowerCase();
			String nodeUrn = (nextLine[columns.get(URN_TESTBED_PREFIX)] + nextLine[columns.get(NODE_ID)]).toLowerCase();

			Node node = nodeMap.get(hostname);
			if (node == null) {

				node = new Node();
				node.setId(hostname);
				nodeMap.put(hostname, node);

				ServerConnection serverConnection = new ServerConnection();
				serverConnection.setType("tcp");
				serverConnection.setAddress(hostname + ":8880");

				ServerConnections serverConnections = new ServerConnections();
				serverConnections.getServerconnection().add(serverConnection);
				node.setServerconnections(serverConnections);

				testbed.getNodes().add(node);
			}

			// add device urn as node name to overlay node
			NodeNames nodeNames = node.getNames();
			if (nodeNames == null) {
				nodeNames = new NodeNames();
				node.setNames(nodeNames);
			}
			NodeName nodeName = new NodeName();
			nodeName.setName(nodeUrn);
			nodeNames.getNodename().add(nodeName);

			// add WSN application if not yet existing
			Application application = new Application();
			application.setName("WSNDeviceApp");
			application.setFactoryclass(WSNDeviceAppFactory.class.getCanonicalName());

			Applications applications = node.getApplications();
			if (applications == null) {
				applications = new Applications();
				node.setApplications(applications);
			}
			applications.getApplication().add(application);

			// add WSN device (current row of CSV)
			WsnDevice wsnDevice = new WsnDevice();
			wsnDevice.setType(nextLine[columns.get(NODE_TYPE)]);
			wsnDevice.setUrn(nodeUrn);

			if (cmdLineParameters.maximummessagerate != null) {
				wsnDevice.setMaximummessagerate(cmdLineParameters.maximummessagerate);
			}

			String nodePort = nextLine[columns.get(NODE_PORT)];
			if (nodePort != null && !"".equals(nodePort)) {
				wsnDevice.setSerialinterface(nodePort);
			}

			String nodeReference = nextLine[columns.get(NODE_REFERENCE)];
			if (nodeReference != null && !"".equals(nodeReference)) {
				wsnDevice.setUsbchipid(nodeReference);
			}

			application.setAny(wsnDevice);

		}

		// ====== write configuration file ======

		JAXBContext jc = JAXBContext.newInstance(
				Testbed.class,
				Portalapp.class,
				WsnDevice.class
		);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		FileWriter writer = new FileWriter(cmdLineParameters.outputFile.getAbsolutePath());
		marshaller.marshal(testbed, writer);

	}

	private void parseCmdLineParameters(String[] args) {

		// ====== parse command line parameters ======

		Properties properties = new Properties();

		try {

			CommandLine line = cmdLineParser.parse(cmdLineOptions, args);

			if (line.hasOption('c')) {

				String xmlFilename = line.getOptionValue('c');
				cmdLineParameters.csvFile = new File(xmlFilename);

				if (!cmdLineParameters.csvFile.exists()) {
					throw new Exception(
							"The given file name " + cmdLineParameters.csvFile.getAbsolutePath() + " does not exist!"
					);
				}

				if (!cmdLineParameters.csvFile.canRead()) {
					throw new Exception(
							"The given file " + cmdLineParameters.csvFile.getAbsolutePath() + " is not readable!"
					);
				}

			} else {
				throw new Exception("Please supply -c");
			}

			if (line.hasOption('p')) {

				properties.load(new FileInputStream(line.getOptionValue('p')));

				if (properties.getProperty("urnprefix") != null) {
					cmdLineParameters.testbedPrefix = properties.getProperty("urnprefix");
				} else {
					throw new Exception("Property file is missing the urnprefix property");
				}

				if (properties.getProperty("portal.hostname") != null) {
					cmdLineParameters.portalHostname = properties.getProperty("portal.hostname");
					cmdLineParameters.portalInternalAddress =
							properties.getProperty("portal.hostname") + ":" + properties
									.getProperty("portal.overlayport", "8880");
				} else {
					throw new Exception("Property file is missing the portal.hostname property");
				}

				if (properties.getProperty("portal.sessionmanagementurl") != null) {
					cmdLineParameters.sessionmanagementendpointurl =
							properties.getProperty("portal.sessionmanagementurl");
				} else {
					cmdLineParameters.sessionmanagementendpointurl =
							"http://" + properties.getProperty("portal.hostname") + ":8888/sessions";
				}

				if (properties.getProperty("portal.wsninstancebaseurl") != null) {
					cmdLineParameters.wsninstancebaseurl = properties.getProperty("portal.wsninstancebaseurl");
				} else {
					cmdLineParameters.wsninstancebaseurl =
							"http://" + properties.getProperty("portal.hostname") + ":8888/wsn/";
				}

				if (properties.getProperty("portal.wisemlpath") != null) {
					cmdLineParameters.wisemlfilename = properties.getProperty("portal.wisemlpath");
				} else {
					throw new Exception("Property file is missing the portal.wisemlpath property");
				}

				boolean hasProtobufIp = false;
				boolean hasProtobufHostname = false;
				boolean hasProtobufPort = false;
				if (properties.getProperty("portal.protobuf.ip") != null) {
					cmdLineParameters.protobufIp = properties.getProperty("portal.protobuf.ip");
					hasProtobufIp = true;
				}

				if (properties.getProperty("portal.protobuf.hostname") != null) {
					cmdLineParameters.protobufHostname = properties.getProperty("portal.protobuf.hostname");
					hasProtobufHostname = true;
				}

				if (properties.getProperty("portal.protobuf.port") != null) {
					cmdLineParameters.protobufPort = Integer.parseInt(properties.getProperty("portal.protobuf.port"));
					hasProtobufPort = true;
				}

				if ((hasProtobufPort && !(hasProtobufHostname || hasProtobufIp)) || (hasProtobufIp && !hasProtobufPort) || (hasProtobufHostname && !hasProtobufPort)) {
					throw new IllegalArgumentException("If portal.protobuf.port is specified exactly one of portal.protobuf.ip and portal.protobuf.hostname must be specified too.");
				}

				if (properties.getProperty("setup.coordinatetype") != null) {
					cmdLineParameters.setupCoordinateType = properties.getProperty("setup.coordinatetype");
				}

				if (properties.getProperty("setup.description") != null) {
					cmdLineParameters.setupDescription = properties.getProperty("setup.description");
				}

				if (properties.getProperty("setup.origin.x") != null) {
					cmdLineParameters.setupOriginX = properties.getProperty("setup.origin.x");
				}

				if (properties.getProperty("setup.origin.y") != null) {
					cmdLineParameters.setupOriginY = properties.getProperty("setup.origin.y");
				}

				if (properties.getProperty("setup.origin.z") != null) {
					cmdLineParameters.setupOriginZ = properties.getProperty("setup.origin.z");
				}

				if (properties.getProperty("setup.origin.phi") != null) {
					cmdLineParameters.setupOriginPhi = properties.getProperty("setup.origin.phi");
				}

				if (properties.getProperty("setup.origin.theta") != null) {
					cmdLineParameters.setupOriginTheta = properties.getProperty("setup.origin.theta");
				}

				cmdLineParameters.useAutodetection =
						Boolean.parseBoolean(properties.getProperty("mac-autodetection", "true"));
				cmdLineParameters.useHexValues = Boolean.parseBoolean(properties.getProperty("hex", "true"));

				if (properties.getProperty("maximummessagerate") != null) {
					cmdLineParameters.maximummessagerate = Integer.parseInt(properties.getProperty("maximummessagerate"));
				}

				cmdLineParameters.reservationSystemEndpointURL = properties.getProperty("portal.reservationsystem");
				cmdLineParameters.snaaEndpointUrl = properties.getProperty("portal.snaaendpointurl");

			} else {
				throw new Exception("Please supply -p");
			}

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
				Logger.getLogger("de.uniluebeck.itm").setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {
				Level level = Level.toLevel(line.getOptionValue('l'));
				Logger.getRootLogger().setLevel(level);
				Logger.getLogger("de.uniluebeck.itm").setLevel(level);
			}

			if (line.hasOption('h')) {
				usage(cmdLineOptions);
			}

			if (line.hasOption('o')) {

				String configFilename = line.getOptionValue('o');

				cmdLineParameters.outputFile = new File(configFilename).getAbsoluteFile();

				if (!cmdLineParameters.outputFile.exists()) {
					cmdLineParameters.outputFile.getParentFile().mkdirs();
					cmdLineParameters.outputFile.createNewFile();
				}

				if (!cmdLineParameters.outputFile.canWrite()) {
					throw new Exception(
							"The given file " + cmdLineParameters.outputFile.getAbsolutePath() + " is not writable!"
					);
				}

				log.info("Using output file: {}", cmdLineParameters.outputFile.getAbsolutePath());

			} else {
				throw new Exception("Please supply -o");
			}

			if (line.hasOption('w')) {

				String wisemlOutputFilename = line.getOptionValue('w');
				cmdLineParameters.wisemlOutputFile = new File(wisemlOutputFilename).getAbsoluteFile();

				if (!cmdLineParameters.wisemlOutputFile.exists()) {
					cmdLineParameters.wisemlOutputFile.getParentFile().mkdirs();
					cmdLineParameters.wisemlOutputFile.createNewFile();
				}

				if (!cmdLineParameters.wisemlOutputFile.canWrite()) {
					throw new Exception("The given file " + cmdLineParameters.wisemlOutputFile
							.getAbsolutePath() + " is not writable!"
					);
				}

				log.info("Using WiseML output file: {}", cmdLineParameters.wisemlOutputFile.getAbsolutePath());

			} else {
				throw new Exception("Please supply -w");
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(cmdLineOptions);
		}

	}

	private void configureLoggingDefaults() {

		// ====== configure logging defaults ======

		{
			Appender appender = new ConsoleAppender(new PatternLayout("%-4r [%t] %-5p %c %x - %m%n"));

			Logger itmLogger = Logger.getLogger("de.uniluebeck.itm");

			if (!itmLogger.getAllAppenders().hasMoreElements()) {
				itmLogger.addAppender(appender);
				itmLogger.setLevel(Level.INFO);
			}
		}

		log = LoggerFactory.getLogger(CSV2Config.class);

	}

	private void setUpCmdLineParameters() {

		// ====== set up command line parameters ======

		cmdLineParser = new PosixParser();
		cmdLineOptions = new Options();

		cmdLineOptions.addOption("c", "csv", true, "CSV nodes file");
		cmdLineOptions.addOption("p", "properties", true, "Properties file");
		cmdLineOptions.addOption("o", "output", true, "Testbed Runtime config file to be generated");
		cmdLineOptions.addOption("w", "wisemloutput", true, "WiseML file to be generated");

		cmdLineOptions.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
		cmdLineOptions.addOption("l", "logging", true,
				"Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "]), optional"
		);
		cmdLineOptions.addOption("h", "help", false, "Display this help output, optional");

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, CSV2Config.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}

}