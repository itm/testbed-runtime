package com.coalesenses.serial;

import com.coalesenses.otap.core.MotapController;
import com.coalesenses.otap.core.OtapPlugin;
import com.coalesenses.otap.core.cli.AbstractOtapCLI;
import com.coalesenses.otap.core.cli.OtapConfig;
import com.coalesenses.otap.core.connector.DeviceConnector;
import com.coalesenses.otap.core.connector.DeviceConnectorListener;
import com.coalesenses.serial.connector.MotapDeviceFactory;
import de.uniluebeck.itm.motelist.MoteType;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 27.12.10
 * Time: 10:37
 * To change this template use File | Settings | File Templates.
 */
public class MotapCLI extends AbstractOtapCLI{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MotapCLI.class);

    private List<Long> macs;


    public static class Config extends OtapConfig{

        public String type;

        public String port;

        public Long macAddress;
    }

    public static void main(String[] args) {
        Logging.setLoggingDefaults();

        MotapCLI cli = new MotapCLI();
        final Object waitLock = new Object();

        final OtapPlugin otapPlugin;
        Config config = cli.parseCmdLine(args);


        // if user gave port
        DeviceConnector device = null;
        if (config.port != null && !"".equals(config.port)) {
            device = MotapDeviceFactory.create(config.type, config.port);
        } else if (config.macAddress != null) {
            device = MotapDeviceFactory.create(config.type, config.macAddress);
        }

        if (device == null) {
            log.error("Could not connect to device. Exiting...");
            System.exit(1);
        }

        final DeviceConnector finalDevice = device;
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                finalDevice.shutdown();
                            }
                        }));

        otapPlugin = new OtapPlugin(device);
        otapPlugin.setChannel(config.channel);
        otapPlugin.setMultihopSupportState(config.multihop);
        otapPlugin.setOtapKey(null, false);
        otapPlugin.setProgramFilename(config.program);


        device.addListener(
                new DeviceConnectorListener() {

                    @Override
                    public void handleDevicePacket(com.coalesenses.otap.core.seraerial.SerAerialPacket p) {
                        otapPlugin.handleDevicePacket(p);
                    }
                });

        MotapController motapController = new MotapController();
        motapController.executeProgramming(waitLock, otapPlugin, config);





        System.exit(0);


    }


    private Config parseCmdLine(String[] args) {

		Config config = new Config();
		Options options = createOptions();

		try {

			CommandLineParser parser = new PosixParser();
			CommandLine commandLine = parser.parse(options, args);

			super.parseCmdLine(commandLine, options, config);

            config.type = commandLine.getOptionValue('t');

            if (commandLine.hasOption('m')) {
                config.macAddress = StringUtils.parseHexOrDecLong(commandLine.getOptionValue('m'));
            }

            if (commandLine.hasOption('p')) {
                config.port = commandLine.getOptionValue('p');
            }

		} catch (Exception e) {
			log.error("Invalid command line: " + e, e);
			usage(options);
			System.exit(1);
		}

        assert config.type != null;
        assert config.port != null || config.macAddress != null;
        
		return config;

	}

    protected Options createOptions() {

		Options options = super.createOptions();

        // type
        Option typeOption =
                new Option("t", "type", true, "The type of the serial device. One of : " + MoteType.getTypesString());
        typeOption.setRequired(true);
        options.addOption(typeOption);

        // mac
        OptionGroup macOptionGroup = new OptionGroup();
        Option macOption =
                new Option("m", "mac", true, "The MAC address of the device (necessary for port auto-detection)");
        macOption.setRequired(true);
        macOptionGroup.addOption(macOption);
        options.addOptionGroup(macOptionGroup);

        // port
        OptionGroup portOptionGroup = new OptionGroup();
        Option portOption =
                new Option("p", "port", true, "The port to which the device is attached (e.g. /dev/ttyUSB0)");
        portOption.setRequired(true);
        portOptionGroup.addOption(portOption);
        options.addOptionGroup(portOptionGroup);

		return options;
	}



}

