package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.common.rest.RestApplicationBase;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

public class RestApiApplication extends RestApplicationBase {

	private final ExperimentResource experimentResource;

	private final RemoteExperimentConfigurationResource remoteExperimentConfigurationResource;

	private final RsResource rsResource;

	private final SnaaResource snaaResource;

	private final CookieResource cookieResource;

	private final TestbedsResource testbedsResource;

	@Inject
	public RestApiApplication(final ExperimentResource experimentResource,
							  final RemoteExperimentConfigurationResource remoteExperimentConfigurationResource,
							  final RsResource rsResource,
							  final SnaaResource snaaResource,
							  final CookieResource cookieResource,
							  final TestbedsResource testbedsResource) {

		this.experimentResource = checkNotNull(experimentResource);
		this.remoteExperimentConfigurationResource = checkNotNull(remoteExperimentConfigurationResource);
		this.rsResource = checkNotNull(rsResource);
		this.snaaResource = checkNotNull(snaaResource);
		this.cookieResource = checkNotNull(cookieResource);
		this.testbedsResource = checkNotNull(testbedsResource);
	}

	@Override
	public Set<?> getSingletonsInternal() {
		return newHashSet(
				experimentResource,
				remoteExperimentConfigurationResource,
				rsResource,
				snaaResource,
				cookieResource,
				testbedsResource,
				new Base64ExceptionMapper(),
				new SNAAFaultExceptionMapper(),
				new RSFaultExceptionMapper(),
				new RSAuthenticationFaultExceptionMapper(),
				new SNAAAuthenticationFaultExceptionMapper(),
				new WSNAuthorizationFaultExceptionMapper(),
				new RSAuthorizationFaultExceptionMapper(),
				new ReservationConflictFaultExceptionMapper(),
				new RSUnknownSecretReservationKeyFaultExceptionMapper(),
				new RuntimeExceptionMapper(),
				new SMUnknownSecretReservationKeyFaultExceptionMapper(),
				new WebServiceExceptionMapper()
		);
	}
}