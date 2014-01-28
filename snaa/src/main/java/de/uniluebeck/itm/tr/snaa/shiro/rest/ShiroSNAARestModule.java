package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.PrivateModule;

import javax.persistence.EntityManager;

public class ShiroSNAARestModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(EntityManager.class);

		bind(ShiroSNAARestService.class).to(ShiroSNAARestServiceImpl.class);
		bind(ActionResource.class).to(ActionResourceImpl.class);
		bind(UserResource.class).to(UserResourceImpl.class);
		bind(RoleResource.class).to(RoleResourceImpl.class);
		bind(PermissionResource.class).to(PermissionResourceImpl.class);

		expose(ShiroSNAARestService.class);
	}
}
