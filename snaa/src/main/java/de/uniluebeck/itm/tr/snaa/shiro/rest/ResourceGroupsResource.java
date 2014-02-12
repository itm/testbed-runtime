package de.uniluebeck.itm.tr.snaa.shiro.rest;

import de.uniluebeck.itm.tr.snaa.shiro.dto.ResourceGroupDto;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/resource_groups")
public interface ResourceGroupsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ResourceGroupDto> listResourceGroups();

}
