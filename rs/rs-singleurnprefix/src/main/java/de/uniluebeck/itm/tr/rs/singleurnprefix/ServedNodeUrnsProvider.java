package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import eu.wisebed.api.sm.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class ServedNodeUrnsProvider implements Provider<String[]> {

	private static final Logger log = LoggerFactory.getLogger(ServedNodeUrnsProvider.class);

	@Inject
	@Nullable
	private SessionManagement sessionManagement;

	@Override
	public String[] get() {
		try {
			final List<String> nodeUrns = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork());
			return nodeUrns.toArray(new String[nodeUrns.size()]);
		} catch (Exception e) {
			log.warn(
					"Could not contact session management endpoint {}! Skipping validity check of nodes to be reserved.",
					sessionManagement
			);
			throw new RuntimeException(e);
		}
	}
}