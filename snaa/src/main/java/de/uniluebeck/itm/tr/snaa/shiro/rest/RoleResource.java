package de.uniluebeck.itm.tr.snaa.shiro.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/roles")
public interface RoleResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<String> listRoles();

}
