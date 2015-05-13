package de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers.*;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class EventStoreAdminApplication extends Application {

    private final EventStoreAdminResource adminResource;

    @Inject
    public EventStoreAdminApplication(final EventStoreAdminResource adminResource) {
        this.adminResource = checkNotNull(adminResource);
    }

    @Override
    public Set<Object> getSingletons() {
        return newHashSet(
                adminResource,
                createJaxbElementProvider(),
                createJsonProvider(),
                new Base64ExceptionMapper(),
                new SNAAFaultExceptionMapper(),
                new RSFaultExceptionMapper(),
                new RSAuthenticationFaultExceptionMapper(),
                new SNAAAuthenticationFaultExceptionMapper(),
                new WSNAuthorizationFaultExceptionMapper(),
                new RSAuthorizationFaultExceptionMapper(),
                new ReservationConflictFaultExceptionMapper(),
                new RSUnknownSecretReservationKeyFaultExceptionMapper(),
                new SMUnknownSecretReservationKeyFaultExceptionMapper(),
                new WebServiceExceptionMapper(),
                new UserAlreadyExistsExceptionMapper(),
                new UserPwdMismatchExceptionMapper(),
                new UserUnknownExceptionMapper());
    }

    /**
     * Customized JAXB serialization provider to enable formatted pretty-print by default.
     *
     * @return a JAXB serialization provider
     */
    private JAXBElementProvider createJaxbElementProvider() {
        final JAXBElementProvider jaxbElementProvider = new JAXBElementProvider();
        final HashMap<Object, Object> marshallerProperties = newHashMap();
        marshallerProperties.put("jaxb.formatted.output", true);
        jaxbElementProvider.setMarshallerProperties(marshallerProperties);
        return jaxbElementProvider;
    }

    /**
     * Customized Jackson provider for JSON serialization.
     *
     * @return a serialization provider for JSON
     */
    private JacksonJsonProvider createJsonProvider() {
        final JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
        jsonProvider.setMapper(new ObjectMapper());
        return jsonProvider;
    }
}
