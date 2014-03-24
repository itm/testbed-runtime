package de.uniluebeck.itm.tr.plugins.defaultimage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public interface NodeStatusTrackerResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	List<NodeStatusDto> getNodeStatusList();

}
