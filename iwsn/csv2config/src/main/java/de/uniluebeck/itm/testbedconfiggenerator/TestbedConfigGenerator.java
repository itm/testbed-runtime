//package de.uniluebeck.itm.testbedconfiggenerator;
//
//import au.com.bytecode.opencsv.CSVReader;
//import com.google.common.base.Preconditions;
//import com.google.common.collect.BiMap;
//import com.google.common.collect.HashBiMap;
//import de.uniluebeck.itm.tr.runtime.portalapp.PortalServerFactory;
//import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
//import de.uniluebeck.itm.tr.runtime.portalapp.xml.WebService;
//import de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppFactory;
//import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
//import de.uniluebeck.itm.tr.runtime.wsnapp.xml.Wsnapp;
//import de.uniluebeck.itm.tr.xml.*;
//import eu.wisebed.ns.wiseml._1.Setup;
//import eu.wisebed.ns.wiseml._1.Wiseml;
//import org.apache.commons.cli.*;
//import org.apache.log4j.*;
//import org.slf4j.LoggerFactory;
//
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//
//
//public class TestbedConfigGenerator {
//
//	public static final String HOSTNAME = "hostname";
//
//	public static final String NODE_ID = "node.id";
//
//	public static final String NODE_PORT = "node.port";
//
//	public static final String META_ROOM_NAME = "meta.room.name";
//
//	public static final String META_ROOM_NUMBER = "meta.room.number";
//
//	public static final String NODE_TYPE = "node.type";
//
//	public static final String META_NODE_GATEWAY = "node.gateway";
//
//	public static final String META_NODE_DESCRIPTION = "node.description";
//
//	public static final String META_NODE_CAPABILITY_PREFIX = "node.capability.";
//
//	public static final String META_NODE_CAPABILITY_SUFFIX_URN = ".urn";
//
//	public static final String META_NODE_CAPABILITY_SUFFIX_DATATYPE = ".datatype";
//
//	public static final String META_NODE_CAPABILITY_SUFFIX_UNIT = ".unit";
//
//	public static final String META_NODE_CAPABILITY_SUFFIX_DEFAULT = ".default";
//
//	public static final String URN_TESTBED_PREFIX = "urn.testbed.prefix";
//
//	public static void main(String[] args) throws IOException, JAXBException {
//
//		// ====== set up command line parameters ======
//
//		CommandLineParser parser = new PosixParser();
//		Options options = new Options();
//		options.addOption("f", "file", true, "The (XML) configuration file");
//		options.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
//		options.addOption("l", "logging", true,
//				"Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "])"
//		);
//		options.addOption("h", "help", false, "Help output");
//		options.addOption("p", "prefix", true, "Name of the testbed prefix for which to generate the config file");
//		options.addOption("o", "output", true, "Name of the config file to be written");
//		options.addOption("O", "wisemloutput", true, "Name of the WiseML file to be generated");
//		options.addOption("i", "internal", true,
//				"The testbed internal server connection to be opened by the portal (e.g. localhost:8080)"
//		);
//		options.addOption("s", "sessionmanagementendpointurl", true,
//				"The endpoint URL of the session management API instance of the portal server"
//		);
//		options.addOption("w", "wsninstancebaseurl", true,
//				"The base URL of the WSN API instances of the portal server to which the random suffix is attached "
//						+ "upon instance creation"
//		);
//		options.addOption("n", "wisemlfilename", true,
//				"The WiseML file to be used by the getNetwork() function on the portal server"
//		);
//		options.addOption("a", "autodetection", false, "Use device port auto detection by MAC address");
//		options.addOption("z", "portalhostname", true, "The hostname of the portal server");
//		options.addOption("x", "hex", false, "If set generate hex numbers for node IDs");
//
//		// ====== configure logging defaults ======
//
//		{
//			Appender appender = new ConsoleAppender(new PatternLayout("%-4r [%t] %-5p %c %x - %m%n"));
//
//			Logger itmLogger = Logger.getLogger("de.uniluebeck.itm");
//
//			if (!itmLogger.getAllAppenders().hasMoreElements()) {
//				itmLogger.addAppender(appender);
//				itmLogger.setLevel(Level.INFO);
//			}
//		}
//
//		final org.slf4j.Logger log = LoggerFactory.getLogger(TestbedConfigGenerator.class);
//
//		// ====== parse command line parameters ======
//
//		File xmlFile = null;
//		String testbedPrefix = null;
//		File outputFile = null, wisemlOutputFile = null;
//		String portalInternalAddress = null;
//		String sessionmanagementendpointurl = null;
//		String wsninstancebaseurl = null;
//		String wisemlfilename = null;
//		String portalHostname = null;
//		boolean useHexValues = false;
//		boolean useAutodetection = false;
//
//		try {
//
//			CommandLine line = parser.parse(options, args);
//
//			if (line.hasOption('v')) {
//				Logger.getRootLogger().setLevel(Level.DEBUG);
//				Logger.getLogger("de.uniluebeck.itm").setLevel(Level.DEBUG);
//			}
//
//			if (line.hasOption('l')) {
//				Level level = Level.toLevel(line.getOptionValue('l'));
//				System.out.println("Setting log level to " + level);
//				Logger.getRootLogger().setLevel(level);
//				Logger.getLogger("de.uniluebeck.itm").setLevel(level);
//			}
//
//			if (line.hasOption('h')) {
//				usage(options);
//			}
//
//			if (line.hasOption('p')) {
//				testbedPrefix = line.getOptionValue('p');
//			} else {
//				throw new Exception("Please supply -p");
//			}
//
//			if (line.hasOption('i')) {
//				portalInternalAddress = line.getOptionValue('i');
//			} else {
//				throw new Exception("Please supply -i");
//			}
//
//			if (line.hasOption('s')) {
//				sessionmanagementendpointurl = line.getOptionValue('s');
//			} else {
//				throw new Exception("Please supply -s");
//			}
//
//			if (line.hasOption('w')) {
//				wsninstancebaseurl = line.getOptionValue('w');
//			} else {
//				throw new Exception("Please supply -w");
//			}
//
//			useHexValues = line.hasOption('x');
//			useAutodetection = line.hasOption('a');
//
//			if (line.hasOption('n')) {
//				wisemlfilename = line.getOptionValue('n');
//			} else {
//				throw new Exception("Please supply -n");
//			}
//
//			if (line.hasOption('z')) {
//				portalHostname = line.getOptionValue('z');
//			} else {
//				throw new Exception("Please supply -z");
//			}
//
//			if (line.hasOption('o')) {
//
//				String configFilename = line.getOptionValue('o');
//				outputFile = new File(configFilename);
//
//				if (!outputFile.exists()) {
//					outputFile.createNewFile();
//				}
//
//				if (!outputFile.canWrite()) {
//					throw new Exception("The given file " + xmlFile.getAbsolutePath() + " is not writable!");
//				}
//
//				log.info("Using output file: {}", outputFile.getAbsolutePath());
//
//			} else {
//				throw new Exception("Please supply -o");
//			}
//
//			if (line.hasOption('O')) {
//
//				String wisemlOutputFilename = line.getOptionValue('O');
//				wisemlOutputFile = new File(wisemlOutputFilename);
//
//				if (!wisemlOutputFile.exists()) {
//					wisemlOutputFile.createNewFile();
//				}
//
//				if (!wisemlOutputFile.canWrite()) {
//					throw new Exception("The given file " + wisemlOutputFile.getAbsolutePath() + " is not writable!");
//				}
//
//				log.info("Using WiseML output file: {}", wisemlOutputFile.getAbsolutePath());
//
//			} else {
//				throw new Exception("Please supply -O");
//			}
//
//			if (line.hasOption('f')) {
//
//				String xmlFilename = line.getOptionValue('f');
//				xmlFile = new File(xmlFilename);
//
//				if (!xmlFile.exists()) {
//					throw new Exception("The given file name " + xmlFile.getAbsolutePath() + " does not exist!");
//				}
//
//				if (!xmlFile.canRead()) {
//					throw new Exception("The given file " + xmlFile.getAbsolutePath() + " is not readable!");
//				}
//
//			} else {
//				throw new Exception("Please supply -f");
//			}
//
//		} catch (Exception e) {
//			log.error("Invalid command line: " + e, e);
//			usage(options);
//		}
//
//		// ====== parse CSV file ======
//
//		CSVReader reader = new CSVReader(
//				new FileReader(xmlFile),
//				CSVReader.DEFAULT_SEPARATOR,
//				CSVReader.DEFAULT_QUOTE_CHARACTER,
//				0
//		);
//
//		String[] nextLine;
//		String nodeIdISense, portISense, nodeIdPacemate, portPacemate, nodeIdTelosB, portTelosB, boxNr, hostname,
//				hostnameShort, room, roomPersons, gatewayUrnSuffix;
//		Testbed testbed = new Testbed();
//		Set<String> nodeUrns = new HashSet<String>();
//
//		// detect column by looking for property names in the first line
//		BiMap<Integer, String> columnMap = HashBiMap.create();
//		if ((nextLine = reader.readNext()) != null) {
//
//			for (int column = 0; column < nextLine.length; column++) {
//				columnMap.put(column, nextLine[column]);
//			}
//
//			Preconditions.checkNotNull(columnMap.containsValue(HOSTNAME));
//			Preconditions.checkNotNull(columnMap.containsValue(NODE_ID));
//			Preconditions.checkNotNull(columnMap.containsValue(NODE_PORT));
//			Preconditions.checkNotNull(columnMap.containsValue(NODE_TYPE));
//			Preconditions.checkNotNull(columnMap.containsValue(URN_TESTBED_PREFIX));
//
//		} else {
//			throw new RuntimeException(
//					"CSV file must have at least one file containing one line that defines property names for the columns"
//			);
//		}
//
//		while ((nextLine = reader.readNext()) != null) {
//			// nextLine[] is an array of values from the line
//
//			if (nextLine.length >= FIELD_ROOM_PERSONS && testbedPrefix.equals(nextLine[FIELD_TESTBED_PREFIX])) {
//
//
//				nodeIdISense = nextLine[FIELD_NODE_ID_ISENSE].startsWith("0x") ?
//						nextLine[FIELD_NODE_ID_ISENSE].substring(2) :
//						nextLine[FIELD_NODE_ID_ISENSE];
//				portISense = nextLine[FIELD_PORT_ISENSE];
//
//				nodeIdPacemate = nextLine[FIELD_NODE_ID_PACEMATE].startsWith("0x") ?
//						nextLine[FIELD_NODE_ID_PACEMATE].substring(2) :
//						nextLine[FIELD_NODE_ID_PACEMATE];
//				portPacemate = nextLine[FIELD_PORT_PACEMATE];
//
//				nodeIdTelosB = nextLine[FIELD_NODE_ID_TELOSB].startsWith("0x") ?
//						nextLine[FIELD_NODE_ID_TELOSB].substring(2) :
//						nextLine[FIELD_NODE_ID_TELOSB];
//				portTelosB = nextLine[FIELD_PORT_TELOSB];
//
//				boxNr = nextLine[FIELD_BOX_NR];
//				hostname = nextLine[FIELD_HOSTNAME];
//				hostnameShort = nextLine[FIELD_HOSTNAME_SHORT];
//				gatewayUrnSuffix = nextLine[FIELD_GATEWAY_URN_SUFFIX];
//				room = nextLine[FIELD_ROOM];
//				roomPersons = nextLine[FIELD_ROOM_PERSONS];
//
//				Gateway gateway = testbed.getGateways().get(hostnameShort);
//				if (gateway == null) {
//					gateway = new Gateway(hostname, hostnameShort, testbedPrefix + gatewayUrnSuffix, room, roomPersons);
//					testbed.getGateways().put(hostnameShort, gateway);
//				}
//				Box box = new Box(boxNr, nodeIdISense, portISense, nodeIdPacemate, portPacemate, nodeIdTelosB,
//						portTelosB
//				);
//				gateway.getBoxes().add(box);
//
//			} else {
//				log.warn("Ignoring CSV line: {}" + Arrays.toString(nextLine));
//			}
//
//		}
//
//		// ====== now generate the XML config class instances ======
//
//		de.uniluebeck.itm.tr.xml.Testbed config = new de.uniluebeck.itm.tr.xml.Testbed();
//
//		// add portal to config
//		Node portalNode = new Node();
//		portalNode.setId(portalHostname);
//		NodeNames portalNodeNames = new NodeNames();
//		NodeName portalNodeName = new NodeName();
//		portalNodeName.setName(portalHostname);
//		portalNodeNames.getNodename().add(portalNodeName);
//		portalNode.setNames(portalNodeNames);
//		ServerConnections portalServerConnections = new ServerConnections();
//		ServerConnection portalServerConnection = new ServerConnection();
//		portalServerConnection.setAddress(portalInternalAddress);
//		portalServerConnection.setType("tcp");
//		portalServerConnections.getServerconnection().add(portalServerConnection);
//		portalNode.setServerconnections(portalServerConnections);
//		Applications portalApplications = new Applications();
//		Application portalApplication = new Application();
//		portalApplication.setName("Portal");
//		portalApplication.setFactoryclass(PortalServerFactory.class.getCanonicalName());
//		Portalapp portalapp = new Portalapp();
//		WebService webservice = new WebService();
//		// webservice.setReservationendpointurl();
//		webservice.setSessionmanagementendpointurl(sessionmanagementendpointurl);
//		webservice.setUrnprefix(testbedPrefix);
//		webservice.setWisemlfilename(wisemlfilename);
//		webservice.setWsninstancebaseurl(wsninstancebaseurl);
//		portalapp.setWebservice(webservice);
//		portalApplication.setAny(portalapp);
//		portalApplications.getApplication().add(portalApplication);
//		portalNode.setApplications(portalApplications);
//		config.getNodes().add(portalNode);
//
//		for (Gateway gateway : testbed.getGateways().values()) {
//
//			log.info("{}", gateway);
//
//			NodeNames nodeNames = new NodeNames();
//
//			// add name for every sensor node attached to gateway
//			NodeName nodeName;
//			for (Box box : gateway.getBoxes()) {
//
//				nodeName = new NodeName();
//				if (useHexValues) {
//					nodeName.setName(testbedPrefix + "0x" + Long.toString(box.getNodeIdISense(), 16));
//				} else {
//					nodeName.setName(testbedPrefix + Long.toString(box.getNodeIdISense(), 10));
//				}
//				nodeNames.getNodename().add(nodeName);
//
//				// TODO add TelosB and Pacemate names
//
//				if (useHexValues) {
//					nodeUrns.add(testbedPrefix + "0x" + Long.toString(box.getNodeIdISense(), 16));
//				} else {
//					nodeUrns.add(testbedPrefix + Long.toString(box.getNodeIdISense(), 10));
//				}
//
//			}
//
//			Node node = new Node();
//			node.setId(gateway.getHostname());
//			node.setNames(nodeNames);
//
//			// add server connection for this gateway
//			ServerConnection serverConnection = new ServerConnection();
//			serverConnection.setType("tcp");
//			serverConnection.setAddress(gateway.getHostname() + ":8880");
//			ServerConnections serverConnections = new ServerConnections();
//			serverConnections.getServerconnection().add(serverConnection);
//			node.setServerconnections(serverConnections);
//
//			Wsnapp wsnapp = new Wsnapp();
//
//			for (Box box : gateway.getBoxes()) {
//
//				WsnDevice wsnDevice = new WsnDevice();
//				if (useAutodetection) {
//					if (useHexValues) {
//						wsnDevice.setAutodetectionMac("0x" + Long.toString(box.getNodeIdISense(), 16));
//					} else {
//						wsnDevice.setAutodetectionMac(Long.toString(box.getNodeIdISense(), 10));
//					}
//				} else {
//					wsnDevice.setSerialinterface(box.getPortISense());
//				}
//				wsnDevice.setType("isense");
//				if (useHexValues) {
//					wsnDevice.setUrn(testbedPrefix + "0x" + Long.toString(box.getNodeIdISense(), 16));
//				} else {
//					wsnDevice.setUrn(testbedPrefix + Long.toString(box.getNodeIdISense(), 10));
//				}
//
//				wsnapp.getDevice().add(wsnDevice);
//
//				// TODO add TelosB and Pacemate devices
//
//				//log.info(StringUtils.jaxbMarshalFragment(wsnapp));
//
//			}
//
//			Application application = new Application();
//			application.setName("WSNDeviceApp");
//			application.setFactoryclass(WSNDeviceAppFactory.class.getCanonicalName());
//			application.setAny(wsnapp);
//
//			Applications applications = new Applications();
//			applications.getApplication().add(application);
//			node.setApplications(applications);
//
//			config.getNodes().add(node);
//
//		}
//
//		// ====== generate the XML files ======
//
//		JAXBContext jc = JAXBContext.newInstance(
//				Portalapp.class,
//				de.uniluebeck.itm.tr.xml.Testbed.class,
//				Wsnapp.class
//		);
//		Marshaller marshaller = jc.createMarshaller();
//		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//		FileWriter writer = new FileWriter(outputFile.getAbsolutePath());
//		marshaller.marshal(config, writer);
//
//		// ====== generate the WiseML files ======
//		Wiseml wiseml = new Wiseml();
//		Setup setup = new Setup();
//		wiseml.setSetup(setup);
//
//		for (Gateway gateway : testbed.getGateways().values()) {
//			for (Box box : gateway.getBoxes()) {
//				Setup.Node node = new Setup.Node();
//				if (useHexValues) {
//					node.setId(testbedPrefix + "0x" + Long.toString(box.getNodeIdISense(), 16));
//				} else {
//					node.setId(testbedPrefix + Long.toString(box.getNodeIdISense(), 10));
//				}
//				setup.getNode().add(node);
//			}
//		}
//
//		jc = JAXBContext.newInstance(
//				Wiseml.class
//		);
//		marshaller = jc.createMarshaller();
//		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//		writer = new FileWriter(wisemlOutputFile.getAbsolutePath());
//		marshaller.marshal(wiseml, writer);
//
//
//		System.out.println(Arrays.toString(nodeUrns.toArray()));
//
//		System.out.println();
//
//		for (String nodeUrn : nodeUrns) {
//			System.out.print("\"" + nodeUrn + "\",");
//		}
//
//		System.out.println();
//
//		for (String nodeUrn : nodeUrns) {
//			System.out.print("0,");
//		}
//
//		System.out.println();
//
//		for (String nodeUrn : nodeUrns) {
//			System.out.print(nodeUrn + ",");
//		}
//
//	}
//
//	private static void usage(Options options) {
//		HelpFormatter formatter = new HelpFormatter();
//		formatter.printHelp(TestbedConfigGenerator.class.getCanonicalName(), options);
//		System.exit(1);
//	}
//
//}
