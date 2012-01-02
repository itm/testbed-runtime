package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import de.uniluebeck.itm.gtr.LocalNodeNameManager;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.messaging.server.MessageServerService;
import de.uniluebeck.itm.gtr.naming.NamingEntry;
import de.uniluebeck.itm.gtr.naming.NamingService;
import de.uniluebeck.itm.gtr.routing.RoutingTableService;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMTuple;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IWSNOverlayManagerTest {

	private static final String ONE_NODE_ONE_NAME_NO_APPS =
			"de/uniluebeck/itm/tr/iwsn/cmdline/config-one-node-one-name-no-apps.xml";

	private static final String ONE_NODE_TWO_NAMES_NO_APPS =
			"de/uniluebeck/itm/tr/iwsn/cmdline/config-one-node-two-names-no-apps.xml";

	private static final String TWO_NODES_ONE_NAME_NO_APPS =
			"de/uniluebeck/itm/tr/iwsn/cmdline/config-two-nodes-one-name-no-apps.xml";

	@Mock
	private DOMObserver domObserverMock;

	@Mock
	private TestbedRuntime testbedRuntimeMock;

	@Mock
	private NamingService namingServiceMock;

	@Mock
	private RoutingTableService routingTableService;

	@Mock
	private LocalNodeNameManager localNodeNameManagerMock;

	@Mock
	private MessageServerService messageServerServiceMock;

	private IWSNOverlayManager listener;

	private Object newDOM;

	private Object oldDOM;

	@Before
	public void setUp() throws Exception {
		listener = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(TestbedRuntime.class).toInstance(testbedRuntimeMock);
				bind(IWSNOverlayManagerFactory.class).to(IWSNOverlayManagerFactoryImpl.class);
			}
		}
		).getInstance(IWSNOverlayManagerFactory.class).create(testbedRuntimeMock, domObserverMock, "localhost");

		when(testbedRuntimeMock.getNamingService()).thenReturn(namingServiceMock);
		when(testbedRuntimeMock.getRoutingTableService()).thenReturn(routingTableService);
		when(testbedRuntimeMock.getLocalNodeNameManager()).thenReturn(localNodeNameManagerMock);
		when(testbedRuntimeMock.getMessageServerService()).thenReturn(messageServerServiceMock);
	}

	@Test
	public void testIfNodeNamesAreAddedWhenThereIsAnEmptyConfigBefore() throws Exception {

		setListenerState(ONE_NODE_ONE_NAME_NO_APPS);

		verify(routingTableService, atLeastOnce()).setNextHop("urn:local:0xdcb0", "urn:local:0xdcb0");

		ArgumentCaptor<NamingEntry> namingEntryArgumentCaptor = ArgumentCaptor.forClass(NamingEntry.class);
		verify(namingServiceMock).addEntry(namingEntryArgumentCaptor.capture());
		assertEquals("urn:local:0xdcb0", namingEntryArgumentCaptor.getValue().getNodeName());
		assertEquals("tcp", namingEntryArgumentCaptor.getValue().getIface().getType());
		assertEquals("localhost:8888", namingEntryArgumentCaptor.getValue().getIface().getAddress());
	}

	@Test
	public void testIfNodeNamesAreAddedWhenThereIsAConfigBefore() throws Exception {

		setListenerState(ONE_NODE_ONE_NAME_NO_APPS);
		reset(routingTableService);

		setListenerState(ONE_NODE_TWO_NAMES_NO_APPS);

		verify(routingTableService, never()).setNextHop("urn:local:0xdcb0", "urn:local:0xdcb0");
		verify(routingTableService).setNextHop("urn:local:0xdcb1", "urn:local:0xdcb1");
	}

	@Test
	public void testIfNodeNamesAreRemovedWhenThereIsAConfigBefore() throws Exception {

		setListenerState(ONE_NODE_TWO_NAMES_NO_APPS);
		setListenerState(ONE_NODE_ONE_NAME_NO_APPS);

		verify(routingTableService).removeNextHop("urn:local:0xdcb1");
	}

	@Test
	public void testIfNodeIsAddedIfThereWasOnlyOneNodeBefore() throws Exception {

		verify(routingTableService, never()).setNextHop(Matchers.<String>any(), Matchers.<String>any());
		verify(namingServiceMock, never()).addEntry(Matchers.<NamingEntry>any());

		setListenerState(ONE_NODE_ONE_NAME_NO_APPS);

		ArgumentCaptor<NamingEntry> namingEntryArgumentCaptor = ArgumentCaptor.forClass(NamingEntry.class);
		verify(namingServiceMock).addEntry(namingEntryArgumentCaptor.capture());
		assertEquals("urn:local:0xdcb0", namingEntryArgumentCaptor.getValue().getNodeName());
		assertEquals("tcp", namingEntryArgumentCaptor.getValue().getIface().getType());
		assertEquals("localhost:8888", namingEntryArgumentCaptor.getValue().getIface().getAddress());

		reset(namingServiceMock);
		reset(routingTableService);

		setListenerState(TWO_NODES_ONE_NAME_NO_APPS);

		verify(routingTableService, atLeastOnce()).setNextHop("urn:local:0xdcb2", "urn:local:0xdcb2");

		ArgumentCaptor<NamingEntry> namingEntryArgumentCaptor2 = ArgumentCaptor.forClass(NamingEntry.class);
		verify(namingServiceMock).addEntry(namingEntryArgumentCaptor2.capture());
		assertEquals("urn:local:0xdcb2", namingEntryArgumentCaptor2.getValue().getNodeName());
		assertEquals("tcp", namingEntryArgumentCaptor2.getValue().getIface().getType());
		assertEquals("localhost2:8888", namingEntryArgumentCaptor2.getValue().getIface().getAddress());
	}

	private Object getDOMFromFile(final String fileName, final String xPathExpression, final QName qName)
			throws IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document document =
				dBuilder.parse(IWSNOverlayManagerTest.class.getClassLoader().getResourceAsStream(fileName));

		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		XPathExpression expression = xPath.compile(xPathExpression);

		return expression.evaluate(document, qName);
	}

	private void setListenerState(final String fileName)
			throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {

		oldDOM = newDOM;
		newDOM = getDOMFromFile(fileName, "/*", XPathConstants.NODE);

		listener.domObserverListener.onDOMChanged(new DOMTuple(oldDOM, newDOM));
	}
}
