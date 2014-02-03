package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.snaa.shiro.dao.PermissionDao;
import de.uniluebeck.itm.tr.snaa.shiro.dto.DtoConverter;
import de.uniluebeck.itm.tr.snaa.shiro.dto.PermissionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class PermissionResourceImpl implements PermissionResource {

	private static final Logger log = LoggerFactory.getLogger(PermissionResourceImpl.class);

	private final PermissionDao permissionsDao;

	@Inject
	public PermissionResourceImpl(final PermissionDao permissionsDao) {
		this.permissionsDao = permissionsDao;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<PermissionDto> listPermissions() {
		log.trace("UserResourceImpl.listPermissions()");
		return DtoConverter.convertPermissionList(permissionsDao.find());
	}
}
