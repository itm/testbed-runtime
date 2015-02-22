package de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.EventStoreResource;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import org.apache.shiro.config.Ini;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventStoreAdminServiceImpl extends AbstractService implements EventStoreAdminService {

    private final ServicePublisher servicePublisher;
    private final EventStoreAdminApplication restApplication;
    private final CommonConfig commonConfig;

    private ServicePublisherService restService;

    @Inject
    public EventStoreAdminServiceImpl(final CommonConfig commonConfig,
                                      final ServicePublisher servicePublisher,
                                      final EventStoreAdminApplication restApplication) {
        this.commonConfig = checkNotNull(commonConfig);
        this.servicePublisher = checkNotNull(servicePublisher);
        this.restApplication = checkNotNull(restApplication);
    }

    @Override
    protected void doStart() {
        try {

            final Ini shiroRestIni = new Ini();
            shiroRestIni.addSection("urls");
            shiroRestIni.getSection("urls").put("/**", "authcBasic");
            shiroRestIni.addSection("users");
            shiroRestIni.getSection("users").put(commonConfig.getAdminUsername(), commonConfig.getAdminPassword());

            restService = servicePublisher.createJaxRsService(
                    Constants.EVENTSTORE.ADMIN_WEB_APP_CONTEXT_PATH,
                    restApplication,
                    shiroRestIni
            );
            restService.startAsync().awaitRunning();
            notifyStarted();

        } catch (Exception e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        try {
            notifyStopped();
        } catch (Exception e) {
            notifyFailed(e);
        }
    }
}
