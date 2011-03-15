package de.itm.uniluebeck.tr.wiseml.merger.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Indentation;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.enums.PrefixOutput;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

/**
 * Represents the configuration of a WiseML merging operation.
 * Main objective is to select strategies for the resolution of various
 * conflicts that may arise.
 * 
 * @author Jacob Kuypers
 *
 */
public class MergerConfiguration {
	
	/*
	 * Adding a new configuration property:
	 * 
	 * 1. Add a new member variable. The property can be of any primitive type and 
	 * of any class which contains a static valueOf(String) function. 
	 * 
	 * 2. Choose an annotation, either Property or Conflict.
	 * 		Conflict is used for merging conflict resolution strategies.
	 * 		For each conflict type (identified by a name) there may be
	 * 			- a resolution strategy (throw exception, warn etc)
	 * 			- an output method (how to merge or what to use instead...)
	 * 			- some custom value, used in certain output methods
	 * 
	 * 3. Set an appropriate default value (in the annotation, as a string)
	 * 
	 * 4. Generate getters and setters.
	 * 
	 * Everything else is done through reflection.
	 * 
	 */
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Conflict {
		enum Item { 
			Resolution("resolution"), 
			Output("output"), 
			CustomValue("custom"),
			;
			
			private String categoryName;
			
			Item(String categoryName) {
				this.categoryName = categoryName;
			}
			
			public String key() {
				return this.categoryName;
			}
		}
		
		String name();
		Conflict.Item item();
		String defaultValue();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Property {
		String category();
		String name();
		String defaultValue();
	}
	
	private static final String MERGER_PREFIX =
		"de.itm.uniluebeck.tr.wiseml.merger.";
	
	private static final String CONFLICT_CATEGORY = "conflict";
	private static final String XML_OUTPUT_CATEGORY = "xmloutput";
	
	@Conflict(name = "origin", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution originResolution;
	
	@Conflict(name = "origin", item = Conflict.Item.Output, defaultValue = "UseCentralOriginAndTransform")
	private OriginOutput originOutput;
	
	@Conflict(name = "timeInfoDuration", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution timeInfoDurationResolution;
	
	@Conflict(name = "timeInfoDuration", item = Conflict.Item.Output, defaultValue = "FirstFileSelect")
	private TimeInfoDurationOutput timeInfoDurationOutput;
	
	@Conflict(name = "timeInfoUnit", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution timeInfoUnitResolution;
	
	@Conflict(name = "timeInfoUnit", item = Conflict.Item.Output, defaultValue = "Best")
	private TimeInfoUnitOutput timeInfoUnitOutput;
	
	@Conflict(name = "timeInfoUnit", item = Conflict.Item.CustomValue, defaultValue = "milliseconds")
	private Unit customTimeInfoUnit;
	
	@Conflict(name = "interpolation", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution interpolationResolution;
	
	@Conflict(name = "interpolation", item = Conflict.Item.Output, defaultValue = "UseCustom")
	private InterpolationOutput interpolationOutput;
	
	@Conflict(name = "interpolation", item = Conflict.Item.CustomValue, defaultValue = "none")
	private Interpolation customInterpolation;
	
	@Conflict(name = "coordinateType", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution coordinateTypeResolution;
	
	@Conflict(name = "coordinateType", item = Conflict.Item.Output, defaultValue = "UseCustom")
	private CoordinateTypeOutput coordinateTypeOutput;
	
	@Conflict(name = "coordinateType", item = Conflict.Item.CustomValue, defaultValue = "XYZ")
	private String customCoordinateType;
	
	@Conflict(name = "description", item = Conflict.Item.Resolution, defaultValue = "ResolveWithWarning")
	private ConflictResolution descriptionResolution;
	
	@Conflict(name = "description", item = Conflict.Item.Output, defaultValue = "UseCustomPlusInputDescriptions")
	private DescriptionOutput descriptionOutput;
	
	@Conflict(name = "description", item = Conflict.Item.CustomValue, defaultValue = "Created by merging multiple files.\n\tOriginal descriptions:\n")
	private String customDescription;
	
	@Property(category = "conflict", name = "description.forceResolve", defaultValue = "true")
	private boolean forceResolveDescription;
	
	@Property(category = XML_OUTPUT_CATEGORY, name = "indentation", defaultValue = "OneSpace")
	private Indentation indentation;
	
	@Property(category = XML_OUTPUT_CATEGORY, name = "prefix.output", defaultValue = "DefaultNamespaceOnly")
	private PrefixOutput prefixOutput;
	
	@Property(category = XML_OUTPUT_CATEGORY, name = "prefix.custom", defaultValue = "wiseml")
	private String customPrefix;

	@Property(category = "lists", name = "scenario.mode", defaultValue = "SameIDsSortedAlphanumerically")
	private ListMergingMode scenarioListMergingMode;
	
	@Property(category = "lists", name = "scenario.customid", defaultValue = "0")
	private String customScenarioID;
	
	@Property(category = "lists", name = "trace.mode", defaultValue = "SameIDsSortedAlphanumerically")
	private ListMergingMode traceListMergingMode;
	
	@Property(category = "lists", name = "trace.customid", defaultValue = "0")
	private String customTraceID;
	
	@Property(category = "timestamps", name = "style.custom", defaultValue = "Offsets")
	private TimestampStyle customTimestampStyle;

	public MergerConfiguration() {
		this(getDefaultProperties(), false);
	}
	
	public MergerConfiguration(final Properties properties) {
		this(properties, true);
	}
	
	private MergerConfiguration(final Properties properties, 
			final boolean mergeWithDefaults) {
		Properties settings = properties;

        if (properties != null) {
            if (mergeWithDefaults) {
                settings = new Properties(getDefaultProperties());
                settings.putAll(properties);
            }
        } else {
            settings = getDefaultProperties();
        }
        
        Field[] fields = MergerConfiguration.class.getDeclaredFields();
		for (Field field : fields) {
			if (isMappedProperty(field)) {
				writeCurrentValue(field, settings.getProperty(getPropertyKey(field)));
			}
		}
	}

	public Properties getProperties() {
		Properties result = new Properties();
		
		Field[] fields = MergerConfiguration.class.getDeclaredFields();
		for (Field field : fields) {
			if (isMappedProperty(field)) {
				result.setProperty(getPropertyKey(field), getCurrentValue(field));
			}
		}
		return result;
	}
	
	public static final Properties getDefaultProperties() {
		Properties result = new Properties();
		
		Field[] fields = MergerConfiguration.class.getDeclaredFields();
		
		for (Field field : fields) {
			if (isMappedProperty(field)) {
				result.setProperty(getPropertyKey(field), getDefaultValue(field));
			}
		}
		return result;
	}
	
	private static boolean isMappedProperty(Field field) {
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		
		return field.getAnnotation(Property.class) != null
			|| field.getAnnotation(Conflict.class) != null;
	}
	
	private static String getDefaultValue(Field field) {
		Property property = field.getAnnotation(Property.class);
		if (property != null) {
			return property.defaultValue();
		}
		Conflict conflict = field.getAnnotation(Conflict.class);
		if (conflict != null) {
			return conflict.defaultValue();
		}
		return null;
	}
	
	/**
	 * Returns the current value of a field using the toString() method.
	 * 
	 * @param field
	 * @return
	 * @throws NullPointerException if the value is null.
	 */
	private String getCurrentValue(Field field) {
		field.setAccessible(true);
		try {
			return field.get(this).toString();
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets a field to a value encoded in a string.
	 * 
	 * @param field
	 * @param value
	 * @throws IllegalArgumentException if the value could not be parsed
	 */
	private void writeCurrentValue(Field field, String value) {
		if (value == null) {
			throw new IllegalArgumentException("cannot set "+field.getName()+" to null");
		}
		
		field.setAccessible(true);
		Class<?> fieldClass = field.getType();
		try {
			// Strings
			if (fieldClass.isAssignableFrom(String.class)) {
				field.set(this, value);
				return;
			}
			
			// Classes with a static valueOf method
			try {
				Method valueOfMethod = fieldClass.getMethod("valueOf", String.class);
				if (valueOfMethod.getReturnType().equals(fieldClass)) {
					Object obj = valueOfMethod.invoke(null, value);
					field.set(this, obj);
					return;
				}
			} catch (NoSuchMethodException e) {
			}
			
			// primitive types
			if (fieldClass.isPrimitive()) {
				if (fieldClass.equals(boolean.class)) {
					field.setBoolean(this, Boolean.valueOf(value).booleanValue());
				} else if (fieldClass.equals(int.class)) {
					field.setInt(this, Integer.valueOf(value).intValue());
				} else if (fieldClass.equals(short.class)) {
					field.setShort(this, Short.valueOf(value).shortValue());
				} else if (fieldClass.equals(long.class)) {
					field.setLong(this, Long.valueOf(value).longValue());
				} else if (fieldClass.equals(float.class)) {
					field.setFloat(this, Float.valueOf(value).floatValue());
				} else if (fieldClass.equals(double.class)) {
					field.setDouble(this, Double.valueOf(value).doubleValue());
				} else if (fieldClass.equals(byte.class)) {
					field.setByte(this, Byte.valueOf(value).byteValue());
				} else if (fieldClass.equals(char.class)) {
					field.setChar(this, value.charAt(0));
				} else {
					throw new IllegalArgumentException("could not assign '"+value+"' to member "+field.getName());
				}
				return;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("could not assign '"+value+"' to member "+field.getName(), e);
		}
		throw new IllegalArgumentException("could not assign '"+value+"' to member "+field.getName());
	}
	
	/**
	 * Generates a property key string for a field using annotations.
	 * 
	 * @param field annotated field
	 * @return property key or null if no annotation found
	 */
	private static String getPropertyKey(Field field) {
		Property property = field.getAnnotation(Property.class);
		if (property != null) {
			return MERGER_PREFIX + property.category() + "." + property.name();
		}
		Conflict conflict = field.getAnnotation(Conflict.class);
		if (conflict != null) {
			return MERGER_PREFIX + "." + CONFLICT_CATEGORY + "." + conflict.item().key() + "." + conflict.name();
		}
		return null;
	}

	public ConflictResolution getOriginResolution() {
		return originResolution;
	}

	public void setOriginResolution(ConflictResolution originResolution) {
		this.originResolution = originResolution;
	}

	public OriginOutput getOriginOutput() {
		return originOutput;
	}

	public void setOriginOutput(OriginOutput originOutput) {
		this.originOutput = originOutput;
	}

	public ConflictResolution getTimeInfoDurationResolution() {
		return timeInfoDurationResolution;
	}

	public void setTimeInfoDurationResolution(
			ConflictResolution timeInfoDurationResolution) {
		this.timeInfoDurationResolution = timeInfoDurationResolution;
	}

	public TimeInfoDurationOutput getTimeInfoDurationOutput() {
		return timeInfoDurationOutput;
	}

	public void setTimeInfoDurationOutput(
			TimeInfoDurationOutput timeInfoDurationOutput) {
		this.timeInfoDurationOutput = timeInfoDurationOutput;
	}

	public ConflictResolution getTimeInfoUnitResolution() {
		return timeInfoUnitResolution;
	}

	public void setTimeInfoUnitResolution(ConflictResolution timeInfoUnitResolution) {
		this.timeInfoUnitResolution = timeInfoUnitResolution;
	}

	public TimeInfoUnitOutput getTimeInfoUnitOutput() {
		return timeInfoUnitOutput;
	}

	public void setTimeInfoUnitOutput(TimeInfoUnitOutput timeInfoUnitOutput) {
		this.timeInfoUnitOutput = timeInfoUnitOutput;
	}

	public Unit getCustomTimeInfoUnit() {
		return customTimeInfoUnit;
	}

	public void setCustomTimeInfoUnit(Unit customTimeInfoUnit) {
		this.customTimeInfoUnit = customTimeInfoUnit;
	}

	public ConflictResolution getInterpolationResolution() {
		return interpolationResolution;
	}

	public void setInterpolationResolution(
			ConflictResolution interpolationResolution) {
		this.interpolationResolution = interpolationResolution;
	}

	public InterpolationOutput getInterpolationOutput() {
		return interpolationOutput;
	}

	public void setInterpolationOutput(InterpolationOutput interpolationOutput) {
		this.interpolationOutput = interpolationOutput;
	}

	public Interpolation getCustomInterpolation() {
		return customInterpolation;
	}

	public void setCustomInterpolation(Interpolation customInterpolation) {
		this.customInterpolation = customInterpolation;
	}

	public ConflictResolution getCoordinateTypeResolution() {
		return coordinateTypeResolution;
	}

	public void setCoordinateTypeResolution(
			ConflictResolution coordinateTypeResolution) {
		this.coordinateTypeResolution = coordinateTypeResolution;
	}

	public CoordinateTypeOutput getCoordinateTypeOutput() {
		return coordinateTypeOutput;
	}

	public void setCoordinateTypeOutput(CoordinateTypeOutput coordinateTypeOutput) {
		this.coordinateTypeOutput = coordinateTypeOutput;
	}

	public String getCustomCoordinateType() {
		return customCoordinateType;
	}

	public void setCustomCoordinateType(String customCoordinateType) {
		this.customCoordinateType = customCoordinateType;
	}

	public ConflictResolution getDescriptionResolution() {
		return descriptionResolution;
	}

	public void setDescriptionResolution(ConflictResolution descriptionResolution) {
		this.descriptionResolution = descriptionResolution;
	}

	public DescriptionOutput getDescriptionOutput() {
		return descriptionOutput;
	}

	public void setDescriptionOutput(DescriptionOutput descriptionOutput) {
		this.descriptionOutput = descriptionOutput;
	}

	public String getCustomDescription() {
		return customDescription;
	}

	public void setCustomDescription(String customDescription) {
		this.customDescription = customDescription;
	}

	public boolean isForceResolveDescription() {
		return forceResolveDescription;
	}

	public void setForceResolveDescription(boolean forceResolveDescription) {
		this.forceResolveDescription = forceResolveDescription;
	}

	public Indentation getIndentation() {
		return indentation;
	}

	public void setIndentation(Indentation indentation) {
		this.indentation = indentation;
	}

	public PrefixOutput getPrefixOutput() {
		return prefixOutput;
	}

	public void setPrefixOutput(PrefixOutput prefixOutput) {
		this.prefixOutput = prefixOutput;
	}

	public String getCustomPrefix() {
		return customPrefix;
	}

	public void setCustomPrefix(String customPrefix) {
		this.customPrefix = customPrefix;
	}
	
	public ListMergingMode getScenarioListMergingMode() {
		return scenarioListMergingMode;
	}

	public void setScenarioListMergingMode(ListMergingMode scenarioListMergingMode) {
		this.scenarioListMergingMode = scenarioListMergingMode;
	}

	public String getCustomScenarioID() {
		return customScenarioID;
	}

	public void setCustomScenarioID(String customScenarioID) {
		this.customScenarioID = customScenarioID;
	}

	public ListMergingMode getTraceListMergingMode() {
		return traceListMergingMode;
	}

	public void setTraceListMergingMode(ListMergingMode traceListMergingMode) {
		this.traceListMergingMode = traceListMergingMode;
	}

	public String getCustomTraceID() {
		return customTraceID;
	}

	public void setCustomTraceID(String customTraceID) {
		this.customTraceID = customTraceID;
	}

	public TimestampStyle getCustomTimestampStyle() {
		return customTimestampStyle;
	}

	public void setCustomTimestampStyle(TimestampStyle customTimestampStyle) {
		this.customTimestampStyle = customTimestampStyle;
	}
	
}
