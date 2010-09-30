package de.itm.uniluebeck.tr.wiseml.merger.internals.parse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.BooleanParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.CapabilityParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.CoordinateTypeParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.DataTypeParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.DescriptionParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.InterpolationParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.LinkPropertiesParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.NodePropertiesParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.OriginParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.PositionParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.RSSIParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.StringParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.TimeInfoParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.UnitParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ParserManager {
	
	private static ParserManager instance;
	
	private static class ParserKey {
		private final WiseMLTag context;
		private final WiseMLTag tag;
		
		public ParserKey(WiseMLTag context, WiseMLTag tag) {
			this.context = context;
			this.tag = tag;
		}
/*
		public WiseMLTag getContext() {
			return context;
		}

		public WiseMLTag getTag() {
			return tag;
		}
*/		
		@Override
		public int hashCode() {
			return 31*context.hashCode() + tag.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ParserKey)) {
				return  false;
			}
			ParserKey other = (ParserKey)obj;
			return this.context.equals(other.context) && this.tag.equals(other.tag);
		}
	}
	
	private ParserManager() {
		// setup.properties
		register(WiseMLTag.setup, WiseMLTag.origin, OriginParser.class);
		register(WiseMLTag.setup, WiseMLTag.position, PositionParser.class);
		register(WiseMLTag.setup, WiseMLTag.timeinfo, TimeInfoParser.class);
		register(WiseMLTag.setup, WiseMLTag.interpolation, InterpolationParser.class);
		register(WiseMLTag.setup, WiseMLTag.coordinateType, CoordinateTypeParser.class);
		register(WiseMLTag.setup, WiseMLTag.description, DescriptionParser.class);
		
		// <defaults>
		register(WiseMLTag.defaults, WiseMLTag.node, NodePropertiesParser.class);
		register(WiseMLTag.defaults, WiseMLTag.link, LinkPropertiesParser.class);
		
		// node.properties
		register(WiseMLTag.node, WiseMLTag.position, PositionParser.class);
		register(WiseMLTag.node, WiseMLTag.gateway, BooleanParser.class);
		register(WiseMLTag.node, WiseMLTag.programDetails, StringParser.class);
		register(WiseMLTag.node, WiseMLTag.nodeType, StringParser.class);
		register(WiseMLTag.node, WiseMLTag.description, StringParser.class);
		register(WiseMLTag.node, WiseMLTag.capability, CapabilityParser.class);
		
		// link.properties
		register(WiseMLTag.link, WiseMLTag.encrypted, BooleanParser.class);
		register(WiseMLTag.link, WiseMLTag.virtual, BooleanParser.class);
		register(WiseMLTag.link, WiseMLTag.rssi, RSSIParser.class);
		register(WiseMLTag.link, WiseMLTag.capability, CapabilityParser.class);
		
		// <capability>
		register(WiseMLTag.capability, WiseMLTag.name, StringParser.class);
		register(WiseMLTag.capability, WiseMLTag.dataType, DataTypeParser.class);
		register(WiseMLTag.capability, WiseMLTag.unit, UnitParser.class);
		register(WiseMLTag.capability, WiseMLTag.capabilityDefaultValue, StringParser.class);
		
		// TODO
	}
	
	private Map<ParserKey,Constructor<? extends WiseMLElementParser<?>>> parserMap = 
		new HashMap<ParserKey,Constructor<? extends WiseMLElementParser<?>>>();
	
	public void register(
			final WiseMLTag context,
			final WiseMLTag tag, 
			final Class<? extends WiseMLElementParser<?>> clazz) {
		try {
			Constructor<? extends WiseMLElementParser<?>> constructor = 
				clazz.getConstructor(WiseMLTreeReader.class);
			parserMap.put(new ParserKey(context, tag), constructor);
		} catch (SecurityException e) {
			throw new RuntimeException("while registering "+tag, e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("while registering "+tag, e);
		}
	}
	
	public WiseMLElementParser<?> createParser(final WiseMLTreeReader reader) {
		return createParser(reader.getTag(), reader);
	}
	
	public WiseMLElementParser<?> createParser(
			final WiseMLTag tag, 
			final WiseMLTreeReader reader) {
		return createParser(reader.getParentReader().getTag(), tag, reader);
	}
	
	public WiseMLElementParser<?> createParser(
			final WiseMLTag context,
			final WiseMLTag tag, 
			final WiseMLTreeReader reader) {
		Constructor<? extends WiseMLElementParser<?>> constructor 
				= parserMap.get(new ParserKey(context, tag));
		
		if (constructor == null) {
			return null;
		}
		
		try {
			return constructor.newInstance(reader);
		} catch (IllegalArgumentException e) {
			reader.exception("could not create parser for tag <"+tag+">", e);
		} catch (InstantiationException e) {
			reader.exception("could not create parser for tag <"+tag+">", e);
		} catch (IllegalAccessException e) {
			reader.exception("could not create parser for tag <"+tag+">", e);
		} catch (InvocationTargetException e) {
			reader.exception("could not create parser for tag <"+tag+">", e);
		}
		
		return null;
	}
	
	public static ParserManager sharedInstance() {
		if (instance == null) {
			instance = new ParserManager();
		}
		return instance;
	}

}
