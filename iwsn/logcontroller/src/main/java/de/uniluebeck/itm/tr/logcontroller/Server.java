package de.uniluebeck.itm.tr.logcontroller;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Main-class for the logcontroller
 */
public class Server {
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private static String propertyFile;
    private static ControllerService controller;

    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("f", "file", true, "Path to the configuration file");
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("f"))
                propertyFile = line.getOptionValue("f");
            else
                throw new MissingArgumentException("please supply argument -f");
        } catch (Exception e) {
            log.error("invalid commandline: {}", e);
            usage(options);
            System.exit(1);
        }

        Properties props = new Properties();
        try {
            log.debug("Loading PropertyFile: {}", propertyFile);
            props.load(new FileReader(propertyFile));
        } catch (IOException e) {
            log.error("Propertyfile {1} not found!", propertyFile);
            System.exit(1);
        }

        startReservationDummy();

        controller = new ControllerService();
        log.debug("Instance of {} created", controller.getClass().getSimpleName());
        try {
            controller.init(props);
            log.info("{} initalized", controller.getClass().getSimpleName());
            controller.startup();
            log.info("{} succesfully started", controller.getClass().getSimpleName());
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Received shutdown signal. Shutting down...");
                    try {
                        controller.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }));
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                controller.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * prints information about console-parameters
     * @param options
     */
    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("de.uniluebeck.itm.tr.logcontroller.Server", options);
    }

    private static void startReservationDummy()
    {
        Endpoint.publish("http://localhost:8081/reservation/",
                new ReservationDummy());
    }
}
