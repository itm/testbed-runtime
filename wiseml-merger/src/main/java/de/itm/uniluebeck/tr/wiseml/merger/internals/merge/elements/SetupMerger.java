package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Map;

import org.joda.time.DateTime;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.config.TimestampStyle;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLSequence;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupMerger extends WiseMLElementMerger {

	private static final Logger log = LoggerFactory.getLogger(SetupMerger.class);
	
	private static final int PROPERTIES = 1;
	private static final int DEFAULTS = 2;
	private static final int NODES = 3;
	private static final int LINKS = 4;
	
	private int state;	

	public SetupMerger(
			final WiseMLTreeMerger parent, 
			final WiseMLTreeReader[] inputs,
			final MergerConfiguration configuration, 
			final MergerResources resources) {
		super(parent, inputs, configuration, resources, WiseMLTag.setup);
		this.state = PROPERTIES;
	}

	@Override
	public void fillQueue() {
		if (state == PROPERTIES && mergeProperties()) {
			return;
		}
		if (state == DEFAULTS && mergeDefaults()) {
			return;
		}
		if (state == NODES && mergeNodes()) {
			return;
		}
		if (state == LINKS && mergeLinks()) {
			return;
		}
		finished = true;
	}
	
	
	

	private boolean mergeNodes() {
		//updateInputs();
		//printInputState("before <node>*");
		WiseMLTreeReader[] nodeListInputs = 
			findSequenceReaders(WiseMLSequence.SetupNode);
		
		
		if (nodeListInputs != null) {
			queue.add(new NodeListMerger(this, nodeListInputs, null, null));
		}
		
		//printInputState("after <node>*");
		
		state++;
		
		return !queue.isEmpty();
	}
	
	private boolean mergeLinks() {
		//updateInputs();
		//printInputState("before <link>*");
		WiseMLTreeReader[] linkListInputs = 
			findSequenceReaders(WiseMLSequence.SetupLink);
		
		
		if (linkListInputs != null) {
			queue.add(new LinkListMerger(this, linkListInputs, null, null));
		}
		
		//printInputState("after <link>*");
		
		state++;
		
		return !queue.isEmpty();
	}

	private boolean mergeDefaults() {
		final NodeProperties[] inputDefaultNodes = 
			new NodeProperties[inputCount()];
		final LinkProperties[] inputDefaultLinks = 
			new LinkProperties[inputCount()];
		
		//printInputState("before <defaults>");
		
		// loop through inputs (<defaults> is optional)
		for (int i = 0; i < inputCount(); i++) {
			WiseMLTreeReader defaultsReader = getSubInputReader(i);
			if (defaultsReader.isMappedToTag() 
					&& defaultsReader.getTag().equals(WiseMLTag.defaults)) {
				readDefaults(inputDefaultNodes, inputDefaultLinks, 
						i, defaultsReader);
				
			} else {
				holdInput(i);
			}
		}
		
		NodeProperties outputDefaultNode = null;
		LinkProperties outputDefaultLink = null;

		if (!allNull(inputDefaultNodes)) {
			if (allEqual(inputDefaultNodes)) {
				outputDefaultNode = inputDefaultNodes[0];
			} else {
				outputDefaultNode = null; // TODO: find good defaults
			}
		}
		if (!allNull(inputDefaultLinks)) {
			if (allEqual(inputDefaultLinks)) {
				outputDefaultLink = inputDefaultLinks[0];
			} else {
				outputDefaultLink = null; // TODO: find good defaults
			}
		}
		
		// save defaults
		//resources.setDefaultNodes(inputDefaultNodes, outputDefaultNode);
		//resources.setDefaultLinks(inputDefaultLinks, outputDefaultLink);
		
		// create transformers
		resources.setNodePropertiesTransformer(
				new NodePropertiesTransformer(
						inputDefaultNodes, 
						outputDefaultNode));
		resources.setLinkPropertiesTransformer(
				new LinkPropertiesTransformer(
						inputDefaultLinks, 
						outputDefaultLink));
		resources.setNodeItemTransformer(
				new NodeItemTransformer(
						inputDefaultNodes, 
						outputDefaultNode));
		
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
		Coordinate[] origins = new Coordinate[inputCount()];
		TimeInfo[] timeInfos = new TimeInfo[inputCount()];
		Interpolation[] interpolations = new Interpolation[inputCount()];
		String[] coordinateTypes = new String[inputCount()];
		String[] descriptions = new String[inputCount()];
		
		// retrieve properties from inputs
		for (int i = 0; i < inputCount(); i++) {
			Map<WiseMLTag, Object> map = getStructures(i);
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

//        System.out.println("JUHU"+configuration.getDescriptionResolution());
//        System.out.println("conflict = "+conflict);
//        System.out.println("is forced = "+configuration.isForceResolveDescription());
		if (configuration.isForceResolveDescription() || conflict) {
			if (conflict) {
				switch (configuration.getDescriptionResolution()) {
				case ResolveSilently: break;
				case ResolveWithWarning: 
					log.warn("descriptions not equal, resolving");
					break;
				case ThrowException: 
					exception("descriptions not equal", null);
					break;
				}
			}
			
			String description = "";
			
			switch (configuration.getDescriptionOutput()) {
			case UseCustomPlusInputDescriptions:
				description = configuration.getCustomDescription() + "\n";
			case ListInputDescriptions: {
				StringBuilder sb = new StringBuilder();
				sb.append(description);
				for (int i = 0; i < descriptions.length; i++) {
					if (descriptions[i] != null) {
						sb.append("\t"+(i+1)+": "+descriptions[i]);
						sb.append("\n\n");
					}
				}
				description = sb.toString();
			}
			}
			
//			System.out.println("description = "+description);
			
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
				log.warn("coordinate types not equal, resolving (not recommended!)");
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
				log.warn("interpolations not equal, resolving");
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
		
		resources.setInputTimeInfos(timeInfos);
		
		if (allEqual(timeInfos)) {
			timeInfo = timeInfos[0];
		} else {
			timeInfo = firstNonNull(timeInfos);
			
			// check for duration/end conflicts
			boolean durationConflict = false;
			for (int i = 0; i < timeInfos.length; i++) {
				if (timeInfos[i] != null) {
					if (timeInfo.isEndDefined() 
							!= timeInfos[i].isEndDefined()) {
						durationConflict = true;
						break;
					}
				}
			}
			if (durationConflict) {
				switch (configuration.getTimeInfoDurationResolution()) {
				case ResolveSilently: break;
				case ResolveWithWarning:
					log.warn("timeinfo definitions not equal, resolving");
					break;
				case ThrowException:
					exception("timeinfo definition not equal", null);
					break;
				}
			}
			
			// check for unit conflicts
			boolean unitConflict = false;
			for (int i = 0; i < timeInfos.length; i++) {
				if (timeInfos[i] != null) {
					if (!timeInfo.getUnit().equals(timeInfos[i].getUnit())) {
						unitConflict = true;
						break;
					}
				}
			}
			if (unitConflict) {
				switch (configuration.getTimeInfoUnitResolution()) {
				case ResolveSilently: break;
				case ResolveWithWarning:
					log.warn("timeinfo units not equal, resolving");
					break;
				case ThrowException:
					exception("timeinfo units not equal", null);
					break;
				}
			}
			
			boolean useEnd = timeInfo.isEndDefined();
			Unit unit = timeInfo.getUnit();
			
			switch (configuration.getTimeInfoDurationOutput()) {
			case FirstFileSelect: break;
			case OverrideUseDuration:
				useEnd = false;
				break;
			case OverrideUseEnd:
				useEnd = true;
				break;
			}
			
			switch (configuration.getTimeInfoUnitOutput()) {
			case Best:
				unit = Unit.milliseconds; // TODO: check if seconds can be used as well
				break;
			case FirstFileSelect:
				break;
			case Custom:
				unit = configuration.getCustomTimeInfoUnit();
				break;
			}
			
			// combine timeinfos
			for (int i = 0; i < timeInfos.length; i++) {
				if (timeInfos[i] != null) {
					if (!timeInfo.equals(timeInfos[i])) {
						DateTime start = timeInfos[i].getStart();
						DateTime end = timeInfos[i].getEnd();
						
						if (start.isBefore(timeInfo.getStart())) {
							timeInfo.setStart(start);
						}
						if (end.isAfter(timeInfo.getEnd())) {
							timeInfo.setEnd(end);
						}
					}
				}
			}
			
			timeInfo.setEndDefined(useEnd);
			timeInfo.setUnit(unit);
			
			// timestamp style
			resources.setTimestampOffsetDefined(
					configuration.getCustomTimestampStyle()
					== TimestampStyle.Offsets);
		}
		
		if (timeInfo != null) {
			resources.setOutputTimeInfo(timeInfo);
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
					log.warn("Resolving origin conflict using this strategy: " +
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
