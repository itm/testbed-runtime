package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverListener;
import de.uniluebeck.itm.tr.util.domobserver.DOMTuple;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

public class IWSNServer implements Service {

	@Inject
	private TestbedRuntime overlay;

	@Inject
	private DOMObserver configObserver;

	private DOMObserverListener nodeNamesObserver = new DOMObserverListener() {
		@Override
		public QName getQName() {
			return XPathConstants.NODESET;
		}

		@Override
		public String getXPathExpression() {
			return "//nodes";
		}

		@Override
		public void onDOMChanged(final DOMTuple oldAndNew) {
			Object oldDOM = oldAndNew.getFirst();
			Object newDOM = oldAndNew.getSecond();
		}
	};

	@Override
	public void start() throws Exception {
		startObservingNodeNames();
		// TODO implement
	}

	@Override
	public void stop() {
		stopObservingNodeNames();
		// TODO implement
	}

	private void startObservingNodeNames() {
		// TODO implement
	}

	private void stopObservingNodeNames() {
		// TODO implement
	}
}
