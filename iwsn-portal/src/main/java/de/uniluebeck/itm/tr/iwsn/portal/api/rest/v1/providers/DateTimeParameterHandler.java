package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.joda.time.DateTime;

import javax.ws.rs.ext.Provider;

@Provider
public class DateTimeParameterHandler implements ParameterHandler<DateTime> {

	@Override
	public DateTime fromString(final String s) {
		return DateTime.parse(s);
	}
}