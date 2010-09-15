package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLElementMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.CoordinateReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;

public class SetupMerger extends WiseMLElementMerger {
	
	protected static class SetupProperties {
		Coordinate origin;
		TimeInfo timeInfo;
		Interpolation interpolation;
		String coordinateType;
		String description;
		
		public SetupProperties(final Map<WiseMLTag,Object> structures) {
			origin = (Coordinate)structures.get(WiseMLTag.origin);
			timeInfo = (TimeInfo)structures.get(WiseMLTag.timeinfo);
			interpolation = (Interpolation)structures.get(WiseMLTag.interpolation);
			coordinateType = (String)structures.get(WiseMLTag.coordinateType);
			description = (String)structures.get(WiseMLTag.description);
		}

		public SetupProperties(Coordinate origin, TimeInfo timeInfo,
				Interpolation interpolation, String coordinateType,
				String description) {
			this.origin = origin;
			this.timeInfo = timeInfo;
			this.interpolation = interpolation;
			this.coordinateType = coordinateType;
			this.description = description;
		}

		public Coordinate getOrigin() {
			return origin;
		}
		
		
	}
	
	private static final int INIT = 0;
	private static final int PROPERTIES = 1;
	private static final int DEFAULTS = 2;
	private static final int NODES = 3;
	private static final int LINKS = 4;
	
	private int state;

	public SetupMerger(
			WiseMLTreeMerger parent, 
			WiseMLTreeReader[] inputs,
			MergerConfiguration configuration, 
			MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLTag.setup);
		this.state = INIT;
	}

	@Override
	public void fillQueue() {
		if (state == INIT && mergeProperties()) {
			return;
		}
		if (state == PROPERTIES && mergeDefaults()) {
			return;
		}
		if (state == DEFAULTS && mergeNodes()) {
			return;
		}
		if (state == NODES && mergeLinks()) {
			return;
		}
		finished = true;
	}
	
	private boolean mergeLinks() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean mergeNodes() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean mergeDefaults() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean mergeProperties() {
		// retrieve properties from inputs
		SetupProperties[] inputProperties = new SetupProperties[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			inputProperties[i] = new SetupProperties(getStructures(inputs[i]));
		}
		
		// merge properties
		SetupProperties outputProperties = new SetupProperties(
				mergeOrigin(inputProperties),
				mergeTimeInfo(inputProperties),
				mergeInterpolation(inputProperties),
				mergeCoordinateType(inputProperties),
				mergeDescription(inputProperties));
		
		// save origins
		resources.setOrigins(getOrigins(inputProperties), outputProperties.getOrigin());
		
		// next state
		state++;
		
		return !queue.isEmpty();
	}
	
	private Coordinate[] getOrigins(SetupProperties[] propertiesArray) {
		return null; // TODO
	}
	
	private String mergeDescription(SetupProperties[] inputProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	private String mergeCoordinateType(SetupProperties[] inputProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	private Interpolation mergeInterpolation(SetupProperties[] inputProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	private TimeInfo mergeTimeInfo(SetupProperties[] inputProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	private Coordinate mergeOrigin(SetupProperties[] inputProperties) {
		Coordinate result = null; // TODO
		
		
		queue.add(new CoordinateReader(this, WiseMLTag.origin, result));
		return result;
	}
	
	

}
