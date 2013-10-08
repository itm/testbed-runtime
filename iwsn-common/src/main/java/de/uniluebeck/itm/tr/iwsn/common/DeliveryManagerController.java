package de.uniluebeck.itm.tr.iwsn.common;

import eu.wisebed.api.v3.controller.Controller;

import javax.annotation.Nullable;

public interface DeliveryManagerController extends Controller {

	/**
	 * Returns the endpoint URL of the controller if this controller is a real testbed client running a SOAP endpoint or
	 * {@code null} if this controller is e.g., internal.
	 *
	 * @return an endpoint URL or {@code null} if not applicable
	 */
	@Nullable
	String getEndpointUrl();

}
