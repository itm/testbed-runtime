package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.*;
import eu.wisebed.api.v3.wsn.ChannelPipelinesMap;
import eu.wisebed.wiseml.Wiseml;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public interface ExperimentResource {

	@GET
	@Path("network")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	Wiseml getNetwork();

	@GET
	@Path("network.json")
	@Produces(MediaType.APPLICATION_JSON)
	Wiseml getNetworkAsJson();

	@GET
	@Path("network.xml")
	@Produces(MediaType.APPLICATION_XML)
	Wiseml getNetworkAsXml();

	@GET
	@Path("{secretReservationKeyBase64}/network")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	Wiseml getExperimentNetwork(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) throws Exception;

	@GET
	@Path("{secretReservationKeyBase64}/network.json")
	@Produces(MediaType.APPLICATION_JSON)
	Wiseml getExperimentNetworkAsJson(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) throws Exception;

	@GET
	@Path("{secretReservationKeyBase64}/network.xml")
	@Produces(MediaType.APPLICATION_XML)
	Wiseml getExperimentNetworkAsXml(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) throws Exception;

	@GET
	@Path("nodes")
	@Produces({MediaType.APPLICATION_JSON})
	NodeUrnList getNodes(@QueryParam("filter") String filter,
						 @QueryParam("capability") String capability);

	@GET
	@Consumes({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/nodeUrns")
	NodeUrnList getNodeUrns(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64)
			throws Exception;

	/**
	 * <code>
	 * {
	 * [
	 * {"nodeUrns" : ["urn:...:0x1234", "urn:...:0x2345", ...], "image" : base64-string },
	 * {"nodeUrns" : ["urn:...:0x1234", "urn:...:0x2345", ...], "image" : base64-string }
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
	Response flashPrograms(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						   FlashProgramsRequest flashData) throws Exception;

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
	Response flashProgramsStatus(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
			@PathParam("flashResponseTrackersIdBase64") String flashResponseTrackersIdBase64)
			throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/resetNodes")
	Response resetNodes(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						NodeUrnList nodeUrnList) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/getChannelPipelines")
	List<ChannelPipelinesMap> getChannelPipelines(
			@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
			NodeUrnList nodeUrnList) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("areNodesConnected")
	Response areNodesConnected(NodeUrnList nodeUrnList);

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/areNodesAlive")
	Response areNodesAlive(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						   NodeUrnList nodeUrnList) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/send")
	Response send(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
				  SendMessageData data) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/destroyVirtualLink")
	Response destroyVirtualLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
								TwoNodeUrns nodeUrns) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disableNode")
	Response disableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						 String nodeUrn) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enableNode")
	Response enableNode(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
						String nodeUrn) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/disablePhysicalLink")
	Response disablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
								 TwoNodeUrns nodeUrns) throws Exception;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("{secretReservationKeyBase64}/enablePhysicalLink")
	Response enablePhysicalLink(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
								TwoNodeUrns nodeUrns) throws Exception;
}
