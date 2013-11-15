package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import eu.wisebed.api.v3.wsn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Instances of this class make sure that WSN functionality are only provided for users which are authorized by a
 * provided {@link SNAA} component. The authorization is performed for each of the actions separately in case that some
 * actions are allowed for the user but others are not. Furthermore Exceptions will be thrown in case the action is not
 * allowed.
 */
public class AuthorizingWSNImpl implements AuthorizingWSN {

	/**
	 * Instance for logging debug, error, etc. messages
	 */
	private static final Logger log = LoggerFactory.getLogger(AuthorizingWSNImpl.class);

	/**
	 * WSN instance which actually provides the WSN functionality
	 */
	private final WSN delegate;

	/**
	 * The reservation which resulted in the instantiation of this class
	 */
	private final Reservation reservation;

	/**
	 * A component which is capable to check whether a certain action is authorized for a certain user
	 */
	private final SNAA snaa;


	/**
	 * Constructor
	 *
	 * @param snaa
	 * 		The component used to check whether a certain action is authorized for a certain user
	 * @param reservation
	 * 		A user's reservation
	 * @param wsnDelegate
	 * 		The WSN instance which actually provides the WSN functionality
	 */
	@Inject
	public AuthorizingWSNImpl(final SNAA snaa,
							  @Assisted final Reservation reservation,
							  @Assisted final WSN wsnDelegate) {
		this.snaa = snaa;
		this.delegate = wsnDelegate;
		this.reservation = reservation;
	}

	@Override
	public void addController(final String controllerEndpointUrl) throws AuthorizationFault {
		assertIsAuthorized(Action.WSN_ADD_CONTROLLER, reservation.getNodeUrns());
		delegate.addController(controllerEndpointUrl);
	}

