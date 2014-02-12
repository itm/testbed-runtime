package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ResourceGroupDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.ResourceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter.convertResourceGroupList;

public class ResourceGroupsResourceImpl implements ResourceGroupsResource {

	private static final Logger log = LoggerFactory.getLogger(ResourceGroupsResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Inject
	public ResourceGroupsResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<ResourceGroupDto> listResourceGroups() {
		log.trace("ResourceGroupsResourceImpl.listResourceGroups()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<ResourceGroup> query = criteriaBuilder.createQuery(ResourceGroup.class);
		query.from(ResourceGroup.class);
		final List<ResourceGroup> resultList = em.createQuery(query).getResultList();
		return convertResourceGroupList(resultList);
	}
}
