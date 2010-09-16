package de.itm.uniluebeck.tr.wiseml.merger.internals.parse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.OriginParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements.PositionParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class ParserManager {
	
	private static ParserManager instance;
	
	private ParserManager() {
		register(WiseMLTag.origin, OriginParser.class);
		register(WiseMLTag.position, PositionParser.class);
		// TODO
	}
	
	private Map<WiseMLTag,Constructor<? extends WiseMLElementParser<?>>> parserMap = 
		new HashMap<WiseMLTag,Constructor<? extends WiseMLElementParser<?>>>();
	
	public void register(final WiseMLTag tag, final Class<? extends WiseMLElementParser<?>> clazz) {
		try {
			Constructor<? extends WiseMLElementParser<?>> constructor = 
				clazz.getConstructor(WiseMLTreeReader.class);
			parserMap.put(tag, constructor);
		} catch (SecurityException e) {
			throw new RuntimeException("while registering "+tag, e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("while registering "+tag, e);
		}
	}
	
	public WiseMLElementParser<?> createParser(final WiseMLTag tag, final WiseMLTreeReader reader) {
		Constructor<? extends WiseMLElementParser<?>> constructor = parserMap.get(tag);
		
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
