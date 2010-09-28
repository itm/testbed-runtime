package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

public class ScenarioDefinition implements Comparable<ScenarioDefinition> {
	
	private String id;

	public ScenarioDefinition(String id) {
		this.id = id;
	}

	@Override
	public int compareTo(ScenarioDefinition o) {
		return id.compareTo(o.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ScenarioDefinition)) {
			return false;
		}
		ScenarioDefinition other = (ScenarioDefinition)obj;
		return id.equals(other.id);
	}

}
