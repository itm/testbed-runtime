package de.uniluebeck.itm.testbedconfiggenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.uniluebeck.itm.tr.runtime.portalapp.PortalServerFactory;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.WebService;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.WsnDevice;
import de.uniluebeck.itm.tr.runtime.wsnapp.xml.Wsnapp;
import de.uniluebeck.itm.tr.xml.Application;
import de.uniluebeck.itm.tr.xml.Applications;
import de.uniluebeck.itm.tr.xml.Node;
import de.uniluebeck.itm.tr.xml.NodeName;
import de.uniluebeck.itm.tr.xml.NodeNames;
import de.uniluebeck.itm.tr.xml.ServerConnection;
import de.uniluebeck.itm.tr.xml.ServerConnections;
import de.uniluebeck.itm.tr.xml.Testbed;


public class CSV2Config {

	public static final String HOSTNAME = "hostname";

	public static final String NODE_ID = "node.id";

	public static final String NODE_PORT = "node.port";

	public static final String NODE_TYPE = "node.type";

	public static final String URN_TESTBED_PREFIX = "urn.testbed.prefix";

	public static final String META_ROOM_NAME = "meta.room.name";

	public static final String META_ROOM_NUMBER = "meta.room.number";

	public static final String META_NODE_GATEWAY = "node.gateway";

	public static final String META_NODE_DESCRIPTION = "node.description";

	public static final String META_NODE_CAPABILITY_PREFIX = "node.capability.";

	public static final String META_NODE_CAPABILITY_SUFFIX_URN = ".urn";

	public static final String META_NODE_CAPABILITY_SUFFIX_DATATYPE = ".datatype";

	public static final String META_NODE_CAPABILITY_SUFFIX_UNIT = ".unit";

	public static final String META_NODE_CAPABILITY_SUFFIX_DEFAULT = ".default";

	private PosixParser cmdLineParser;

	private org.slf4j.Logger log;

	private Options cmdLineOptions;

	private class CmdLineParameters {

		public File xmlFile = null;
		public String testbedPrefix = null;
		public File outputFile = null, wisemlOutputFile = null;
		public String portalInternalAddress = null;
		public String sessionmanagementendpointurl = null;
		public String wsninstancebaseurl = null;
		public String wisemlfilename = null;
		public String portalHostname = null;
		public boolean useHexValues = false;
		public boolean useAutodetection = false;

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

	private void createWiseMLFile() {
		// TODO implement
	}