	@Override
	public void enablePhysicalLinks(final long requestId, final List<Link> links)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		Set<NodeUrn> nodeUrnSet = new HashSet<NodeUrn>();
		for (Link link : links) {
			nodeUrnSet.add(link.getSourceNodeUrn());
			nodeUrnSet.add(link.getTargetNodeUrn());
		}
		assertIsAuthorized(Action.WSN_ENABLE_PHYSICAL_LINKS, nodeUrnSet);
		delegate.enablePhysicalLinks(requestId, links);
	}

	@Override
	public void resetNodes(final long requestId, final List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_RESET_NODES, nodeUrns);
		delegate.resetNodes(requestId, nodeUrns);
	}

	@Override
	public void disablePhysicalLinks(final long requestId, final List<Link> links)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		Set<NodeUrn> nodeUrnSet = new HashSet<NodeUrn>();
		for (Link link : links) {
			nodeUrnSet.add(link.getSourceNodeUrn());
			nodeUrnSet.add(link.getTargetNodeUrn());
		}
		assertIsAuthorized(Action.WSN_DISABLE_PHYSICAL_LINKS, nodeUrnSet);
		delegate.disablePhysicalLinks(requestId, links);
	}

	@Override
	public List<ChannelPipelinesMap> getChannelPipelines(final List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_GET_CHANNEL_PIPELINES, nodeUrns);
		return delegate.getChannelPipelines(nodeUrns);
	}

	@Override
	public void enableNodes(final long requestId, final List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		assertIsAuthorized(Action.WSN_ENABLE_NODES, nodeUrns);
		delegate.enableNodes(requestId, nodeUrns);
	}

	@Override
	public void send(final long requestId, final List<NodeUrn> nodeUrns, final byte[] message)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_SEND, nodeUrns);
		delegate.send(requestId, nodeUrns, message);
	}

	@Override
	public void disableVirtualization()
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_DISABLE_VIRTUALIZATION, reservation.getNodeUrns());
		delegate.disableVirtualization();
	}

	@Override
	public void removeController(final String controllerEndpointUrl) throws AuthorizationFault {
		assertIsAuthorized(Action.WSN_REMOVE_CONTROLLER, reservation.getNodeUrns());
		delegate.removeController(controllerEndpointUrl);
	}

	@Override
	public void setChannelPipeline(final long requestId, final List<NodeUrn> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_SET_CHANNEL_PIPELINE, nodeUrns);
		delegate.setChannelPipeline(requestId, nodeUrns, channelHandlerConfigurations);
	}

	@Override
	public void disableNodes(final long requestId, final List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		assertIsAuthorized(Action.WSN_DISABLE_NODES, nodeUrns);
		delegate.disableNodes(requestId, nodeUrns);
	}

	@Override
	public void disableVirtualLinks(final long requestId, final List<Link> links)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		Set<NodeUrn> nodeUrnSet = new HashSet<NodeUrn>();
		for (Link link : links) {
			nodeUrnSet.add(link.getSourceNodeUrn());
			nodeUrnSet.add(link.getTargetNodeUrn());
		}
		assertIsAuthorized(Action.WSN_DESTROY_VIRTUAL_LINKS, nodeUrnSet);
		delegate.disableVirtualLinks(requestId, links);
	}

	@Override
	public void setSerialPortParameters(final List<NodeUrn> nodeUrns, final SerialPortParameters parameters)
			throws ReservationNotRunningFault_Exception, RuntimeException, AuthorizationFault {
		assertIsAuthorized(Action.WSN_SET_SERIAL_PORT_PARAMETERS, nodeUrns);
		delegate.setSerialPortParameters(nodeUrns, parameters);

	}

	@Override
	public String getNetwork() throws eu.wisebed.api.v3.wsn.AuthorizationFault, RuntimeException {
		assertIsAuthorized(Action.WSN_GET_NETWORK, reservation.getNodeUrns());
		return delegate.getNetwork();
	}

	@Override
	public void enableVirtualization()
			throws VirtualizationNotSupportedFault_Exception, ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_ENABLE_VIRTUALIZATION, reservation.getNodeUrns());
		delegate.enableVirtualization();
	}

	@Override
	public void areNodesAlive(final long requestId, final List<NodeUrn> nodeUrns)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		assertIsAuthorized(Action.WSN_ARE_NODES_ALIVE, nodeUrns);
		delegate.areNodesAlive(requestId, nodeUrns);
	}

	@Override
	public void flashPrograms(final long requestId, final List<FlashProgramsConfiguration> configurations)
			throws ReservationNotRunningFault_Exception, AuthorizationFault {
		Set<NodeUrn> nodeUrnSet = new HashSet<NodeUrn>();
		for (FlashProgramsConfiguration configuration : configurations) {
			nodeUrnSet.addAll(configuration.getNodeUrns());
		}
		assertIsAuthorized(Action.WSN_FLASH_PROGRAMS, nodeUrnSet);
		delegate.flashPrograms(requestId, configurations);
	}

	@Override
	public void enableVirtualLinks(final long requestId, final List<VirtualLink> links)
			throws ReservationNotRunningFault_Exception, AuthorizationFault, VirtualizationNotEnabledFault_Exception {
		Set<NodeUrn> nodeUrnSet = new HashSet<NodeUrn>();
		for (VirtualLink link : links) {
			nodeUrnSet.add(link.getSourceNodeUrn());
			nodeUrnSet.add(link.getTargetNodeUrn());
		}
		assertIsAuthorized(Action.WSN_SET_VIRTUAL_LINK, nodeUrnSet);
		delegate.enableVirtualLinks(requestId, links);
	}

	/**
	 * Asserts that a certain action is authorized for a user.
	 *
	 * @param requestedAction
	 * 		The action to be authorized
	 * @param nodeUrnCollection
	 * 		The involved node urns
	 *
	 * @throws RuntimeException
	 * 		Thrown if the action is not authorized
	 */
	private void assertIsAuthorized(Action requestedAction, Collection<NodeUrn> nodeUrnCollection)
			throws AuthorizationFault {

		List<UsernameNodeUrnsMap> usernameNodeUrnsMapList = newArrayList();

		for (Reservation.Entry entry : reservation.getEntries()) {

			UsernameNodeUrnsMap usernameNodeUrnsMap = new UsernameNodeUrnsMap();
			usernameNodeUrnsMap.setUsername(entry.getUsername());
			usernameNodeUrnsMap.setUrnPrefix(entry.getNodeUrnPrefix());
			usernameNodeUrnsMap.getNodeUrns().addAll(nodeUrnCollection);
			usernameNodeUrnsMapList.add(usernameNodeUrnsMap);
		}

		final AuthorizationResponse authorized;
		try {

			authorized = snaa.isAuthorized(usernameNodeUrnsMapList, requestedAction);

			if (authorized.isAuthorized()) {

				log.debug("Requested action {} was authorized", requestedAction.toString());

			} else {

				StringBuilder sb = new StringBuilder("Requested action '");
				sb.append(requestedAction.toString());
				sb.append("' was NOT authorized: ");
				sb.append(authorized.getMessage());

				final eu.wisebed.api.v3.common.AuthorizationFault faultInfo =
						new eu.wisebed.api.v3.common.AuthorizationFault();
				final String value = sb.toString();
				faultInfo.setMessage(value);
				throw new AuthorizationFault(value, faultInfo);
			}

		} catch (SNAAFault_Exception e) {
			throw new RuntimeException("An exception occurred while requesting authorization from SNAA server", e);
		}
	}
}
