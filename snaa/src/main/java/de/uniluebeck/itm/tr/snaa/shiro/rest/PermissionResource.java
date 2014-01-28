package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/permissions")
public interface PermissionResource {

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PermissionDto> listPermissions();

}