	private void createTRConfig() throws IOException, JAXBException {

		// ====== parse CSV file ======

		CSVReader reader = new CSVReader(
				new FileReader(cmdLineParameters.xmlFile),
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
		portalNodeName.setName(cmdLineParameters.portalHostname);
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
		// webservice.setReservationendpointurl();
		webservice.setSessionmanagementendpointurl(cmdLineParameters.sessionmanagementendpointurl);
		webservice.setUrnprefix(cmdLineParameters.testbedPrefix);
		webservice.setWisemlfilename(cmdLineParameters.wisemlfilename);
		webservice.setWsninstancebaseurl(cmdLineParameters.wsninstancebaseurl);
		portalapp.setWebservice(webservice);
		portalApplication.setAny(portalapp);
		portalApplications.getApplication().add(portalApplication);
		portalNode.setApplications(portalApplications);
		testbed.getNodes().add(portalNode);

		// ====== create configuration for overlay nodeMap ======

		Map<String, Node> nodeMap = new HashMap<String, Node>();
		Map<String, Application> applicationMap = new HashMap<String, Application>();

		while ((nextLine = reader.readNext()) != null) {

			// add overlay node name if not yet existing
			String hostname = nextLine[columnMap.inverse().get(HOSTNAME)];
			Node node = nodeMap.get(hostname);
			if (node == null) {

				node = new Node();
				node.setId(hostname);
				nodeMap.put(hostname, node);

				ServerConnection serverConnection = new ServerConnection();
				serverConnection.setType("tcp");
				serverConnection.setAddress(hostname + ":8880");

				node.getServerconnections().getServerconnection().add(serverConnection);

				testbed.getNodes().add(node);
			}

			// add device urn as node name to overlay node
			NodeName nodeName = new NodeName();
			nodeName.setName(nextLine[columnMap.inverse().get(URN_TESTBED_PREFIX)] + nextLine[columnMap.inverse().get(NODE_ID)]);
			node.getNames().getNodename().add(nodeName);

			// add WSN application if not yet existing
			Application application = applicationMap.get(hostname);
			if (application == null) {

				application = new Application();
				application.setName("WSNDeviceApp");
				application.setFactoryclass(WSNDeviceAppFactory.class.getCanonicalName());
				application.setAny(new Wsnapp());

				node.getApplications().getApplication().add(application);

				applicationMap.put(hostname, application);
			}

			// add WSN device (current row of CSV)
			WsnDevice wsnDevice = new WsnDevice();
			wsnDevice.setType(nextLine[columnMap.inverse().get(NODE_TYPE)]);
			wsnDevice.setUrn(nextLine[columnMap.inverse().get(URN_TESTBED_PREFIX)] + nextLine[columnMap.inverse().get(NODE_ID)]);
			wsnDevice.setAutodetectionMac(nextLine[columnMap.inverse().get(NODE_ID)]);

			Wsnapp wsnApp = (Wsnapp) application.getAny();
			wsnApp.getDevice().add(wsnDevice);

		}

		// ====== write configuration file ======

		JAXBContext jc = JAXBContext.newInstance(
				Testbed.class,
				Portalapp.class,
				Wsnapp.class
		);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		FileWriter writer = new FileWriter(cmdLineParameters.outputFile.getAbsolutePath());
		marshaller.marshal(testbed, writer);

	}

	private void parseCmdLineParameters(String[] args) {

		// ====== parse command line parameters ======

		try {

			CommandLine line = cmdLineParser.parse(cmdLineOptions, args);

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
				Logger.getLogger("de.uniluebeck.itm").setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {
				Level level = Level.toLevel(line.getOptionValue('l'));
				System.out.println("Setting log level to " + level);
				Logger.getRootLogger().setLevel(level);
				Logger.getLogger("de.uniluebeck.itm").setLevel(level);
			}

			if (line.hasOption('h')) {
				usage(cmdLineOptions);
			}

			if (line.hasOption('p')) {
				cmdLineParameters.testbedPrefix = line.getOptionValue('p');
			} else {
				throw new Exception("Please supply -p");
			}

			if (line.hasOption('i')) {
				cmdLineParameters.portalInternalAddress = line.getOptionValue('i');
			} else {
				throw new Exception("Please supply -i");
			}

			if (line.hasOption('s')) {
				cmdLineParameters.sessionmanagementendpointurl = line.getOptionValue('s');
			} else {
				throw new Exception("Please supply -s");
			}

			if (line.hasOption('w')) {
				cmdLineParameters.wsninstancebaseurl = line.getOptionValue('w');
			} else {
				throw new Exception("Please supply -w");
			}

			cmdLineParameters.useHexValues = line.hasOption('x');
			cmdLineParameters.useAutodetection = line.hasOption('a');

			if (line.hasOption('n')) {
				cmdLineParameters.wisemlfilename = line.getOptionValue('n');
			} else {
				throw new Exception("Please supply -n");
			}

			if (line.hasOption('z')) {
				cmdLineParameters.portalHostname = line.getOptionValue('z');
			} else {
				throw new Exception("Please supply -z");
			}

			if (line.hasOption('o')) {

				String configFilename = line.getOptionValue('o');
				cmdLineParameters.outputFile = new File(configFilename);

				if (!cmdLineParameters.outputFile.exists()) {
					cmdLineParameters.outputFile.createNewFile();
				}

				if (!cmdLineParameters.outputFile.canWrite()) {
					throw new Exception("The given file " + cmdLineParameters.xmlFile.getAbsolutePath() + " is not writable!");
				}

				log.info("Using output file: {}", cmdLineParameters.outputFile.getAbsolutePath());

			} else {
				throw new Exception("Please supply -o");
			}

			if (line.hasOption('O')) {

				String wisemlOutputFilename = line.getOptionValue('O');
				cmdLineParameters.wisemlOutputFile = new File(wisemlOutputFilename);

				if (!cmdLineParameters.wisemlOutputFile.exists()) {
					cmdLineParameters.wisemlOutputFile.createNewFile();
				}

				if (!cmdLineParameters.wisemlOutputFile.canWrite()) {
					throw new Exception("The given file " + cmdLineParameters.wisemlOutputFile.getAbsolutePath() + " is not writable!");
				}

				log.info("Using WiseML output file: {}", cmdLineParameters.wisemlOutputFile.getAbsolutePath());

			} else {
				throw new Exception("Please supply -O");
			}

			if (line.hasOption('f')) {

				String xmlFilename = line.getOptionValue('f');
				cmdLineParameters.xmlFile = new File(xmlFilename);

				if (!cmdLineParameters.xmlFile.exists()) {
					throw new Exception("The given file name " + cmdLineParameters.xmlFile.getAbsolutePath() + " does not exist!");
				}

				if (!cmdLineParameters.xmlFile.canRead()) {
					throw new Exception("The given file " + cmdLineParameters.xmlFile.getAbsolutePath() + " is not readable!");
				}

			} else {
				throw new Exception("Please supply -f");
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
		cmdLineOptions.addOption("f", "file", true, "The (XML) configuration file");
		cmdLineOptions.addOption("v", "verbose", false, "Verbose logging output (equal to -l DEBUG)");
		cmdLineOptions.addOption("l", "logging", true,
				"Set logging level (one of [" + Level.TRACE + "," + Level.DEBUG + "," + Level.INFO + "," + Level.WARN + "," + Level.ERROR + "])"
		);
		cmdLineOptions.addOption("h", "help", false, "Help output");
		cmdLineOptions.addOption("p", "prefix", true, "Name of the testbed prefix for which to generate the config file");
		cmdLineOptions.addOption("o", "output", true, "Name of the config file to be written");
		cmdLineOptions.addOption("O", "wisemloutput", true, "Name of the WiseML file to be generated");
		cmdLineOptions.addOption("i", "internal", true,
				"The testbed internal server connection to be opened by the portal (e.g. localhost:8080)"
		);
		cmdLineOptions.addOption("s", "sessionmanagementendpointurl", true,
				"The endpoint URL of the session management API instance of the portal server"
		);
		cmdLineOptions.addOption("w", "wsninstancebaseurl", true,
				"The base URL of the WSN API instances of the portal server to which the random suffix is attached "
						+ "upon instance creation"
		);
		cmdLineOptions.addOption("n", "wisemlfilename", true,
				"The WiseML file to be used by the getNetwork() function on the portal server"
		);
		cmdLineOptions.addOption("a", "autodetection", false, "Use device port auto detection by MAC address");
		cmdLineOptions.addOption("z", "portalhostname", true, "The hostname of the portal server");
		cmdLineOptions.addOption("x", "hex", false, "If set generate hex numbers for node IDs");

	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(CSV2Config.class.getCanonicalName(), options);
		System.exit(1);
	}

}
