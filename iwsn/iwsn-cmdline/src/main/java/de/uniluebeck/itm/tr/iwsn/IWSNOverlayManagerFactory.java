package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;

public interface IWSNOverlayManagerFactory {

	IWSNOverlayManager create(final DOMObserver domObserver);

}
