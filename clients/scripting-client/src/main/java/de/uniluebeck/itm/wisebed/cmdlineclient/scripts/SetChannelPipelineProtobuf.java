package de.uniluebeck.itm.wisebed.cmdlineclient.scripts;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wisebed.cmdlineclient.ReservationKey;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;

public class SetChannelPipelineProtobuf {

	private static final Logger log = LoggerFactory.getLogger(SetChannelPipelineProtobuf.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		final String sessionManagementEndpointUrl = "http://wisebed-staging.itm.uni-luebeck.de:8888/sessions";
		final String urnPrefix = "urn:wisebed:uzl-staging:";
		final String secretReservationKey = "DE59F780301790570EA1FA21B4CE8B47";

		final WisebedClient client = new WisebedClient(sessionManagementEndpointUrl);

		final ReservationKey reservationKey = new ReservationKey(secretReservationKey, urnPrefix);
		final WSNAsyncWrapper wsn = client.connectToExperiment(reservationKey).get();

		final List<String> nodeUrns = WiseMLHelper.getNodeUrns(wsn.getNetwork().get());

		final ChannelHandlerConfiguration dleStxEtxDecoder = new ChannelHandlerConfiguration();
		dleStxEtxDecoder.setName("dle-stx-etx-decoder");
		final ChannelHandlerConfiguration dleStxEtxEncoder = new ChannelHandlerConfiguration();
		dleStxEtxEncoder.setName("dle-stx-etx-encoder");

		final JobResult jobResult = wsn.setChannelPipeline(
				nodeUrns,
				newArrayList(dleStxEtxDecoder, dleStxEtxEncoder),
				10,
				TimeUnit.SECONDS
		).get();

		log.info("{}", jobResult);

	}
}
