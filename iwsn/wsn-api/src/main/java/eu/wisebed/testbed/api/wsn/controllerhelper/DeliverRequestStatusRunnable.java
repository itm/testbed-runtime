package eu.wisebed.testbed.api.wsn.controllerhelper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;

class DeliverRequestStatusRunnable extends DeliverRunnable {

	private Function<List<RequestStatus>, List<String>> extractRequestIdListFunction =
			new Function<List<RequestStatus>, List<String>>() {
				@Override
				public List<String> apply(final List<RequestStatus> requestStatuses) {
					if (requestStatuses == null) {
						return null;
					}
					List<String> requestIds = Lists.newArrayListWithCapacity(requestStatuses.size());
					for (RequestStatus requestStatus : requestStatuses) {
						requestIds.add(requestStatus.getRequestId());
					}
					return requestIds;
				}
			};

	private Function<List<?>, String> convertListToStringFunction =
			new Function<List<?>, String>() {
				@Override
				public String apply(final List<?> objects) {
					return objects == null ? null : Arrays.toString(objects.toArray());
				}
			};

	private Function<List<RequestStatus>, String> convertRequestStatusListToStringFunction = Functions.compose(
			convertListToStringFunction,
			extractRequestIdListFunction
	);

	private List<RequestStatus> requestStatusList;

	DeliverRequestStatusRunnable(final ScheduledThreadPoolExecutor scheduledExecutorService,
								 final DeliverFailureListener failureListener,
								 final String controllerEndpointUrl,
								 final Controller controllerEndpoint,
								 final List<RequestStatus> requestStatusList) {

		super(scheduledExecutorService, failureListener, controllerEndpointUrl, controllerEndpoint);
		this.requestStatusList = requestStatusList;
	}

	@Override
	protected void deliver(Controller controller) {

		if (log.isDebugEnabled()) {

			log.debug("StatusDelivery[requestIds={},endpointUrl={},queueSize={}]",
					new Object[]{
							convertRequestStatusListToStringFunction.apply(requestStatusList),
							controllerEndpointUrl,
							scheduler.getQueue().size()
					}
			);
		}

		controller.receiveStatus(requestStatusList);
	}
}