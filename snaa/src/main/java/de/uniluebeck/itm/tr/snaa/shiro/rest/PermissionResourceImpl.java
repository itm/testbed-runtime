package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter;
import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class PermissionResourceImpl implements PermissionResource {

	private static final Logger log = LoggerFactory.getLogger(PermissionResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Inject
	public PermissionResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<PermissionDto> listPermissions() {
		log.trace("UserResourceImpl.listPermissions()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Permission> query = criteriaBuilder.createQuery(Permission.class);
		query.from(Permission.class);
		return DtoConverter.convertPermissionList(em.createQuery(query).getResultList());
	}
}
