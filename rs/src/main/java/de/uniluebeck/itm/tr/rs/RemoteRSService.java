package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

@WebService(
		name = "RS",
		endpointInterface = "eu.wisebed.api.v3.rs.RS",
		portName = "RSPort",
		serviceName = "RSService",
		targetNamespace = "http://wisebed.eu/api/v3/rs"
)
public class RemoteRSService extends AbstractService implements RSService {

	private final RSServiceConfig rsServiceConfig;

	private final RS rs;

	private final ServicePublisher servicePublisher;

	private ServicePublisherService jaxWsService;

	@Inject
	public RemoteRSService(final RSServiceConfig rsServiceConfig,
						   @DecoratedImpl final RS rs,
						   final ServicePublisher servicePublisher) {
		this.rsServiceConfig = rsServiceConfig;
		this.rs = rs;
		this.servicePublisher = servicePublisher;
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(rsServiceConfig.getRsContextPath(), this, null);
			jaxWsService.startAndWait();
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			if (jaxWsService != null && jaxWsService.isRunning()) {
				jaxWsService.stopAndWait();
			}
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "deleteReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.DeleteReservation")
	@ResponseWrapper(localName = "deleteReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.DeleteReservationResponse")
	public void deleteReservation(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws AuthorizationFault, RSFault_Exception, UnknownSecretReservationKeyFault, AuthenticationFault {
		checkState(isRunning());
		rs.deleteReservation(secretAuthenticationKeys, secretReservationKey);
	}

	@Override
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to,
			@WebParam(name = "offset", targetNamespace = "") final Integer offset,
			@WebParam(name = "amount", targetNamespace = "") final Integer amount)
			throws RSFault_Exception {
		checkState(isRunning());
		return rs.getReservations(from, to, offset, amount);
	}

	@Override
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to,
			@WebParam(name = "offset", targetNamespace = "") final Integer offset,
			@WebParam(name = "amount", targetNamespace = "") final Integer amount)
			throws AuthorizationFault, RSFault_Exception, AuthenticationFault {
		checkState(isRunning());
		return rs.getConfidentialReservations(secretAuthenticationKey, from, to, offset, amount);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservationData", targetNamespace = "")
	@RequestWrapper(localName = "getReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservation")
	@ResponseWrapper(localName = "getReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservationResponse")
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		checkState(isRunning());
		return rs.getReservation(secretReservationKey);
	}

	@Override
	@WebMethod
	@WebResult(name = "secretReservationKey", targetNamespace = "")
	@RequestWrapper(localName = "makeReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.MakeReservation")
	@ResponseWrapper(localName = "makeReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.MakeReservationResponse")
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "nodeUrns", targetNamespace = "") final
			List<NodeUrn> nodeUrns,
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to,
			@WebParam(name = "description", targetNamespace = "") final String description,
			@WebParam(name = "options", targetNamespace = "") final
			List<KeyValuePair> options)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception, AuthenticationFault {
		checkState(isRunning());
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to, description, options);
	}
}
