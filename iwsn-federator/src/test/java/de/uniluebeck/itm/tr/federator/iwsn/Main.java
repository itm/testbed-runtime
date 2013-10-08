package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherImpl;
import de.uniluebeck.itm.tr.common.PreconditionsFactory;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.util.SecureIdGenerator;
import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import de.uniluebeck.itm.util.scheduler.SchedulerServiceImpl;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.wsn.WSN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Main {

	static {
		Logging.setLoggingDefaults(LogLevel.TRACE);
	}

	@Mock
	private FederatedEndpoints<SessionManagement> fE;

	@Mock
	private PreconditionsFactory pF;

	@Mock
	private IWSNFederatorServiceConfig c;

	private ListeningExecutorService eS;

	@Mock
	private FederatedReservationManager rM;

	@Mock
	private ServedNodeUrnPrefixesProvider sNUPP;

	@Mock
	private ServedNodeUrnsProvider sNUP;

	@Mock
	private SessionManagementWisemlProvider wP;

	private ServicePublisher sP;

	private SessionManagementFederatorServiceImpl sm;

	@Mock
	private DeliveryManager dM;

	private FederatorControllerImpl fC;

	private SchedulerService sS;

	@Mock
	private SecureIdGenerator sIG;

	@Mock
	private FederatedEndpoints<WSN> wFE;

	@Mock
	private Set<NodeUrnPrefix> nUP;

	@Mock
	private Set<NodeUrn> nU;

	private WSNFederatorServiceImpl wF;

	@Before
	public void setUp() throws Exception {

		final URI value = URI.create("http://localhost:8080/soap/v3/sm");

		when(c.getFederatorSmEndpointUri()).thenReturn(value);
		when(c.getFederatorControllerEndpointUriBase()).thenReturn(value);
		when(c.getFederatorWsnEndpointUriBase()).thenReturn(value);

		sIG = new SecureIdGenerator();
		eS = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

		sP = new ServicePublisherImpl(new ServicePublisherConfig(8080));
		sP.startAndWait();

		sS = new SchedulerServiceImpl(-1, "Test");
		sS.startAndWait();

		sm = new SessionManagementFederatorServiceImpl(fE, pF, c, sP, eS, rM, sNUPP, sNUP, wP);
		fC = new FederatorControllerImpl(sP, dM, c, pF, sS, sIG, wFE, nUP, nU);
		wF = new WSNFederatorServiceImpl(sP, c, eS, pF, sIG, fC, wFE, nUP, nU);
	}

	@Test
	public void testName() throws Exception {

		sm.startAndWait();
		fC.startAndWait();
		wF.startAndWait();

		while (sm.isRunning()) {
			Thread.sleep(100);
		}
	}
}
