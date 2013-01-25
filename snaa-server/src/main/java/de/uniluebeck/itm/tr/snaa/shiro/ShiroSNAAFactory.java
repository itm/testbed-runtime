package de.uniluebeck.itm.tr.snaa.shiro;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.Set;

/**
 * Implementations of this interface create an {@link SNAA} server based on Apache Shiro.
 */
public interface ShiroSNAAFactory {

	/**
	 * Access authorization for users is performed for nodes which uniform resource locator starts
	 * with this prefix.
	 */

	/**
	 * Creates and returns an {@link SNAA} server based on Apache Shiro.
	 * 
	 * @param nodeUrnPrefixes
	 *            The created server performs authorization for actions on nodes which uniform
	 *            resource locator starts with theses prefixes.
	 * @return An {@link SNAA} server based on Apache Shiro
	 */
	ShiroSNAA create(Set<NodeUrnPrefix> nodeUrnPrefixes);

}
