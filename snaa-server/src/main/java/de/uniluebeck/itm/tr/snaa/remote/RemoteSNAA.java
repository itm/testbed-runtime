package de.uniluebeck.itm.tr.snaa.remote;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherService;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

@WebService(
		name = "SNAA",
		endpointInterface = "eu.wisebed.api.v3.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://wisebed.eu/api/v3/snaa"
)
public class RemoteSNAA extends AbstractService implements SNAAService {

	private final SNAAServiceConfig snaaServiceConfig;

	private final SNAA snaa;

	private final ServicePublisher servicePublisher;

	private ServicePublisherService jaxWsService;

	@Inject
	public RemoteSNAA(final SNAAServiceConfig snaaServiceConfig, @DecoratedImpl final SNAA snaa,
					  final ServicePublisher servicePublisher) {
		this.snaaServiceConfig = snaaServiceConfig;
		this.snaa = snaa;
		this.servicePublisher = servicePublisher;
	}

	@Override
	protected void doStart() {
		try {
			jaxWsService = servicePublisher.createJaxWsService(snaaServiceConfig.getSnaaContextPath(), this);
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
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") final List<AuthenticationTriple> authenticationData)
			throws AuthenticationFault_Exception, SNAAFault_Exception {
		checkState(isRunning());
		return snaa.authenticate(authenticationData);
	}

	@Override
	public AuthorizationResponse isAuthorized(
			@WebParam(name = "usernameNodeUrnsMapList", targetNamespace = "") final
			List<UsernameNodeUrnsMap> usernameNodeUrnsMapList,
			@WebParam(name = "action", targetNamespace = "") final Action action) throws SNAAFault_Exception {
		checkState(isRunning());
		return snaa.isAuthorized(usernameNodeUrnsMapList, action);
	}

	@Override
	public List<ValidationResult> isValid(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys) throws SNAAFault_Exception {
		checkState(isRunning());
		return snaa.isValid(secretAuthenticationKeys);
	}
}
