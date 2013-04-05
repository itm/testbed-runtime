package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.UnknownSecretReservationKeyException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Setup.Node;
import eu.wisebed.wiseml.Wiseml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.JobState;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static de.uniluebeck.itm.tr.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newFlashImagesRequest;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper.encode;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;
import static eu.wisebed.wiseml.WiseMLHelper.serialize;

/**
 * TODO: The following WISEBED functions are not implemented yet:
 * <p/>
 * List<String> getFilters();<br/>
 * List<ChannelHandlerDescription> getSupportedChannelHandlers();<br/>
 * String getVersion();<br/>
 * String setChannelPipeline(List<String> nodes, List<ChannelHandlerConfiguration> channelHandlerConfigurations);<br/>
 * String setVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance, List<String>
 * parameters,<br/>
 * List<String> filters);
 */
@Path("/experiments/")
public class ExperimentResource {

	private static final Logger log = LoggerFactory.getLogger(ExperimentResource.class);

	private static final Random RANDOM = new Random();

	private final TimedCache<Long, List<Long>> flashResponseTrackers;

	@Context
	private UriInfo uriInfo;

	private final DeviceDB deviceDB;

	private final ReservationManager reservationManager;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final RequestIdProvider requestIdProvider;

	@Inject
	public ExperimentResource(final DeviceDB deviceDB,
							  final ReservationManager reservationManager,
							  final ResponseTrackerFactory responseTrackerFactory,
							  final RequestIdProvider requestIdProvider,
							  final TimedCache<Long, List<Long>> flashResponseTrackers) {
		this.deviceDB = checkNotNull(deviceDB);
		this.reservationManager = checkNotNull(reservationManager);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.flashResponseTrackers = checkNotNull(flashResponseTrackers);
	}

	@GET
	@Path("network")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getNetworkJson() {
		return Response.ok(serialize(convertToWiseML(deviceDB.getAll()))).build();
	}

	@GET
	@Path("nodes")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getNodes(@QueryParam("filter") final String filter,
							 @QueryParam("capability") final String capability) {

		try {

			final Wiseml wiseml = convertToWiseML(deviceDB.getAll());

			NodeUrnList nodeList = new NodeUrnList();
			nodeList.nodeUrns = new LinkedList<String>();

			// First add all
			for (Node node : wiseml.getSetup().getNode()) {
				nodeList.nodeUrns.add(node.getId());
			}

			// Then remove non-matching ones
			for (Node node : wiseml.getSetup().getNode()) {
				boolean remove = false;
				String text = "" + node.getDescription() + " " + node.getId() + " " + node.getNodeType() + " " + node
						.getProgramDetails() + " "
						+ toString(node.getCapability());

				if (filter != null && !text.contains(filter)) {
					remove = true;
				}

				if (capability != null) {
					if (!toString(node.getCapability()).contains(capability)) {
						remove = true;
					}
				}

				if (remove) {
					nodeList.nodeUrns.remove(node.getId());
				}
			}

			String jsonString = toJSON(nodeList);
			log.trace("Returning JSON representation of node list for filter {}: {}", filter, jsonString);
			return Response.ok(jsonString).build();

		} catch (Exception e) {
			if (e instanceof UncheckedTimeoutException) {
				return returnError(
						"Timeout while retrieving WiseML from testbed backend",
						e,
						Status.SERVICE_UNAVAILABLE
				);
			}
			return returnError("Unable to retrieve WiseML", e, Status.INTERNAL_SERVER_ERROR);
		}
	}

	private String toString(List<Capability> capabilities) {
		StringBuilder sb = new StringBuilder();
		for (Capability c : capabilities) {
			sb.append(toString(c));
		}
		return sb.toString();

	}

