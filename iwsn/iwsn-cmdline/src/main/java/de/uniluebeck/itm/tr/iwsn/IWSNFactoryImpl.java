package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.Inject;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.tr.util.CachingConvertingFileProvider;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverFactory;
import org.w3c.dom.Node;

import java.io.File;

import static de.uniluebeck.itm.tr.util.XmlFunctions.fileToRootElementFunction;

public class IWSNFactoryImpl implements IWSNFactory {

	@Inject
	private TestbedRuntime testbedRuntime;

	@Inject
	private DOMObserverFactory domObserverFactory;

	@Inject
	private IWSNOverlayManagerFactory overlayManagerFactory;

	@Inject
	private IWSNApplicationManagerFactory applicationManagerFactory;


	@Override
	public IWSN create(final File configurationFile, final String nodeId) {

		final CachingConvertingFileProvider<Node> newDOMProvider = new CachingConvertingFileProvider<Node>(
				configurationFile,
				fileToRootElementFunction()
		);

		final DOMObserver domObserver = domObserverFactory.create(
				newDOMProvider
		);

		final IWSNOverlayManager overlayManager = overlayManagerFactory.create(
				testbedRuntime,
				domObserver,
				nodeId
		);

		final IWSNApplicationManager applicationManager = applicationManagerFactory.create(
				testbedRuntime,
				domObserver,
				nodeId
		);

		return new IWSNImpl(
				testbedRuntime,
				applicationManager,
				overlayManager
		);
	}
}
