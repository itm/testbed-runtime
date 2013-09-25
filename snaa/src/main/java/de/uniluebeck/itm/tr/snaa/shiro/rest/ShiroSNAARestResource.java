package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Action;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public interface ShiroSNAARestResource {

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
	List<User> listUsers();
	
	@GET
	@Path("roles")
	@Produces(MediaType.APPLICATION_JSON)
	List<Role> listRoles();
	
	@GET
	@Path("actions")
	@Produces(MediaType.APPLICATION_JSON)
	List<Action> listActions();
	
	@GET
	@Path("users/test")
	Response test();
	
	@POST
	@Path("users")
	@Consumes(MediaType.APPLICATION_JSON)
	Response addUser(String name, String password, String salt);

}
