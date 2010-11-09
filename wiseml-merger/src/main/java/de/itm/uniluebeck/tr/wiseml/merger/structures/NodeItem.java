package de.itm.uniluebeck.tr.wiseml.merger.structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains data as defined by node.scenario and node.traceitem (in the
 * grammar). Both consist of a <node> element with an optional <position>
 * element and a sequence of <data> elements.
 * 
 * @author jacob kuypers
 *
 */
public class NodeItem {
	
	private String id;
	private Coordinate position;
	private List<String> dataItems; // key, data pairs

	public NodeItem(String id) {
		this.id = id;
		this.dataItems = new ArrayList<String>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Coordinate getPosition() {
		return position;
	}

	public void setPosition(Coordinate position) {
		this.position = position;
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
