package de.uniluebeck.itm.tr.snaa.shiro.rest;

import com.google.inject.Inject;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class ShiroSNAARestResource {

	private final EntityManager entityManager;

	@Inject
	public ShiroSNAARestResource(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Context
	private UriInfo uriInfo;

	@GET
	public String get() {
		return "hello from " + uriInfo.getRequestUri();
	}

}
