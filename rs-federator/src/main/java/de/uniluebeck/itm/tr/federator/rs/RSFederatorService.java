package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.util.concurrent.Service;
import eu.wisebed.api.v3.rs.RS;

public interface RSFederatorService extends Service, RS {

	public static final String RS_FEDERATOR_EXECUTOR_SERVICE = "rsFederatorExecutorService";

}
