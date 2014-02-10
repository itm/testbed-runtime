package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ActionDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class ActionResourceImpl implements ActionResource {

	private static final Logger log = LoggerFactory.getLogger(ActionResourceImpl.class);

	private final Provider<EntityManager> emProvider;

	@Inject
	public ActionResourceImpl(final Provider<EntityManager> emProvider) {
		this.emProvider = emProvider;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public List<ActionDto> listActions() {
		log.trace("ActionResourceImpl.listActions()");
		final EntityManager em = emProvider.get();
		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Action> query = criteriaBuilder.createQuery(Action.class);
		query.from(Action.class);
		return DtoConverter.convertActionList(em.createQuery(query).getResultList());
	}
}
