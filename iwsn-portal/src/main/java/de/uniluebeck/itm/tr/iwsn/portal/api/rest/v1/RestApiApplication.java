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

	private final TestbedsResource testbedsResource;

	private final CookieResource cookieResource;

	@Inject
	public RestApiApplication(final ExperimentResourceImpl experimentResource,
							  final CookieResource cookieResource,
							  final RemoteExperimentConfigurationResourceImpl remoteExperimentConfigurationResource,
							  final RsResourceImpl rsResource,
							  final SnaaResourceImpl snaaResource,
							  final TestbedsResourceImpl testbedsResource) {

		this.experimentResource = checkNotNull(experimentResource);
		this.cookieResource = checkNotNull(cookieResource);
		this.remoteExperimentConfigurationResource = checkNotNull(remoteExperimentConfigurationResource);
		this.rsResource = checkNotNull(rsResource);
		this.snaaResource = checkNotNull(snaaResource);
		this.testbedsResource = checkNotNull(testbedsResource);
	}

	@Override
	public Set<?> getSingletonsInternal() {
		return newHashSet(
				experimentResource,
				cookieResource,
				remoteExperimentConfigurationResource,
				rsResource,
				snaaResource,
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