	private String toString(Capability c) {
		StringBuilder sb = new StringBuilder();
		if (c.getName() != null) {
			sb.append(c.getName()).append(" ");
		}

		if (c.getDatatype() != null && c.getDatatype().value() != null) {
			sb.append(c.getDatatype().value()).append(" ");
		}

		if (c.getDefault() != null) {
			sb.append(c.getDefault()).append(" ");
		}

		if (c.getUnit() != null && c.getUnit().value() != null) {
			sb.append(c.getUnit().value()).append(" ");
		}

		return sb.toString();
	}

	@GET
	@Path("network")
	@Produces({MediaType.APPLICATION_XML})
	public Response getNetworkXml() {
		return Response.ok(serialize(convertToWiseML(deviceDB.getAll()))).build();
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	public Response getInstance(SecretReservationKeyListRs reservationKey) {

		final List<SecretReservationKey> secretReservationKeys = reservationKey.reservations;
		final SecretReservationKey secretReservationKey = secretReservationKeys.get(0);

		try {

			final Reservation reservation = reservationManager.getReservation(secretReservationKey.getKey());

			URI location = UriBuilder
					.fromUri(uriInfo.getRequestUri())
					.path("{secretReservationKeyBase64}")
					.build(encode(reservation.getKey()));

			return Response.ok(location.toString()).location(location).build();

		} catch (ReservationUnknownException e) {
			return Response
					.status(Status.NOT_FOUND)
					.entity("No reservation with the given secret reservation keys could be found!")
					.build();
		}
	}

	/**
	 * <code>
	 * {
	 * [
	 * {"nodeUrns" : ["urn:...:0x1234", "urn:...:0x2345", ...], "imageBase64" : base64-string },
	 * {"nodeUrns" : ["urn:...:0x1234", "urn:...:0x2345", ...], "imageBase64" : base64-string }
	 * ]
	 * }
	 * </code>
	 *
	 * @param secretReservationKeyBase64
	 * 		the base64-encoded URL of the experiment
	 * @param flashData
	 * 		the data to flash onto the nodes
	 *
	 * @return a response
	 */
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/flash")
	public Response flashPrograms(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64,
								  final FlashProgramsRequest flashData) {

		final Reservation reservation = getReservation(secretReservationKeyBase64);

		long flashResponseTrackersId = RANDOM.nextLong();
		synchronized (flashResponseTrackers) {
			while (flashResponseTrackers.containsKey(flashResponseTrackersId)) {
				flashResponseTrackersId = RANDOM.nextLong();
			}
			flashResponseTrackers.put(flashResponseTrackersId, Lists.<Long>newArrayList());
		}

		for (FlashProgramsRequest.FlashTask flashTask : flashData.flashTasks) {

			final long requestId = requestIdProvider.get();
			final Request request = newFlashImagesRequest(
					reservation.getKey(),
					requestId,
					Iterables.transform(flashTask.nodeUrns, NodeUrnHelper.STRING_TO_NODE_URN),
					extractByteArrayFromDataURL(flashTask.imageBase64)
			);

			synchronized (flashResponseTrackers) {
				flashResponseTrackers.get(flashResponseTrackersId).add(requestId);
			}

			reservation.createResponseTracker(request);
			reservation.getEventBus().post(request);
		}

		// remember response trackers, make them available via URL, redirect callers to this URL
		URI location = UriBuilder.fromUri(uriInfo.getRequestUri()).path("{requestId}")
				.build(encode(Long.toString(flashResponseTrackersId)));

		return Response.ok(location.toString()).location(location).build();
	}

	private Reservation getReservation(final String secretReservationKeyBase64) {
		final String secretReservationKey = Base64Helper.decode(secretReservationKeyBase64);
		try {
			return reservationManager.getReservation(secretReservationKey);
		} catch (ReservationUnknownException e) {
			throw new UnknownSecretReservationKeyException(secretReservationKey);
		}
	}

	private Response createExperimentNotFoundResponse(final String secretReservationKeyBase64) {
		return Response.status(Status.BAD_REQUEST)
				.entity("An experiment with the experimentUrl " + secretReservationKeyBase64 + " has not been found! Did you POST to /experiments before?")
				.build();
	}

	private byte[] extractByteArrayFromDataURL(String dataURL) {
		// data:[<mediatype>][;base64]
		int commaPos = dataURL.indexOf(',');
		String header = dataURL.substring(0, commaPos);
		if (!header.endsWith("base64")) {
			throw new RuntimeException("Data URLs are only supported with base64 encoding!");
		}
		return Base64.decode(dataURL.substring(commaPos + 1).getBytes());
	}

	/**
	 * Response looks like: <code>
	 * {
	 * "status" :
	 * [
	 * "urn:wisebed:...." : 100,
	 * "urn:wisebed:...." : -1,
	 * ]
	 * }
	 * </code>
	 *
	 * @param secretReservationKeyBase64
	 * 		the base64-encoded URL of the experiment
	 * @param requestIdBase64
	 * 		the base64-encoded requestId of the flash operation
	 *
	 * @return the current state of the flash operation
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/flash/{requestIdBase64}")
	public Response flashProgramsStatus(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64,
										@PathParam("requestIdBase64") final String requestIdBase64) {

		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		String requestId = Base64Helper.decode(requestIdBase64);

		WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
		if (wsnProxy == null) {
			return createExperimentNotFoundResponse(secretReservationKeyBase64);
		}

		Job job = wsnProxyManagerService.getJob(experimentUrl, requestId);

		if (job == null) {
			return Response.status(Status.NOT_FOUND).entity("No job with requestId " + requestId + " found!").build();
		}

		OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());

		return Response.ok(toJSON(operationStatusMap)).build();
	}

	private OperationStatusMap buildNodeUrnStatusMap(final Map<String, JobNodeStatus> jobNodeStates) {
		OperationStatusMap operationStatusMap = new OperationStatusMap();
		operationStatusMap.operationStatus = new HashMap<String, JobNodeStatus>();
		for (Map.Entry<String, JobNodeStatus> entry : jobNodeStates.entrySet()) {
			operationStatusMap.operationStatus.put(entry.getKey(), entry.getValue());
		}
		return operationStatusMap;
	}

	private OperationStatusMap buildNodeUrnTimeoutMap(final NodeUrnList nodeUrns) {
		OperationStatusMap operationStatusMap = new OperationStatusMap();
		operationStatusMap.operationStatus = new HashMap<String, JobNodeStatus>();
		for (String nodeUrn : nodeUrns.nodeUrns) {
			operationStatusMap.operationStatus.put(nodeUrn, new JobNodeStatus(JobState.FAILED, -1, "Timeout"));
		}
		return operationStatusMap;
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/resetNodes")
	public Response resetNodes(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64, NodeUrnList nodeUrns) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to reset nodes: {}", nodeUrns);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}

			Job job;
			try {

				job = wsnProxy.resetNodes(nodeUrns.nodeUrns, config.operationTimeoutMillis, TimeUnit.MILLISECONDS)
						.get();

			} catch (ExecutionException e) {
				if (e.getCause() instanceof TimeoutException) {
					log.warn("Resetting of (some of?) the nodes {} timed out!", nodeUrns.nodeUrns);
					OperationStatusMap operationStatusMap = buildNodeUrnTimeoutMap(nodeUrns);
					return Response.ok(toJSON(operationStatusMap)).build();
				} else {
					throw propagate(e);
				}
			}

			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/areNodesAlive")
	public Response areNodesAlive(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64, NodeUrnList nodeUrns) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to check for alive nodes: {}", nodeUrns);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			Job job = wsnProxy.areNodesAlive(nodeUrns.nodeUrns, config.operationTimeoutMillis, TimeUnit.MILLISECONDS)
					.get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/send")
	public Response send(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64, SendMessageData data) {

		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to send data:  {}", data);

		try {

			Message message = new Message();
			message.setBinaryData(Base64.decode(data.bytesBase64));
			message.setSourceNodeId(data.sourceNodeUrn);
			message.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}

			Job job = wsnProxy.send(data.targetNodeUrns, message, config.operationTimeoutMillis, TimeUnit.MILLISECONDS)
					.get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());

			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError("Exception while trying to send message downstream", e, Status.INTERNAL_SERVER_ERROR);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/destroyVirtualLink")
	public Response destroyVirtualLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
									   TwoNodeUrns nodeUrns) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to destroy virtual link:  {}->{}", nodeUrns.from, nodeUrns.to);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			Job job = wsnProxy.destroyVirtualLink(nodeUrns.from, nodeUrns.to, config.operationTimeoutMillis,
					TimeUnit.MILLISECONDS
			).get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disableNode")
	public Response disableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64, String nodeUrn) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to disable node:  {}", nodeUrn);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			Job job = wsnProxy.disableNode(nodeUrn, config.operationTimeoutMillis, TimeUnit.MILLISECONDS).get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enableNode")
	public Response enableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64, String nodeUrn) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to enable node:  {}", nodeUrn);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}

			Job job = wsnProxy.enableNode(nodeUrn, config.operationTimeoutMillis, TimeUnit.MILLISECONDS).get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disablePhysicalLink")
	public Response disablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
										TwoNodeUrns nodeUrns) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to disable physical link:  {}->{}", nodeUrns.from, nodeUrns.to);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			Job job = wsnProxy.disablePhysicalLink(nodeUrns.from, nodeUrns.to, config.operationTimeoutMillis,
					TimeUnit.MILLISECONDS
			).get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enablePhysicalLink")
	public Response enablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
									   TwoNodeUrns nodeUrns) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);
		log.debug("Received request to enable physical link:  {}->{}", nodeUrns.from, nodeUrns.to);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}

			Job job = wsnProxy.enablePhysicalLink(nodeUrns.from, nodeUrns.to, config.operationTimeoutMillis,
					TimeUnit.MILLISECONDS
			).get();
			OperationStatusMap operationStatusMap = buildNodeUrnStatusMap(job.getJobNodeStates());
			return Response.ok(toJSON(operationStatusMap)).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/network")
	public Response getExperimentNetworkJson(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			String wisemlString = wsnProxy.getNetwork().get();

			JAXBContext jaxbContext = JAXBContext.newInstance(Wiseml.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Wiseml wiseml = (Wiseml) unmarshaller.unmarshal(new StringReader(wisemlString));

			String json = toJSON(wiseml);
			log.trace("Returning network for experiment {} as json: {}", experimentUrl, json);
			return Response.ok(json).build();

		} catch (JAXBException e) {
			return returnError("Unable to retrieve WiseML", e, Status.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}
	}

	@GET
	@Path("{secretReservationKeyBase64}/network")
	@Produces({MediaType.APPLICATION_XML})
	public Response getExperimentNetworkXml(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) {
		String experimentUrl = Base64Helper.decode(secretReservationKeyBase64);

		try {

			WsnProxyService wsnProxy = wsnProxyManagerService.get(experimentUrl);
			if (wsnProxy == null) {
				return createExperimentNotFoundResponse(secretReservationKeyBase64);
			}
			String wisemlString = wsnProxy.getNetwork().get();
			log.trace("Returning network for experiment {} as xml: {}", experimentUrl, wisemlString);
			return Response.ok(wisemlString).build();

		} catch (Exception e) {
			return returnError(
					String.format("No such experiment: %s (decoded: %s)", secretReservationKeyBase64, experimentUrl), e,
					Status.INTERNAL_SERVER_ERROR
			);
		}

	}

	private Response returnError(String msg, Exception e, Status status) {
		log.debug(msg + " :" + e, e);
		String errorMessage = String.format("%s: %s (%s)", msg, e, e.getMessage());
		return Response.status(status).entity(errorMessage).build();
	}

}
