package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.RoleDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.UserListDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public interface ShiroSNAARestResource {

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
    UserListDto listUsers();

	@POST
	@Path("users")
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(String name, String password, String salt);

}
