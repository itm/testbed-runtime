package de.uniluebeck.itm.tr.devicedb;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.Response;

public class Main {

	public static void main(String[] args) {
		final Client client = Client.create();
		final WebResource resource = client.resource("http://localhost:7654/rest/test")
				.queryParam("a", "1")
				.queryParam("a", "2");
		System.out.println(resource.toString());
		final Response response = resource.get(Response.class);
		System.out.println(response.toString());
	}
}
