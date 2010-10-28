package de.itm.uniluebeck.tr.wiseml.merger.structures;

import java.util.ArrayList;
import java.util.List;

public class LinkTraceItem {
	
	private String source;
	private String target;
	
	private String rssi;
	private List<String> dataItems;
	
	public LinkTraceItem(String source, String target) {
		this.source = source;
		this.target = target;
		this.dataItems = new ArrayList<String>();
	}

	public String getRssi() {
		return rssi;
	}

	public void setRssi(String rssi) {
		this.rssi = rssi;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}
	
	public void addDataItem(String key, String data) {
		this.dataItems.add(key);
		this.dataItems.add(data);
	}
	
	public int dataItemCount() {
		return this.dataItems.size() / 2;
	}
	
	public String getKey(int item) {
		return this.dataItems.get(item*2);
	}
	
	public String getData(int item) {
		return this.dataItems.get(item*2+1);
	}

}
