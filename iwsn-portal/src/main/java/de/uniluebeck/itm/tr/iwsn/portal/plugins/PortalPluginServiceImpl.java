package de.uniluebeck.itm.tr.iwsn.portal.plugins;

import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.common.plugins.PluginContainer;
import de.uniluebeck.itm.tr.common.plugins.PluginContainerFactory;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTracker;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

class PortalPluginServiceImpl extends AbstractService implements PortalPluginService {

    private static final Logger log = LoggerFactory.getLogger(PortalPluginServiceImpl.class);

    private final PluginContainerFactory pluginContainerFactory;

    private final PortalServerConfig portalServerConfig;

    private final RS rs;

    private final SNAA snaa;

    private final SessionManagement sessionManagement;

    private final PortalEventBus portalEventBus;

    private final ReservationManager reservationManager;

    private final DeviceDBService deviceDBService;

    private final ResponseTrackerFactory responseTrackerFactory;

    private final ServicePublisher servicePublisher;

    private final ServedNodeUrnsProvider servedNodeUrnsProvider;

    private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

    private final NodeStatusTracker nodeStatusTracker;

    private final MessageFactory messageFactory;

    private PluginContainer pluginContainer;

    @Inject
    public PortalPluginServiceImpl(final PluginContainerFactory pluginContainerFactory,
                                   final PortalServerConfig portalServerConfig,
                                   final RS rs,
                                   final SNAA snaa,
                                   final SessionManagement sessionManagement,
                                   final PortalEventBus portalEventBus,
                                   final ReservationManager reservationManager,
                                   final DeviceDBService deviceDBService,
                                   final ResponseTrackerFactory responseTrackerFactory,
                                   final ServicePublisher servicePublisher,
                                   final ServedNodeUrnsProvider servedNodeUrnsProvider,
                                   final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider,
                                   final NodeStatusTracker nodeStatusTracker,
                                   final MessageFactory messageFactory) {
        this.portalEventBus = checkNotNull(portalEventBus);
        this.reservationManager = checkNotNull(reservationManager);
        this.rs = checkNotNull(rs);
        this.snaa = checkNotNull(snaa);
        this.sessionManagement = checkNotNull(sessionManagement);
        this.pluginContainerFactory = checkNotNull(pluginContainerFactory);
        this.portalServerConfig = checkNotNull(portalServerConfig);
        this.deviceDBService = checkNotNull(deviceDBService);
        this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
        this.servicePublisher = checkNotNull(servicePublisher);
        this.servedNodeUrnsProvider = checkNotNull(servedNodeUrnsProvider);
        this.servedNodeUrnPrefixesProvider = checkNotNull(servedNodeUrnPrefixesProvider);
        this.nodeStatusTracker = checkNotNull(nodeStatusTracker);
        this.messageFactory = checkNotNull(messageFactory);
    }

    @Override
    protected void doStart() {
        try {

            log.trace("PortalPluginServiceImpl.doStart()");

            if (portalServerConfig.getPluginDirectory() != null && !""
                    .equals(portalServerConfig.getPluginDirectory())) {

                final File pluginDirectory = new File(portalServerConfig.getPluginDirectory());

                if (!pluginDirectory.isDirectory()) {
                    throw new IllegalArgumentException(pluginDirectory.getAbsolutePath() + " is not a directory!");
                }

                if (!pluginDirectory.canRead()) {
                    throw new IllegalArgumentException(pluginDirectory.getAbsolutePath() + " is not readable!");
                }

                final String extenderBundle = copyBundleToTmpFile(
                        "tr.plugins.framework-extender-portal-0.9-SNAPSHOT.jar"
                );

                pluginContainer = pluginContainerFactory.create(pluginDirectory.getAbsolutePath(), extenderBundle);

                pluginContainer.startAsync().awaitRunning();

                pluginContainer.registerService(PluginContainer.class, pluginContainer);
                pluginContainer.registerService(RS.class, rs);
                pluginContainer.registerService(SNAA.class, snaa);
                pluginContainer.registerService(SessionManagement.class, sessionManagement);
                pluginContainer.registerService(PortalEventBus.class, portalEventBus);
                pluginContainer.registerService(ReservationManager.class, reservationManager);
                pluginContainer.registerService(DeviceDBService.class, deviceDBService);
                pluginContainer.registerService(ResponseTrackerFactory.class, responseTrackerFactory);
                pluginContainer.registerService(ServicePublisher.class, servicePublisher);
                pluginContainer.registerService(ServedNodeUrnsProvider.class, servedNodeUrnsProvider);
                pluginContainer.registerService(ServedNodeUrnPrefixesProvider.class, servedNodeUrnPrefixesProvider);
                pluginContainer.registerService(NodeStatusTracker.class, nodeStatusTracker);
                pluginContainer.registerService(MessageFactory.class, messageFactory);
            }

            notifyStarted();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        try {
            log.trace("PortalPluginServiceImpl.doStop()");
            if (pluginContainer != null && pluginContainer.isRunning()) {
                pluginContainer.stopAsync().awaitTerminated();
            }
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    private String copyBundleToTmpFile(final String fileName) throws IOException {
        final URL resource = PortalPluginServiceImpl.class.getResource(fileName);
        final File tempDir = Files.createTempDir();
        final File tempFile = new File(tempDir, fileName);
        Resources.asByteSource(resource).copyTo(Files.asByteSink(tempFile, FileWriteMode.APPEND));
        return tempFile.getAbsolutePath();
    }
}
