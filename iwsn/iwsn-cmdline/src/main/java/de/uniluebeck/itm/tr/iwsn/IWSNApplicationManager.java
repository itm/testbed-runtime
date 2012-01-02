package de.uniluebeck.itm.tr.iwsn;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.gtr.application.TestbedApplicationFactory;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserver;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverListener;
import de.uniluebeck.itm.tr.util.domobserver.DOMTuple;
import de.uniluebeck.itm.tr.xml.Application;
import de.uniluebeck.itm.tr.xml.Testbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class IWSNApplicationManager implements DOMObserverListener, Service {

	private static final Logger log = LoggerFactory.getLogger(IWSNApplicationManager.class);

	private final Map<String, TestbedApplication> applications = newHashMap();

	private final TestbedRuntime testbedRuntime;

	private final DOMObserver domObserver;

	private final String configurationNodeId;

	private ScheduledFuture<?> domObserverSchedule;

	private ScheduledExecutorService scheduler;

	IWSNApplicationManager(final TestbedRuntime testbedRuntime, final DOMObserver domObserver,
						   final String configurationNodeId) {

		this.domObserver = domObserver;
		this.testbedRuntime = testbedRuntime;
		this.configurationNodeId = configurationNodeId;
	}

	@Override
	public QName getQName() {
		return XPathConstants.NODE;
	}

	@Override
	public String getXPathExpression() {
		return "/*";
	}

	@Override
	public void onDOMChanged(final DOMTuple oldAndNew) {

		if (scheduler != null && !scheduler.isShutdown()) {

			scheduler.execute(new Runnable() {
				@Override
				public void run() {
					onDOMChangedInternal(oldAndNew);
				}
			}
			);
		} else {
			onDOMChangedInternal(oldAndNew);
		}
	}

	private void onDOMChangedInternal(final DOMTuple oldAndNew) {
		try {

			Object oldDOM = oldAndNew.getFirst();
			Object newDOM = oldAndNew.getSecond();

			List<Application> oldApplicationList = oldDOM == null ? null : unmarshal((Node) oldDOM);
			List<Application> newApplicationList = newDOM == null ? null : unmarshal((Node) newDOM);

			Set<String> oldApplications = getApplicationNames(oldApplicationList);
			Set<String> newApplications = getApplicationNames(newApplicationList);

			Set<String> commonApplications = newHashSet(Sets.intersection(oldApplications, newApplications));
			restartApplicationsIfConfigChanged(oldApplicationList, newApplicationList, commonApplications);

			Set<String> removedApplications = newHashSet(Sets.difference(oldApplications, newApplications));
			stopApplications(oldApplicationList, removedApplications);

			Set<String> addedApplications = newHashSet(Sets.difference(newApplications, oldApplications));
			startApplications(newApplicationList, addedApplications);

		} catch (Exception e) {
			log.error("Exception while processing runtime configuration changes: {}", getStackTraceAsString(e));
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDOMLoadFailure(final Throwable cause) {
		log.warn("Unable to load changed configuration file {}. Maybe it is corrupt? Ignoring changes! Cause: ",
				cause
		);
	}

	@Override
	public void onXPathEvaluationFailure(final XPathExpressionException cause) {
		log.error("Failed to evaluate XPath expression on configuration file. Maybe it is corrupt? "
				+ "Ignoring changes! Cause: ", cause
		);
	}

	private void restartApplicationsIfConfigChanged(final List<Application> oldApplications,
													final List<Application> newApplications,
													final Set<String> applicationsToPotentiallyRestart) {

		for (String application : applicationsToPotentiallyRestart) {

			final Application oldApplicationXml = getApplicationXml(oldApplications, application);
			final Application newApplicationXml = getApplicationXml(newApplications, application);

			final String oldApplicationName = oldApplicationXml.getName();
			final String newApplicationName = newApplicationXml.getName();
			final String oldApplicationFactory = oldApplicationXml.getFactoryclass();
			final String newApplicationFactory = newApplicationXml.getFactoryclass();
			final Node oldApplicationConfig = (Node) oldApplicationXml.getAny();
			final Node newApplicationConfig = (Node) newApplicationXml.getAny();

			final boolean nameChanged = !oldApplicationName.equals(newApplicationName);
			final boolean factoryClassChanged = !oldApplicationFactory.equals(newApplicationFactory);
			final boolean configurationChanged =
					(oldApplicationConfig == null && newApplicationConfig != null) ||
							(oldApplicationConfig != null && newApplicationConfig == null) ||
							(oldApplicationConfig != null && !oldApplicationConfig.isEqualNode(newApplicationConfig));

			checkState(applications.containsKey(oldApplicationName));

			if (factoryClassChanged || configurationChanged) {

				stopApplication(newApplicationXml);
				startApplication(newApplicationXml);

			} else if (nameChanged) {

				applications.put(newApplicationName, applications.remove(oldApplicationName));
			}
		}
	}

	private void startApplications(final List<Application> newApplicationList, final Set<String> addedApplications) {
		for (String addedApplication : addedApplications) {
			startApplication(getApplicationXml(newApplicationList, addedApplication));
		}
	}

	private void stopApplications(final List<Application> oldApplicationList, final Set<String> removedApplications) {
		for (String removedApplication : removedApplications) {
			stopApplication(getApplicationXml(oldApplicationList, removedApplication));
		}
	}

	private Application getApplicationXml(final List<Application> applicationList, final String application) {
		for (Application applicationXml : applicationList) {
			if (applicationXml.getName().equals(application)) {
				return applicationXml;
			}
		}
		throw new RuntimeException();
	}

	private Set<String> getApplicationNames(final List<Application> applicationList) {
		Set<String> set = newHashSet();

		if (applicationList == null) {
			return set;
		}

		for (Application application : applicationList) {

			if (set.contains(application.getName())) {
				log.warn("An application with the name \"{}\" already exists. Please make sure each application tag "
						+ "has a unique name!", application.getName()
				);
			}

			set.add(application.getName());
		}
		return set;
	}

	private List<Application> unmarshal(final Node rootNode) throws JAXBException {

		JAXBContext context = JAXBContext.newInstance(Testbed.class.getPackage().getName());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Testbed testbed;
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (rootNode) {
			testbed = unmarshaller.unmarshal(rootNode, Testbed.class).getValue();
		}

		for (de.uniluebeck.itm.tr.xml.Node node : testbed.getNodes()) {
			if (configurationNodeId.equals(node.getId())) {
				return node.getApplications().getApplication();
			}
		}

		throw new RuntimeException("No configuration found for overlay node id " + configurationNodeId + "!");
	}

	private void startApplication(Application applicationXml) {

		checkState(!applications.containsKey(applicationXml.getName()));

		String applicationName = applicationXml.getName();
		String applicationFactoryClass = applicationXml.getFactoryclass();
		Object applicationConfig = applicationXml.getAny();

		try {


			log.debug("Creating factory \"{}\" for application \"{}\"", applicationFactoryClass, applicationName);
			TestbedApplicationFactory applicationFactory = (TestbedApplicationFactory) Class
					.forName(applicationFactoryClass)
					.newInstance();

			log.debug("Creating application \"{}\"", applicationName);
			TestbedApplication application = applicationFactory.create(
					testbedRuntime,
					applicationName,
					applicationConfig
			);

			log.info("Starting application \"{}\"", applicationName);
			if (application != null) {
				application.start();
				applications.put(applicationName, application);
			}

		} catch (Exception e) {
			log.error("Exception while starting application \"{}\"", applicationName, e);
		}
	}

	private void stopApplication(Application applicationXml) {

		String applicationName = applicationXml.getName();

		checkState(applications.containsKey(applicationName));

		log.info("Stopping application \"{}\"", applicationName);
		TestbedApplication testbedApplication = applications.get(applicationName);
		try {
			testbedApplication.stop();
		} catch (Exception e) {
			log.error("Exception while stopping application \"{}\": {}\n{}",
					new Object[]{applicationName, e.getMessage(), getStackTraceAsString(e)}
			);
		}
		applications.remove(applicationName);
	}

	@Override
	public void start() throws Exception {
		scheduler = Executors.newScheduledThreadPool(
				1,
				new ThreadFactoryBuilder().setNameFormat("ApplicationManager-Thread %d").build()
		);
		domObserver.addListener(this);
		domObserverSchedule = scheduler.scheduleWithFixedDelay(domObserver, 0, 3, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		for (TestbedApplication testbedApplication : applications.values()) {
			try {
				testbedApplication.stop();
			} catch (Exception e) {
				log.warn("TestbedApplication \"{}\" threw an Exception during shutdown: {}",
						testbedApplication.getName(), e
				);
			}
		}
		if (domObserverSchedule != null) {
			domObserverSchedule.cancel(false);
		}
		domObserver.removeListener(this);
		ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
	}
}
