package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.config.DescriptionOutput;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLElementMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.WiseMLTreeMerger;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserCallback;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.ParserHelper;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.CoordinateTypeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.DefaultsReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.DescriptionReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.InterpolationReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.OriginReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements.TimeInfoReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;

public class SetupMerger extends WiseMLElementMerger {
	
	protected static class SetupProperties {
		private final Coordinate origin;
		private final TimeInfo timeInfo;
		private final Interpolation interpolation;
		private final String coordinateType;
		private final String description;
		
		public SetupProperties(final Map<WiseMLTag,Object> structures) {
			this.origin = (Coordinate)structures.get(WiseMLTag.origin);
			this.timeInfo = (TimeInfo)structures.get(WiseMLTag.timeinfo);
			this.interpolation = (Interpolation)structures.get(WiseMLTag.interpolation);
			this.coordinateType = (String)structures.get(WiseMLTag.coordinateType);
			this.description = (String)structures.get(WiseMLTag.description);
		}

		public SetupProperties(
				final Coordinate origin, 
				final TimeInfo timeInfo,
				final Interpolation interpolation, 
				final String coordinateType,
				final String description) {
			this.origin = origin;
			this.timeInfo = timeInfo;
			this.interpolation = interpolation;
			this.coordinateType = coordinateType;
			this.description = description;
		}

		public Coordinate getOrigin() {
			return this.origin;
		}

		public String getDescription() {
			return this.description;
		}

		public String getCoordinateType() {
			return this.coordinateType;
		}

		public TimeInfo getTimeInfo() {
			return this.timeInfo;
		}

		public Interpolation getInterpolation() {
			return this.interpolation;
		}
		
	}
	
	private static final int INIT = 0;
	private static final int PROPERTIES = 1;
	private static final int DEFAULTS = 2;
	private static final int NODES = 3;
	//private static final int LINKS = 4;
	
	private int state;

	public SetupMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration, 
			final MergerResources resources) {
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
	
	private WiseMLTreeReader[] getListReaders(WiseMLTag tag) {
		boolean found = false;
		WiseMLTreeReader[] nodeListInputs = new WiseMLTreeReader[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			WiseMLTreeReader nextReader = inputs[i].getSubElementReader();
			if (nextReader.isList()) {
				if (nextReader.getSubElementReader() == null) {
					nextReader.nextSubElementReader();
				}
				if (nextReader.getSubElementReader().getTag().equals(tag)) {
					found = true;
					nodeListInputs[i] = nextReader;
				}
			}
		}
		return found?nodeListInputs:null;
	}

	private boolean mergeNodes() {
		WiseMLTreeReader[] nodeListInputs = getListReaders(WiseMLTag.node);
		
		if (nodeListInputs != null) {
			queue.add(new NodeListMerger(this, nodeListInputs, null, null));
		}
		
		state++;
		
		return !queue.isEmpty();
	}
	
	private boolean mergeLinks() {
		WiseMLTreeReader[] linkListInputs = getListReaders(WiseMLTag.link);
		
		if (linkListInputs != null) {
			queue.add(new LinkListMerger(this, linkListInputs, null, null));
		}
		
		state++;
		
		return !queue.isEmpty();
	}

	private boolean mergeDefaults() {
		final NodeProperties[] inputDefaultNodes = new NodeProperties[inputs.length];
		final LinkProperties[] inputDefaultLinks = new LinkProperties[inputs.length];
		
		// loop through inputs (<defaults> is optional)
		for (int i = 0; i < inputs.length; i++) {
			WiseMLTreeReader defaultsReader = inputs[i].getSubElementReader();
			if (defaultsReader.isMappedToTag() && defaultsReader.getTag().equals(WiseMLTag.defaults)) {
				readDefaults(inputDefaultNodes, inputDefaultLinks, i, defaultsReader);
				inputs[i].nextSubElementReader();
			}
		}
		
		NodeProperties outputDefaultNode = null;
		LinkProperties outputDefaultLink = null;

		if (!allNull(inputDefaultNodes)) {
			if (allEqual(inputDefaultNodes)) {
				outputDefaultNode = inputDefaultNodes[0];
			} else {
				outputDefaultNode = null; // TODO: find fine-grained, mutual defaults
			}
		}
		if (!allNull(inputDefaultLinks)) {
			if (allEqual(inputDefaultLinks)) {
				outputDefaultLink = inputDefaultLinks[0];
			} else {
				outputDefaultLink = null; // TODO: find fine-grained, mutual defaults
			}
		}
		
		// save defaults
		resources.setDefaultNodes(inputDefaultNodes, outputDefaultNode);
		resources.setDefaultLinks(inputDefaultLinks, outputDefaultLink);
		
		// queue defaults
		if (outputDefaultNode != null && outputDefaultLink != null) {
			queue.add(new DefaultsReader(this, outputDefaultNode, outputDefaultLink));
		}
		
		// next state
		state++;
		
		return !queue.isEmpty();
	}
	
	private void readDefaults(
			final NodeProperties[] inputDefaultNodes,
			final LinkProperties[] inputDefaultLinks,
			final int index,
			final WiseMLTreeReader defaultsReader) {
		ParserHelper.parseStructures(defaultsReader, new ParserCallback(){
			@Override
			public void nextStructure(WiseMLTag tag, Object structure) {
				switch (tag) {
				case node:
					inputDefaultNodes[index] = (NodeProperties)structure;
					break;
				case link:
					inputDefaultLinks[index] = (LinkProperties)structure;
					break;
				}
			}
		});
	}
	

	private boolean mergeProperties() {
		// retrieve properties from inputs
		SetupProperties[] inputProperties = new SetupProperties[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			inputProperties[i] = new SetupProperties(ParserHelper.getStructures(inputs[i]));
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
		
		// push properties into queue
		queueProperties(outputProperties);
		
		// next state
		state++;
		
		return !queue.isEmpty();
	}
	
	private void queueProperties(SetupProperties properties) {
		if (properties.getOrigin() != null) {
			queue.add(new OriginReader(this, properties.getOrigin()));
		}
		if (properties.getTimeInfo() != null) {
			queue.add(new TimeInfoReader(this, properties.getTimeInfo()));
		}
		if (properties.getInterpolation() != null) {
			queue.add(new InterpolationReader(this, properties.getInterpolation()));
		}
		if (properties.getCoordinateType() != null) {
			queue.add(new CoordinateTypeReader(this, properties.getCoordinateType()));
		}
		if (properties.getDescription() != null) {
			queue.add(new DescriptionReader(this, properties.getDescription()));
		}
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
			if (!inputProperties[i].getDescription()
					.equals(inputProperties[0].getDescription())) {
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
			result = configuration.getCustomDescriptionText() + "\n\n";
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
		
		
		return result;
	}
	
	private static <T> boolean allNull(final T[] array) {
		for (T object : array) {
			if (object != null) {
				return false;
			}
		}
		return true;
	}
	
	private static <T> boolean allEqual(final T[] array) {
		for (int i = 1; i < array.length; i++) {
			T a = array[0];
			T b = array[i];
			if (a == null && b == null) {
				continue;
			}
			if (a == null || b == null) {
				return false;
			}
			if (!a.equals(b)) {
				return false;
			}
		}
		return true;
	}

}
