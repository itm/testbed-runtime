package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

import javax.persistence.EntityManager;

public class ShiroSNAARestModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(EntityManager.class);

		bind(ShiroSNAARestService.class).to(ShiroSNAARestServiceImpl.class).in(Scopes.SINGLETON);
		bind(ShiroSNAARestApplication.class).in(Scopes.SINGLETON);
		bind(ActionResource.class).to(ActionResourceImpl.class).in(Scopes.SINGLETON);
		bind(UserResource.class).to(UserResourceImpl.class).in(Scopes.SINGLETON);
		bind(RoleResource.class).to(RoleResourceImpl.class).in(Scopes.SINGLETON);
		bind(PermissionResource.class).to(PermissionResourceImpl.class).in(Scopes.SINGLETON);
		bind(ResourceGroupsResource.class).to(ResourceGroupsResourceImpl.class).in(Scopes.SINGLETON);

		expose(ShiroSNAARestService.class);
		expose(UserResource.class);
	}
}
