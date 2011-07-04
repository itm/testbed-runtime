package de.uniluebeck.itm.wisebed.cmdlineclient.scripts;

import com.google.common.collect.Lists;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wisebed.cmdlineclient.AuthenticationCredentials;
import de.uniluebeck.itm.wisebed.cmdlineclient.AuthenticationKey;
import de.uniluebeck.itm.wisebed.cmdlineclient.ReservationKey;
import de.uniluebeck.itm.wisebed.cmdlineclient.WisebedClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;

public class SetChannelPipeline {

	private static final Logger log = LoggerFactory.getLogger(SetChannelPipeline.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		final String sessionManagementEndpointUrl = "http://wisebed-staging.itm.uni-luebeck.de:8888/sessions";
		final String urnPrefix = "urn:wisebed:uzl-staging:";
		final String secretReservationKey = "7870521F5A9FBAB43157E426A3E01C7B";

		final WisebedClient client = new WisebedClient(sessionManagementEndpointUrl);

		final ReservationKey reservationKey = new ReservationKey(secretReservationKey, urnPrefix);
		final WSNAsyncWrapper wsn = client.connectToExperiment(reservationKey).get();


		final ChannelHandlerConfiguration dleStxEtxDecoder = new ChannelHandlerConfiguration();
		dleStxEtxDecoder.setName("dlestxetx-framing-decoder");
		final ChannelHandlerConfiguration dleStxEtxEncoder = new ChannelHandlerConfiguration();
		dleStxEtxEncoder.setName("dlestxetx-framing-encoder");

		/*final List<String> nodeUrns = WiseMLHelper.getNodeUrns(wsn.getNetwork().get());*/
		final List<String> nodeUrns = newArrayList();


		/*final ArrayList<ChannelHandlerConfiguration> channelHandlerConfigurations = newArrayList(
				dleStxEtxDecoder,
				dleStxEtxEncoder
		);*/

		/*final ChannelHandlerConfiguration discardConfiguration = new ChannelHandlerConfiguration();
		discardConfiguration.setName("discard");
		final ArrayList<ChannelHandlerConfiguration> channelHandlerConfigurations = newArrayList(discardConfiguration);*/

		final List<ChannelHandlerConfiguration> channelHandlerConfigurations = Lists.newArrayList();

		final JobResult jobResult = wsn.setChannelPipeline(
				nodeUrns,
				channelHandlerConfigurations,
				10,
				TimeUnit.SECONDS
		).get();

		log.info("{}", jobResult);

	}
}
