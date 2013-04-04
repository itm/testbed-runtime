package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import eu.wisebed.restws.dto.FlashProgramsRequest;
import eu.wisebed.restws.dto.FlashProgramsRequest.FlashTask;
import eu.wisebed.restws.dto.NodeUrnList;
import eu.wisebed.restws.dto.RemoteExperimentConfiguration;
import eu.wisebed.restws.dto.RemoteExperimentConfiguration.RemoteExperimentConfigurationEntry;
import eu.wisebed.restws.exceptions.BadRemoteExperimentConfiguration;
import eu.wisebed.restws.util.Base64Helper;
import eu.wisebed.restws.util.InjectLogger;
import eu.wisebed.restws.util.JSONHelper;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Path("/" + Constants.WISEBED_API_VERSION + "/experimentconfiguration/")
public class RemoteExperimentConfigurationResource {

	@InjectLogger
	private Logger log;

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public FlashProgramsRequest retrieveRemoteExperimentConfiguration(@QueryParam("url") String urlString) {

		if (urlString == null)
			throw new WebApplicationException(Status.BAD_REQUEST);

		try {
			URI url = new URI(urlString);
			WebResource.Builder requestBuilder = Client.create().resource(url).accept(MediaType.MEDIA_TYPE_WILDCARD);
			RemoteExperimentConfiguration response = JSONHelper.fromJSON(requestBuilder.get(String.class), RemoteExperimentConfiguration.class);

			FlashProgramsRequest flashProgramsRequest = new FlashProgramsRequest();
			flashProgramsRequest.flashTasks = new ArrayList<FlashProgramsRequest.FlashTask>();

			log.debug("Got remote experiment description from {}: {}", urlString, JSONHelper.toJSON(response));

			for (RemoteExperimentConfigurationEntry entry : response.configurations) {
				FlashTask flashTask = new FlashTask();

				flashTask.nodeUrns = getNodeUrnList(new URL(url.toURL(), entry.nodeUrnsJsonFileUrl).toURI());
				flashTask.imageBase64 = "data:application/octet-stream;base64," + getBinaryProgramImage(new URL(url.toURL(), entry.binaryProgramUrl).toURI());

				flashProgramsRequest.flashTasks.add(flashTask);
			}

			if( log.isTraceEnabled())
				log.debug("Returning experiment configuration url {}: {}", urlString, JSONHelper.toJSON(flashProgramsRequest));
			
			return flashProgramsRequest;

		} catch (URISyntaxException e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration("Unable to convert remote experiment description from " + urlString, e);
		} catch (MalformedURLException e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration("Unable to convert remote experiment description from " + urlString, e);
		} catch (Exception e) {
			log.debug("Unable to convert remote experiment description from " + urlString, e);
			throw new BadRemoteExperimentConfiguration("Unable to convert remote experiment description from " + urlString, e);
		}
	}

	private List<String> getNodeUrnList(URI url) throws UniformInterfaceException, Exception {
		log.debug("Trying to node urns from {}", url.toString());
		WebResource.Builder requestBuilder = Client.create().resource(url).accept(MediaType.WILDCARD);
		NodeUrnList response = JSONHelper.fromJSON(requestBuilder.get(String.class), NodeUrnList.class);
		log.debug("Got node urns from {}: {}", url.toString(), JSONHelper.toJSON(response));
		return response.nodeUrns;
	}

	private String getBinaryProgramImage(URI binaryProgramUrl) {
		log.debug("Trying to read binary file from {}", binaryProgramUrl.toString());
		WebResource.Builder requestBuilder = Client.create().resource(binaryProgramUrl).accept(MediaType.APPLICATION_OCTET_STREAM);
		byte[] bytes = requestBuilder.get(byte[].class);

		if (bytes != null) {
			log.debug("Got {} bytes from {}", bytes.length, binaryProgramUrl.toString());
			return Base64Helper.encode(bytes);
		}

		return null;
	}
}
