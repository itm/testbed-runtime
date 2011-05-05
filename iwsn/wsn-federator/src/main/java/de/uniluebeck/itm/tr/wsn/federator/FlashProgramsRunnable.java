package de.uniluebeck.itm.tr.wsn.federator;

import eu.wisebed.testbed.api.wsn.v23.Program;
import eu.wisebed.testbed.api.wsn.v23.WSN;

import java.util.List;

class FlashProgramsRunnable extends AbstractRequestRunnable {

	private List<String> nodeIds;

	private List<Integer> programIndices;

	private List<Program> programs;

	FlashProgramsRunnable(FederatorController federatorController, WSN wsnEndpoint,
								  String federatorRequestId,
								  List<String> nodeIds, List<Integer> programIndices, List<Program> programs) {

		super(federatorController, wsnEndpoint, federatorRequestId);

		this.nodeIds = nodeIds;
		this.programIndices = programIndices;
		this.programs = programs;
	}

	@Override
	public void run() {
		// instance wsnEndpoint is potentially not thread-safe!!!
		synchronized (wsnEndpoint) {
			done(wsnEndpoint.flashPrograms(nodeIds, programIndices, programs));
		}
	}

}