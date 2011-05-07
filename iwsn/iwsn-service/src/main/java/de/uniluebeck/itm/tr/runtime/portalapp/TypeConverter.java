package de.uniluebeck.itm.tr.runtime.portalapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;

import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;

/**
 * Helper class for this package that converts types from WSNApp representation to Web service representation and back.
 */
class TypeConverter {

	static RequestStatus convert(WSNAppMessages.RequestStatus requestStatus, String requestId) {
		RequestStatus retRequestStatus = new RequestStatus();
		retRequestStatus.setRequestId(requestId);
		WSNAppMessages.RequestStatus.Status status = requestStatus.getStatus();
		Status retStatus = new Status();
		retStatus.setMsg(status.getMsg());
		retStatus.setNodeId(status.getNodeId());
		retStatus.setValue(status.getValue());
		retRequestStatus.getStatus().add(retStatus);
		return retRequestStatus;
	}

	static WSNAppMessages.Message convert(Message message) {
		return WSNAppMessages.Message.newBuilder()
				.setBinaryData(ByteString.copyFrom(message.getBinaryData()))
				.setSourceNodeId(message.getSourceNodeId())
				.setTimestamp(message.getTimestamp().toString())
				.build();
	}

	static Map<String, WSNAppMessages.Program> convert(List<String> nodeIds, List<Integer> programIndices,
														List<Program> programs) {

		Map<String, WSNAppMessages.Program> programsMap = new HashMap<String, WSNAppMessages.Program>();

		List<WSNAppMessages.Program> convertedPrograms = convert(programs);

		for (int i = 0; i < nodeIds.size(); i++) {
			programsMap.put(nodeIds.get(i), convertedPrograms.get(programIndices.get(i)));
		}

		return programsMap;
	}

	static List<WSNAppMessages.Program> convert(List<Program> programs) {
		List<WSNAppMessages.Program> list = new ArrayList<WSNAppMessages.Program>(programs.size());
		for (Program program : programs) {
			list.add(convert(program));
		}
		return list;
	}

	static WSNAppMessages.Program convert(Program program) {
		return WSNAppMessages.Program.newBuilder().setMetaData(convert(program.getMetaData())).setProgram(
				ByteString.copyFrom(program.getProgram())
		).build();
	}

	static WSNAppMessages.Program.ProgramMetaData convert(ProgramMetaData metaData) {
		if (metaData == null) {
			metaData = new ProgramMetaData();
			metaData.setName("");
			metaData.setOther("");
			metaData.setPlatform("");
			metaData.setVersion("");
		}
		return WSNAppMessages.Program.ProgramMetaData.newBuilder().setName(metaData.getName()).setOther(
				metaData.getOther()
		).setPlatform(metaData.getPlatform()).setVersion(metaData.getVersion()).build();
	}

}
