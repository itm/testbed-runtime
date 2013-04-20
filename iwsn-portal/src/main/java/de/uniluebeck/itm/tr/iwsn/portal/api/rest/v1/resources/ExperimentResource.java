package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceDB;
import de.uniluebeck.itm.tr.iwsn.common.NodeUrnHelper;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.UnknownSecretReservationKeyException;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Setup.Node;
import eu.wisebed.wiseml.Wiseml;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.devicedb.WiseMLConverter.convertToWiseML;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.*;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper.*;
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

	private final DeviceDB deviceDB;

	private final ReservationManager reservationManager;

	private final RequestIdProvider requestIdProvider;

	@Context
	private UriInfo uriInfo;

	@Inject
	public ExperimentResource(final DeviceDB deviceDB,
							  final ReservationManager reservationManager,
							  final RequestIdProvider requestIdProvider,
							  final TimedCache<Long, List<Long>> flashResponseTrackers) {
		this.deviceDB = checkNotNull(deviceDB);
		this.reservationManager = checkNotNull(reservationManager);
		this.requestIdProvider = checkNotNull(requestIdProvider);
		this.flashResponseTrackers = checkNotNull(flashResponseTrackers);
	}

	/*@GET
	@Path("network")
	@Produces({MediaType.APPLICATION_JSON})
	public Wiseml getNetworkJson() {
		log.trace("ExperimentResource.getNetworkJson()");
		return convertToWiseML(deviceDB.getAll());
	}*/

	@GET
	@Path("network")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getNetwork() {
		log.trace("ExperimentResource.getNetwork()");
		return Response.ok(convertToWiseML(deviceDB.getAll())).build();
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
								  final FlashProgramsRequest flashData) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);

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
		URI location = UriBuilder
				.fromUri(uriInfo.getRequestUri())
				.path("{flashResponseTrackersIdBase64}")
				.build(encode(Long.toString(flashResponseTrackersId)));

		return Response.ok(location.toString()).location(location).build();
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
	 * @param flashResponseTrackersIdBase64
	 * 		the base64-encoded requestId of the flash operation
	 *
	 * @return the current state of the flash operation
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/flash/{flashResponseTrackersIdBase64}")
	public Response flashProgramsStatus(
			@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64,
			@PathParam("flashResponseTrackersIdBase64") final String flashResponseTrackersIdBase64)
			throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Long flashResponseTrackersId = Long.parseLong(decode(flashResponseTrackersIdBase64));

		if (!flashResponseTrackers.containsKey(flashResponseTrackersId)) {
			return Response
					.status(Status.NOT_FOUND)
					.entity("No flash job with request ID " + flashResponseTrackersId + " found!")
					.build();
		}

		final List<Long> requestIds = flashResponseTrackers.get(flashResponseTrackersId);
		return Response.ok(toJSON(buildOperationStatusMap(reservation, requestIds))).build();
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/resetNodes")
	public Response resetNodes(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
							   NodeUrnList nodeUrnList) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Iterable<NodeUrn> nodeUrns = Iterables.transform(nodeUrnList.nodeUrns, NodeUrnHelper.STRING_TO_NODE_URN);
		final Request request = newResetNodesRequest(reservation.getKey(), requestIdProvider.get(), nodeUrns);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/areNodesAlive")
	public Response areNodesAlive(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
								  NodeUrnList nodeUrnList) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Iterable<NodeUrn> nodeUrns = Iterables.transform(nodeUrnList.nodeUrns, NodeUrnHelper.STRING_TO_NODE_URN);
		final Request request = newAreNodesAliveRequest(reservation.getKey(), requestIdProvider.get(), nodeUrns);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/send")
	public Response send(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						 SendMessageData data) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Iterable<NodeUrn> nodeUrns = Iterables.transform(data.targetNodeUrns, NodeUrnHelper.STRING_TO_NODE_URN);
		final Request request = newSendDownstreamMessageRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				nodeUrns,
				decodeBytes(data.bytesBase64)
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/destroyVirtualLink")
	public Response destroyVirtualLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
									   TwoNodeUrns nodeUrns) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Multimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(new NodeUrn(nodeUrns.from), new NodeUrn(nodeUrns.to));
		final Request request = newDisableVirtualLinksRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				links
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disableNode")
	public Response disableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
								String nodeUrn) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Request request = newDisableNodesRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				newArrayList(new NodeUrn(nodeUrn))
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enableNode")
	public Response enableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
							   String nodeUrn) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Request request = newEnableNodesRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				newArrayList(new NodeUrn(nodeUrn))
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disablePhysicalLink")
	public Response disablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
										TwoNodeUrns nodeUrns) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Multimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(new NodeUrn(nodeUrns.from), new NodeUrn(nodeUrns.to));
		final Request request = newDisablePhysicalLinksRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				links
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enablePhysicalLink")
	public Response enablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
									   TwoNodeUrns nodeUrns) throws Base64Exception {

		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		final Multimap<NodeUrn, NodeUrn> links = HashMultimap.create();
		links.put(new NodeUrn(nodeUrns.from), new NodeUrn(nodeUrns.to));
		final Request request = newEnablePhysicalLinksRequest(
				reservation.getKey(),
				requestIdProvider.get(),
				links
		);

		return sendRequestAndGetOperationStatusMap(reservation, request, 10, TimeUnit.SECONDS);
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/network")
	public Response getExperimentNetworkJson(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) throws Base64Exception {
		return Response.ok(toJSON(getWiseml(secretReservationKeyBase64))).build();
	}

	@GET
	@Path("{secretReservationKeyBase64}/network")
	@Produces({MediaType.APPLICATION_XML})
	public Response getExperimentNetworkXml(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) throws Base64Exception {
		return Response.ok(serialize(getWiseml(secretReservationKeyBase64))).build();
	}

	private Wiseml getWiseml(final String secretReservationKeyBase64) throws Base64Exception {
		final Reservation reservation = getReservationOrThrow(secretReservationKeyBase64);
		return convertToWiseML(deviceDB.getConfigsByNodeUrns(reservation.getNodeUrns()).values());
	}

	private Response returnError(String msg, Exception e, Status status) {
		log.debug(msg + " :" + e, e);
		String errorMessage = String.format("%s: %s (%s)", msg, e, e.getMessage());
		return Response.status(status).entity(errorMessage).build();
	}

	private Reservation getReservationOrThrow(final String secretReservationKeyBase64) throws Base64Exception {
		final String secretReservationKey = decode(secretReservationKeyBase64);
		try {
			return reservationManager.getReservation(secretReservationKey);
		} catch (ReservationUnknownException e) {
			throw new UnknownSecretReservationKeyException(secretReservationKey);
		}
	}

	private byte[] extractByteArrayFromDataURL(String dataURL) {
		// data:[<mediatype>][;base64]
		int commaPos = dataURL.indexOf(',');
		String header = dataURL.substring(0, commaPos);
		if (!header.endsWith("base64")) {
			throw new RuntimeException("Data URLs are only supported with base64 encoding!");
		}
		final char[] chars = dataURL.toCharArray();
		final int offset = commaPos + 1;
		final int length = chars.length - offset;
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(length);
		try {
			Base64Utility.decode(chars, offset, length, byteArrayOutputStream);
		} catch (Base64Exception e) {
			throw propagate(e);
		}
		return byteArrayOutputStream.toByteArray();
	}

	private OperationStatusMap buildOperationStatusMap(final Reservation reservation, final Long requestId) {
		return buildOperationStatusMap(reservation, newArrayList(requestId));
	}

	private OperationStatusMap buildOperationStatusMap(final Reservation reservation, final List<Long> requestIds) {

		final OperationStatusMap operationStatusMap = new OperationStatusMap();
		operationStatusMap.operationStatus = new HashMap<String, JobNodeStatus>();

		for (long requestId : requestIds) {

			JobNodeStatus status;
			final ResponseTracker responseTracker = reservation.getResponseTracker(requestId);

			for (NodeUrn nodeUrn : responseTracker.keySet()) {

				if (responseTracker.get(nodeUrn).isDone()) {
					try {
						responseTracker.get(nodeUrn).get();
						status = new JobNodeStatus(JobState.SUCCESS, 100, null);
					} catch (Exception e) {
						status = new JobNodeStatus(JobState.FAILED, -1, e.getMessage());
					}
				} else {
					status = new JobNodeStatus(
							JobState.RUNNING,
							(int) (responseTracker.get(nodeUrn).getProgress() * 100),
							null
					);
				}

				operationStatusMap.operationStatus.put(nodeUrn.toString(), status);
			}
		}

		return operationStatusMap;
	}

	private Response sendRequestAndGetOperationStatusMap(final Reservation reservation, final Request request,
														 final int timeout, final TimeUnit timeUnit) {
		final ResponseTracker responseTracker = reservation.createResponseTracker(request);
		reservation.getEventBus().post(request);

		try {
			responseTracker.get(timeout, timeUnit);
		} catch (TimeoutException e) {
			return Response.ok(buildOperationStatusMap(reservation, request.getRequestId())).build();
		} catch (Exception e) {
			return Response.serverError().entity(e.getMessage()).build();
		}

		return Response.ok(buildOperationStatusMap(reservation, request.getRequestId())).build();
	}

}
