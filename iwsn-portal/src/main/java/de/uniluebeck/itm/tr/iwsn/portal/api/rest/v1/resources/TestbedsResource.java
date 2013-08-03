package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedDescription;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface TestbedsResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	List<TestbedDescription> getTestbedList();
}
