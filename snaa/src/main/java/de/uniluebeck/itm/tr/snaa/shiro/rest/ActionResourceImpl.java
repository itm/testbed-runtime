package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.ActionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ActionDto;
import de.uniluebeck.itm.tr.snaa.shiro.dto.ShiroEntityDaoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class ActionResourceImpl implements ActionResource {

	private static final Logger log = LoggerFactory.getLogger(ActionResourceImpl.class);

	private final ActionDao actionsDao;

	@Inject
	public ActionResourceImpl(final ActionDao actionsDao) {
		this.actionsDao = actionsDao;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<ActionDto> listActions() {
		log.trace("ActionResourceImpl.listActions()");
		return ShiroEntityDaoConverter.toActionDtoList(actionsDao.find());
	}
}
