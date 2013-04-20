package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;

public class RestApiApplication extends Application {

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
							  final CookieResource cookieResource, final TestbedsResource testbedsResource) {
		this.experimentResource = checkNotNull(experimentResource);
		this.remoteExperimentConfigurationResource = checkNotNull(remoteExperimentConfigurationResource);
		this.rsResource = checkNotNull(rsResource);
		this.snaaResource = checkNotNull(snaaResource);
		this.cookieResource = checkNotNull(cookieResource);
		this.testbedsResource = checkNotNull(testbedsResource);
	}

	@Override
	public Set<Object> getSingletons() {

		final JSONProvider jsonProvider = new JSONProvider();
		jsonProvider.setAttributesToElements(true);
		jsonProvider.setIgnoreNamespaces(true);
		jsonProvider.setDropRootElement(true);
		jsonProvider.setSupportUnwrapped(true);
		jsonProvider.setDropCollectionWrapperElement(true);
		jsonProvider.setSerializeAsArray(true);

		return newHashSet(
				experimentResource,
				remoteExperimentConfigurationResource,
				rsResource,
				snaaResource,
				cookieResource,
				testbedsResource,
				jsonProvider,
				base64ExceptionExceptionMapper,
				snaaFaultExceptionExceptionMapper
		);
	}

	public static ExceptionMapper<Base64Exception> base64ExceptionExceptionMapper = new ExceptionMapper<Base64Exception>() {
		@Override
		public Response toResponse(final Base64Exception exception) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Request URL or payload contains data that is not correctly (or not) Base64-encoded. Error message: " +
							exception.getMessage()
					).build();
		}
	};

	public static ExceptionMapper<SNAAFault_Exception> snaaFaultExceptionExceptionMapper = new ExceptionMapper<SNAAFault_Exception>() {
		@Override
		public Response toResponse(final SNAAFault_Exception exception) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
		}
	};

}