package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.lang.reflect.Field;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.config.DescriptionOutput;
import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.MergerResources;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.VecMath;
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
	
	// TODO: remove this class, replace with arrays
	/*
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
	*/
	
	private static final int INIT = 0;
	private static final int PROPERTIES = 1;
	private static final int DEFAULTS = 2;
	private static final int NODES = 3;
	//private static final int LINKS = 4;
	
	private int state;
	private boolean[] skip; // true if input i should be incremented
	

	public SetupMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration, 
			final MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLTag.setup);
		this.state = INIT;
		this.skip = new boolean[inputs.length];
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
			if (nextReader.isFinished()) {
				inputs[i].nextSubElementReader();
				nextReader = inputs[i].getSubElementReader();
			}
			if (nextReader.isList()) {
				if (nextReader.getSubElementReader() == null) {
					if (!nextReader.nextSubElementReader()) {
						return null;
					}
				}
				if (nextReader.getSubElementReader().getTag().equals(tag)) {
					found = true;
					nodeListInputs[i] = nextReader;
					skip[i] = true;
				}
			}
		}
		return found?nodeListInputs:null;
	}
	
	private void updateInputs() {
		for (int i = 0; i < skip.length; i++) {
			if (skip[i]) {
				inputs[i].nextSubElementReader();
				skip[i] = false;
			}
		}
	}

	private boolean mergeNodes() {
		updateInputs();
		//printInputState("before <node>*");
		WiseMLTreeReader[] nodeListInputs = getListReaders(WiseMLTag.node);
		
		
		if (nodeListInputs != null) {
			queue.add(new NodeListMerger(this, nodeListInputs, null, null));
		}
		
		//printInputState("after <node>*");
		
		state++;
		
		return !queue.isEmpty();
	}
	
	private boolean mergeLinks() {
		updateInputs();
		//printInputState("before <link>*");
		WiseMLTreeReader[] linkListInputs = getListReaders(WiseMLTag.link);
		
		
		if (linkListInputs != null) {
			queue.add(new LinkListMerger(this, linkListInputs, null, null));
		}
		
		//printInputState("after <link>*");
		
		state++;
		
		return !queue.isEmpty();
	}

	private boolean mergeDefaults() {
		final NodeProperties[] inputDefaultNodes = 
			new NodeProperties[inputs.length];
		final LinkProperties[] inputDefaultLinks = 
			new LinkProperties[inputs.length];
		
		//printInputState("before <defaults>");
		
		// loop through inputs (<defaults> is optional)
		for (int i = 0; i < inputs.length; i++) {
			WiseMLTreeReader defaultsReader = inputs[i].getSubElementReader();
			if (defaultsReader.isMappedToTag() 
					&& defaultsReader.getTag().equals(WiseMLTag.defaults)) {
				readDefaults(inputDefaultNodes, inputDefaultLinks, 
						i, defaultsReader);
				
				skip[i] = true;
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
			queue.add(new DefaultsReader(
					this, outputDefaultNode, outputDefaultLink));
		}
		
		// next state
		state++;
		
		//printInputState("after <defaults>");
		
		return !queue.isEmpty();
	}
	/*
	private void printInputState(String msg) {
		if (msg != null) {
			System.err.print(msg);
			System.err.print(" ");
		}
		for (int i = 0; i < inputs.length; i++) {
			System.err.print(i);
			System.err.print(": ");
			System.err.print(inputs[i]);
			if (skip[i]) {
				System.err.print("#SKIP");
			}
			System.err.print(' ');
		}
		System.err.println();
	}
	*/
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
		
		// create arrays
		Coordinate[] origins = new Coordinate[inputs.length];
		TimeInfo[] timeInfos = new TimeInfo[inputs.length];
		Interpolation[] interpolations = new Interpolation[inputs.length];
		String[] coordinateTypes = new String[inputs.length];
		String[] descriptions = new String[inputs.length];
		
		// retrieve properties from inputs
		for (int i = 0; i < inputs.length; i++) {
			Map<WiseMLTag, Object> map = ParserHelper.getStructures(inputs[i]);
			origins[i] = (Coordinate)map.get(WiseMLTag.origin);
			timeInfos[i] = (TimeInfo)map.get(WiseMLTag.timeinfo);
			interpolations[i] = (Interpolation)map.get(WiseMLTag.interpolation);
			coordinateTypes[i] = (String)map.get(WiseMLTag.coordinateType);
			descriptions[i] = (String)map.get(WiseMLTag.description);
		}
		
		// merge properties
		mergeOrigin(origins);
		mergeTimeInfo(timeInfos);
		mergeInterpolation(interpolations);
		mergeCoordinateType(coordinateTypes);
		mergeDescription(descriptions);
		
		// next state
		state++;
		
		return !queue.isEmpty();
	}
	
	private void mergeDescription(String[] descriptions) {
		boolean conflict = !allEqual(descriptions);
		if (configuration.isForceResolveDescription() || conflict) {
			if (conflict) {
				switch (configuration.getDescriptionResolution()) {
				case ResolveSilently: break;
				case ResolveWithWarning: 
					warn("descriptions not equal, resolving");
					break;
				case ThrowException: 
					exception("descriptions not equal", null);
					break;
				}
			}
			
			String description = "";
			
			switch (configuration.getDescriptionOutput()) {
			case UseCustomPlusInputDescriptions:
				description = configuration.getCustomDescription() + "\n\n";
			case ListInputDescriptions: {
				StringBuilder sb = new StringBuilder();
				sb.append(description);
				for (int i = 0; i < descriptions.length; i++) {
					if (descriptions[i] != null) {
						sb.append(descriptions[i]);
						sb.append("\n\n");
					}
				}
				description = sb.toString();
			}
			}
			
			queue.add(new DescriptionReader(this, description));
		}
	}

	private void mergeCoordinateType(String[] coordinateTypes) {
		String coordinateType = null;
		
		if (allEqual(coordinateTypes)) {
			coordinateType = coordinateTypes[0];
		} else {
			switch (configuration.getCoordinateTypeResolution()) {
			case ResolveSilently: break;
			case ResolveWithWarning:
				warn("coordinate types not equal, resolving (not recommended!)");
				break;
			case ThrowException:
				exception("coordinate types not equal", null);
				break;
			}
			
			
			switch (configuration.getCoordinateTypeOutput()) {
			case UseCustom:
				coordinateType = configuration.getCustomCoordinateType();
				break;
			case UseFirstFile:
				coordinateType = coordinateTypes[0];
				break;
			}
		}
		
		if (coordinateType != null) {
			queue.add(new CoordinateTypeReader(this, coordinateType));
		}
	}

	private void mergeInterpolation(Interpolation[] interpolations) {
		Interpolation interpolation = null;
		
		if (allEqual(interpolations)) {
			interpolation = interpolations[0];
		} else {
			switch (configuration.getInterpolationResolution()) {
			case ResolveSilently: break;
			case ResolveWithWarning:
				warn("interpolations not equal, resolving");
				break;
			case ThrowException:
				exception("interpolations not equal", null);
				break;
			}
			
			switch (configuration.getInterpolationOutput()) {
			case UseBest: {
				int max = -1;
				for (int i = 0; i < interpolations.length; i++) {
					if (interpolations[i] != null && 
							(interpolation == null || interpolations[i].getQuality() > max)) {
						interpolation = interpolations[i];
						max = interpolation.getQuality();
					}
				}
			} break;
			case UseFirstFile:
				interpolation = interpolations[0];
				break;
			case UseCustom:
				interpolation = configuration.getCustomInterpolation();
				break;
			}
		}
		
		if (interpolation != null) {
			queue.add(new InterpolationReader(this, interpolation));
		}
	}

	private void mergeTimeInfo(TimeInfo[] timeInfos) {
		TimeInfo timeInfo = null;
		
		if (allEqual(timeInfos)) {
			timeInfo = timeInfos[0];
		} else {
			// TODO
		}
		
		if (timeInfo != null) {
			queue.add(new TimeInfoReader(this, timeInfo));
		}
	}

	private void mergeOrigin(Coordinate[] origins) {
		Coordinate origin = null;
		
		if (allEqual(origins)) {
			origin = origins[0];
		} else {
			// check if origins have the same dimensions
			Coordinate first = null;
			for (int i = 0; i < origins.length; i++) {
				if (first == null) {
					first = origins[i];
				} else if (origins[i] != null){
					if (!first.isComparableTo(origins[i])) {
						exception("origins not mergeable", null);
					}
				}
			}
			
			boolean doTransform = false;
						
			switch (configuration.getOriginOutput()) {
			case UseFirstOriginAndTransform:
				for (int i = 0; i < origins.length; i++) {
					if (origins[i] != null) {
						origin = origins[i];
						break;
					}
				}
				break;
			case UseCentralOriginAndTransform:
				try {
					origin = VecMath.computeCenter(origins);
				} catch (Exception e) {
					exception("could not merge origins", e);
				}
				break;
			}
			
			if (doTransform) {
				// TODO: create transform for each input
				
				switch (configuration.getOriginResolution()) {
				case ResolveSilently: break;
				case ResolveWithWarning:
					warn("Resolving origin conflict using this strategy: " + 
							configuration.getOriginOutput());
					break;
				case ThrowException:
					exception("unresolved origin conflict", null);
					break;
				}
			}
		}
		
		if (origin != null) {
			queue.add(new OriginReader(this, origin));
		}
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
	
	private static <T> T firstNonNull(final T[] array) {
		for (int i = 1; i < array.length; i++) {
			if (array[i] != null) {
				return array[i];
			}
		}
		return null;
	}

}
