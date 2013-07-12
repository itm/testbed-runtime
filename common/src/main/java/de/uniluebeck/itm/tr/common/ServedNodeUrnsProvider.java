package de.uniluebeck.itm.tr.common;

import com.google.inject.Provider;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

public interface ServedNodeUrnsProvider extends Provider<Set<NodeUrn>> {

}
