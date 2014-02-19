package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class PortalEventStoreModule extends PrivateModule {

    @Override
    protected void configure() {

        bind(PortalEventStoreService.class).to(PortalEventStoreServiceImpl.class).in(Singleton.class);
        install(new FactoryModuleBuilder()
                .implement(ReservationEventStore.class, ReservationEventStoreImpl.class)
                .build(ReservationEventStoreFactory.class)
        );

        bind(PortalEventStoreHelper.class).to(PortalEventStoreHelperImpl.class).in(Singleton.class);
        expose(PortalEventStoreService.class);
    }
}
