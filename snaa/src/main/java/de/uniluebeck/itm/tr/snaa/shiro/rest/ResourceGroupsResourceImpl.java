package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ResourceGroupDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.ResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroup;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourceGroupId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.RESOURCE_GROUP_TO_DTO_FUNCTION;
import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.convertResourceGroupList;

public class ResourceGroupsResourceImpl implements ResourceGroupsResource {

	private static final Logger log = LoggerFactory.getLogger(ResourceGroupsResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Context
	private UriInfo uriInfo;

	@Inject
	public ResourceGroupsResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<ResourceGroupDto> list() {
		log.trace("ResourceGroupsResourceImpl.list()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<ResourceGroup> query = criteriaBuilder.createQuery(ResourceGroup.class);
		query.from(ResourceGroup.class);
		final List<ResourceGroup> resultList = em.createQuery(query).getResultList();
		return convertResourceGroupList(resultList);
	}

	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Transactional
	public Response add(final ResourceGroupDto resourceGroupDto) {

		log.trace("ResourceGroupsResourceImpl.add({})", resourceGroupDto);

		final EntityManager em = emProvider.get();
		final ResourceGroup existing = em.find(ResourceGroup.class, resourceGroupDto.getName());

		if (resourceGroupDto.getName() == null || "".equals(resourceGroupDto.getName())) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Resource group is missing a name!")
					.build();
		}

		if (existing != null) {
			return Response
					.status(Response.Status.CONFLICT)
					.entity("Resource group named \"" + resourceGroupDto.getName() + "\" already exists!\n")
					.build();
		}

		final ResourceGroup resourceGroup = new ResourceGroup(resourceGroupDto.getName());
		final Set<UrnResourceGroup> urnResourceGroups = newHashSet();
		for (String nodeUrn : resourceGroupDto.getNodeUrns()) {
			final UrnResourceGroup urnResourceGroup = new UrnResourceGroup(
					new UrnResourceGroupId(nodeUrn, resourceGroupDto.getName()),
					resourceGroup
			);
			urnResourceGroups.add(urnResourceGroup);

		}
		resourceGroup.setUrnResourceGroups(urnResourceGroups);

		em.persist(resourceGroup);

		final URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).fragment(resourceGroupDto.getName()).build();
		final ResourceGroupDto dto = RESOURCE_GROUP_TO_DTO_FUNCTION.apply(resourceGroup);

		return Response.created(location).entity(dto).build();
	}

	@Override
	@PUT
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response update(@PathParam("name") final String name, final ResourceGroupDto resourceGroupDto) {

		log.trace("ResourceGroupsResourceImpl.update()");

		if (!name.equals(resourceGroupDto.getName())) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Resource group name in request URL and request body do not match!")
					.build();
		}

		final EntityManager em = emProvider.get();
		final ResourceGroup resourceGroup = em.find(ResourceGroup.class, name);

		final Set<String> nodeUrnStringsInDto = newHashSet(resourceGroupDto.getNodeUrns());
		final Set<String> nodeUrnStringsInEntity = newHashSet();

		for (UrnResourceGroup urnResourceGroup : resourceGroup.getUrnResourceGroups()) {
			nodeUrnStringsInEntity.add(urnResourceGroup.getId().getUrn());
		}

		final Set<String> nodeUrnStringsToAdd = Sets.difference(nodeUrnStringsInDto, nodeUrnStringsInEntity);
		final Set<String> nodeUrnStringsToRemove = Sets.difference(nodeUrnStringsInEntity, nodeUrnStringsInDto);

		final Set<UrnResourceGroup> nodeUrns = newHashSet();

		for (UrnResourceGroup urnResourceGroup : resourceGroup.getUrnResourceGroups()) {
			if (!nodeUrnStringsToRemove.contains(urnResourceGroup.getId().getUrn())) {
				nodeUrns.add(urnResourceGroup);
			} else {
				em.remove(urnResourceGroup);
			}
		}

		for (String nodeUrnString : nodeUrnStringsToAdd) {

			final UrnResourceGroupId id = new UrnResourceGroupId(nodeUrnString, name);
			final UrnResourceGroup existing = em.find(UrnResourceGroup.class, id);

			if (existing != null) {
				nodeUrns.add(existing);
			} else {
				nodeUrns.add(new UrnResourceGroup(id, resourceGroup));
			}
		}

		resourceGroup.setUrnResourceGroups(nodeUrns);

		return Response.ok(RESOURCE_GROUP_TO_DTO_FUNCTION.apply(resourceGroup)).build();
	}

	@Override
	@DELETE
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response delete(@PathParam("name") final String name) {

		log.trace("ResourceGroupsResourceImpl.delete({})", name);

		final EntityManager em = emProvider.get();
		final ResourceGroup resourceGroup = em.find(ResourceGroup.class, name);

		if (resourceGroup == null) {
			return Response.ok().build();
		}

		em.remove(resourceGroup);

		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
