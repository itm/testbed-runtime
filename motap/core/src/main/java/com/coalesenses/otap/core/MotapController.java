package com.coalesenses.otap.core;

import com.coalesenses.otap.core.cli.OtapConfig;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: maxpagel
 * Date: 27.12.10
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */
public class MotapController {
    private static final long PRESENCE_DETECT_TIMEOUT = 20000;

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MotapController.class);
    final Object waitLock = new Object();

    public boolean executeProgramming(OtapPlugin otapPlugin, OtapConfig config) {
        otapPlugin.setPresenceDetectState(true);
        long startTime = System.currentTimeMillis();
        List<Long> presentDeviceIds = new Vector<Long>();
        Set<OtapDevice> presentDevices = new HashSet<OtapDevice>();

        while (System.currentTimeMillis() - startTime < PRESENCE_DETECT_TIMEOUT) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(1000);
                    for (Long mac : config.macs) {
                        for (OtapDevice otapDecive : otapPlugin.getPresenceDetect().getDetectedDevices()) {
                            Long id = new Long(otapDecive.getId());
                            if (id.longValue() == mac.longValue()) {
                                presentDeviceIds.add(id);
                                presentDevices.add(otapDecive);
                                break;
                            }
                        }
                    }
                    if (config.all) {
                        for (OtapDevice otapDecive : otapPlugin.getPresenceDetect().getDetectedDevices()) {
                            Long id = new Long(otapDecive.getId());
                            if (!presentDeviceIds.contains(id)) {
                                presentDeviceIds.add(id);
                                presentDevices.add(otapDecive);
                            }
                        }
                    }
                    log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
                    if (presentDeviceIds.size() == config.macs.size() && !config.all) {
                        log.info("found all devices");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        log.info("found the following {} devices: {}", presentDeviceIds.size(), presentDeviceIds);
        otapPlugin.setPresenceDetectState(false);
        if (presentDeviceIds.size() != 0 && (presentDeviceIds.size() == config.macs.size() || config.force)) {
            log.info("start programming");
            otapPlugin.setParticipatingDeviceList(presentDevices);
            otapPlugin.otapStart();
            try {
                while (otapPlugin.getState() != OtapPlugin.State.None) Thread.sleep(1000);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupted();
                return false;
            }
        } else {
            log.info("not all requested devices have been found. Aborting");
            return false;
        }
    }
}
