package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.FlashProgramsRequest;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.NodeUrnList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.RemoteExperimentConfiguration;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.exceptions.BadRemoteExperimentConfiguration;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.Base64Helper;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.fromJSON;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;

@Path("/experimentconfiguration/")
public class RemoteExperimentConfigurationResource {

	private static final Logger log = LoggerFactory.getLogger(RemoteExperimentConfigurationResource.class);

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public FlashProgramsRequest retrieveRemoteExperimentConfiguration(@QueryParam("url") String urlString) {

		if (urlString == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		try {
			URI url = new URI(urlString);
			final Response response = WebClient.create(url).accept(MediaType.MEDIA_TYPE_WILDCARD).get();
			final String entity = response.readEntity(String.class);
			final RemoteExperimentConfiguration remoteExperimentConfiguration =
					fromJSON(entity, RemoteExperimentConfiguration.class);

			FlashProgramsRequest flashProgramsRequest = new FlashProgramsRequest();
			flashProgramsRequest.flashTasks = new ArrayList<FlashProgramsRequest.FlashTask>();

			if (log.isDebugEnabled()) {
				log.debug("Got remote experiment description from {}: {}", urlString, toJSON(entity));
			}

			for (RemoteExperimentConfiguration.RemoteExperimentConfigurationEntry entry : remoteExperimentConfiguration.configurations) {

				FlashProgramsRequest.FlashTask flashTask = new FlashProgramsRequest.FlashTask();

				flashTask.nodeUrns = getNodeUrnList(new URL(url.toURL(), entry.nodeUrnsJsonFileUrl).toURI());
				flashTask.imageBase64 = "data:application/octet-stream;base64," + getBinaryProgramImage(
						new URL(url.toURL(), entry.binaryProgramUrl).toURI()
				);

				flashProgramsRequest.flashTasks.add(flashTask);
			}

			if (log.isTraceEnabled()) {
				log.debug("Returning experiment configuration url {}: {}", urlString, toJSON(flashProgramsRequest));
			}

			return flashProgramsRequest;

		} catch (URISyntaxException e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration(
					"Unable to convert remote experiment description from " + urlString, e
			);
		} catch (MalformedURLException e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration(
					"Unable to convert remote experiment description from " + urlString, e
			);
		} catch (Exception e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration(
					"Unable to convert remote experiment description from " + urlString, e
			);
		}
	}

	private List<String> getNodeUrnList(URI url) throws Exception {
		log.debug("Trying to node urns from {}", url.toString());
		NodeUrnList response = fromJSON(
				WebClient.create(url).accept(MediaType.WILDCARD).get().readEntity(String.class),
				NodeUrnList.class
		);
		log.debug("Got node urns from {}: {}", url.toString(), toJSON(response));
		return response.nodeUrns;
	}

	private String getBinaryProgramImage(URI binaryProgramUrl) {
		log.debug("Trying to read binary file from {}", binaryProgramUrl.toString());

		byte[] bytes = WebClient.create(binaryProgramUrl)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.get()
				.readEntity(byte[].class);

		if (bytes != null) {
			log.debug("Got {} bytes from {}", bytes.length, binaryProgramUrl.toString());
			return Base64Helper.encode(bytes);
		}

		return null;
	}
}
