package de.uniluebeck.itm.tr.federator.rs;

import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.v3.rs.RS;

public interface RSFederatorFactory {

	RSFederator create(FederationManager<RS> federationManager);

}
