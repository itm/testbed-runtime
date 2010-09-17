package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.config.DescriptionOutput;
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

		public String getDescription() {
			return description;
		}

		public String getCoordinateType() {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
	
	private static final int INIT = 0;
	private static final int PROPERTIES = 1;
	private static final int DEFAULTS = 2;
	private static final int NODES = 3;
	//private static final int LINKS = 4;
	
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
		// check if descriptions are equal
		boolean conflict = false;
		for (int i = 1; i < inputProperties.length && !conflict; i++) {
			if (inputProperties[i].getDescription() == null) {
				conflict = true;
			}
		}
		for (int i = 1; i < inputProperties.length && !conflict; i++) {
			if (!inputProperties[i].getDescription().equals(inputProperties[0].getDescription())) {
				conflict = true;
			}
		}
		if (!conflict) {
			return inputProperties[0].getDescription();
		}
		
		switch (configuration.getDescriptionConflict()) {
		case ResolveSilently: break;
		case ResolveWithWarning: 
			warn("descriptions not equal");
			break;
		case ThrowException: 
			exception("descriptions not equal", null);
			break;
		}
		
		if (configuration.getDescriptionOutput() 
				== DescriptionOutput.UseCustomDescription) {
			return configuration.getCustomDescriptionText();
		}
		
		String result = "";
		
		switch (configuration.getDescriptionOutput()) {
		case UseCustomPlusInputDescriptions:
			result = configuration.getCustomDescriptionText();
		case ListInputDescriptions: {
			StringBuilder sb = new StringBuilder();
			sb.append(result);
			for (int i = 0; i < inputProperties.length; i++) {
				if (inputProperties[i].getDescription() != null) {
					sb.append(inputProperties[i].getDescription());
					sb.append("\n\n");
				}
			}
			result = sb.toString();
		}
		}
		
		return result;
	}

	private String mergeCoordinateType(SetupProperties[] inputProperties) {
		String[] types = new String[inputProperties.length];
		for (int i = 0; i < types.length; i++) {
			types[i] = inputProperties[i].getCoordinateType();
		}
		
		if (allNull(types)) {
			return null;
		}
		
		for (int i = 1; i < types.length; i++) {
			if (!types[0].equals(types[i])) {
				exception("unresolvable conflict: differing coordinate types", null);
			}
		}
		
		return types[0];
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
	
	private static <T> boolean allNull(String[] strings) {
		for (String s : strings) {
			if (s != null) {
				return false;
			}
		}
		return true;
	}

}
