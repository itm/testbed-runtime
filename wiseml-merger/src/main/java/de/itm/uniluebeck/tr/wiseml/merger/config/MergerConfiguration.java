package de.itm.uniluebeck.tr.wiseml.merger.config;

import java.util.Properties;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

/**
 * Represents the configuration of a WiseML merging operation.
 * Main objective is to select strategies for the resolution of various
 * conflicts that may arise.
 * 
 * @author kuypers
 *
 */
public class MergerConfiguration {
	
	/*
	 * Adding a configuration property:
	 * (1) define a constant for the property name
	 * (2) declare a property variable
	 * (3) add a line to the constructor
	 * (4) add a line to getProperties
	 * (5) add a line to getDefaultProperties
	 * ... add setter/getter
	 * 
	 * If a property uses a constant set of possible values, define an enum.
	 */
	
	// property names (1)
	public static final String ORIGIN_CONFLICT = 
		"wiseml.merger.conflict.origin";
	public static final String ORIGIN_OUTPUT =
		"wiseml.merger.output.origin";
	public static final String TIMEINFO_DURATION_CONFLICT =
		"wiseml.merger.conflict.timeinfo_duration";
	public static final String TIMEINFO_DURATION_OUTPUT = 
		"wiseml.merger.output.timeinfo_duration";
	public static final String TIMEINFO_UNIT_CONFLICT = 
		"wiseml.merger.conflict.timeinfo_unit";
	public static final String TIMEINFO_UNIT_OUTPUT =
		"wiseml.merger.output.timeinfo_unit";
	public static final String TIMEINFO_UNIT_CUSTOM =
		"wiseml.merger.custom.timeinfo_unit";
	public static final String INTERPOLATION_CONFLICT =
		"wiseml.merger.conflict.interpolation";
	public static final String INTERPOLATION_OUTPUT =
		"wiseml.merger.output.interpolation";
	public static final String INTERPOLATION_CUSTOM =
		"wiseml.merger.custom.interpolation";
	public static final String COORDINATE_TYPE_CONFLICT =
		"wiseml.merger.conflict.coordinate_type";
	public static final String COORDINATE_TYPE_OUTPUT =
		"wiseml.merger.output.coordinate_type";
	public static final String COORDINATE_TYPE_CUSTOM =
		"wiseml.merger.custom.coordinate_type";
	public static final String DESCRIPTION_CONFLICT = 
		"wiseml.merger.conflict.description";
	public static final String DESCRIPTION_OUTPUT = 
		"wiseml.merger.output.description";
	public static final String DESCRIPTION_CUSTOM = 
		"wiseml.merger.custom.description";
	
	// property variables (2)
	private ConflictResolution originConflict;
	private OriginOutput originOutput;
	private ConflictResolution timeInfoDurationConflict;
	private TimeInfoDurationOutput timeInfoDurationOutput;
	private ConflictResolution timeInfoUnitConflict;
	private TimeInfoUnitOutput timeInfoUnitOutput;
	private Unit customTimeInfoUnit;
	private ConflictResolution interpolationConflict;
	private InterpolationOutput interpolationOutput;
	private Interpolation customInterpolation;
	private ConflictResolution coordinateTypeConflict;
	private CoordinateTypeOutput coordinateTypeOutput;
	private String customCoordinateType;
	private ConflictResolution descriptionConflict;
	private DescriptionOutput descriptionOutput;
	private String customDescriptionText;
	
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
		
		// read properties to property variables (3)
		originConflict = readEnumProperty(settings, ORIGIN_CONFLICT,
				ConflictResolution.class);
		originOutput = readEnumProperty(settings, ORIGIN_OUTPUT, 
				OriginOutput.class);
		timeInfoDurationConflict = readEnumProperty(settings, 
				TIMEINFO_DURATION_CONFLICT, ConflictResolution.class);
		timeInfoDurationOutput = readEnumProperty(settings, 
				TIMEINFO_DURATION_OUTPUT, TimeInfoDurationOutput.class);
		timeInfoUnitConflict = readEnumProperty(settings, 
				TIMEINFO_UNIT_CONFLICT, ConflictResolution.class);
		timeInfoUnitOutput = readEnumProperty(settings, TIMEINFO_UNIT_OUTPUT, 
				TimeInfoUnitOutput.class);
		customTimeInfoUnit = readEnumProperty(settings, TIMEINFO_UNIT_CUSTOM, 
				Unit.class);
		interpolationConflict = readEnumProperty(settings, 
				INTERPOLATION_CONFLICT, ConflictResolution.class);
		interpolationOutput = readEnumProperty(settings, INTERPOLATION_OUTPUT, 
				InterpolationOutput.class);
		customInterpolation = readEnumProperty(settings, INTERPOLATION_CUSTOM, 
				Interpolation.class);
		coordinateTypeConflict = readEnumProperty(settings, 
				COORDINATE_TYPE_CONFLICT, ConflictResolution.class);
		coordinateTypeOutput = readEnumProperty(settings, 
				COORDINATE_TYPE_OUTPUT, CoordinateTypeOutput.class);
		customCoordinateType = readStringProperty(settings, 
				COORDINATE_TYPE_CUSTOM);
		descriptionConflict = readEnumProperty(settings, DESCRIPTION_CONFLICT, 
				ConflictResolution.class);
		descriptionOutput = readEnumProperty(settings, DESCRIPTION_OUTPUT, 
				DescriptionOutput.class);
		customDescriptionText = readStringProperty(settings, 
				DESCRIPTION_CUSTOM);
	}
	
	public Properties getProperties() {
		Properties result = new Properties();
		
		// write property variables to properties (4)
		writeEnumProperty(result, ORIGIN_CONFLICT, originConflict);
		writeEnumProperty(result, ORIGIN_OUTPUT, originOutput);
		writeEnumProperty(result, TIMEINFO_DURATION_CONFLICT, 
				timeInfoDurationConflict);
		writeEnumProperty(result, TIMEINFO_DURATION_OUTPUT, 
				timeInfoDurationOutput);
		writeEnumProperty(result, TIMEINFO_UNIT_CONFLICT, timeInfoUnitConflict);
		writeEnumProperty(result, TIMEINFO_UNIT_OUTPUT, timeInfoUnitOutput);
		writeEnumProperty(result, TIMEINFO_UNIT_CUSTOM, customTimeInfoUnit);
		writeEnumProperty(result, INTERPOLATION_CONFLICT, 
				interpolationConflict);
		writeEnumProperty(result, INTERPOLATION_OUTPUT, interpolationOutput);
		writeEnumProperty(result, INTERPOLATION_CUSTOM, customInterpolation);
		writeEnumProperty(result, COORDINATE_TYPE_CONFLICT, 
				coordinateTypeConflict);
		writeEnumProperty(result, COORDINATE_TYPE_OUTPUT, coordinateTypeOutput);
		writeStringProperty(result, COORDINATE_TYPE_CUSTOM, 
				customCoordinateType);
		writeEnumProperty(result, DESCRIPTION_CONFLICT, descriptionConflict);
		writeEnumProperty(result, DESCRIPTION_OUTPUT, descriptionOutput);
		writeStringProperty(result, DESCRIPTION_CUSTOM, customDescriptionText);
		
		return result;
	}
	
	public static final Properties getDefaultProperties() {
		Properties result = new Properties();
		
		// write default properties (5)
		writeEnumProperty(result, ORIGIN_CONFLICT, 
				ConflictResolution.ResolveWithWarning);
		writeEnumProperty(result, ORIGIN_OUTPUT, 
				OriginOutput.UseFirstOriginAndTransform);
		writeEnumProperty(result, TIMEINFO_DURATION_CONFLICT, 
				ConflictResolution.ResolveSilently);
		writeEnumProperty(result, TIMEINFO_DURATION_OUTPUT, 
				TimeInfoDurationOutput.FirstFileSelect);
		writeEnumProperty(result, TIMEINFO_UNIT_CONFLICT, 
				ConflictResolution.ResolveWithWarning);
		writeEnumProperty(result, TIMEINFO_UNIT_OUTPUT, 
				TimeInfoUnitOutput.FirstFileSelect);
		writeEnumProperty(result, TIMEINFO_UNIT_CUSTOM, Unit.milliseconds);
		writeEnumProperty(result, INTERPOLATION_CONFLICT, 
				ConflictResolution.ResolveWithWarning);
		writeEnumProperty(result, INTERPOLATION_OUTPUT, 
				InterpolationOutput.UseBest);
		writeEnumProperty(result, INTERPOLATION_CUSTOM, Interpolation.none);
		writeEnumProperty(result, COORDINATE_TYPE_CONFLICT, 
				ConflictResolution.ThrowException);
		writeEnumProperty(result, COORDINATE_TYPE_OUTPUT, 
				CoordinateTypeOutput.UseFirstFile);
		writeStringProperty(result, COORDINATE_TYPE_CUSTOM, "XYZ");
		writeEnumProperty(result, DESCRIPTION_CONFLICT, 
				ConflictResolution.ResolveSilently);
		writeEnumProperty(result, DESCRIPTION_OUTPUT, 
				DescriptionOutput.UseCustomPlusInputDescriptions);
		writeStringProperty(result, DESCRIPTION_CUSTOM, 
				"WiseML file generated by merging multiple sources.");
		
		return result;
	}
	
	public ConflictResolution getOriginConflict() {
		return originConflict;
	}

	public void setOriginConflict(ConflictResolution originConflict) {
		this.originConflict = originConflict;
	}

	public OriginOutput getOriginOutput() {
		return originOutput;
	}

	public void setOriginOutput(OriginOutput originOutput) {
		this.originOutput = originOutput;
	}

	public ConflictResolution getTimeInfoDurationConflict() {
		return timeInfoDurationConflict;
	}

	public void setTimeInfoDurationConflict(
			ConflictResolution timeInfoDurationConflict) {
		this.timeInfoDurationConflict = timeInfoDurationConflict;
	}

	public TimeInfoDurationOutput getTimeInfoDurationOutput() {
		return timeInfoDurationOutput;
	}

	public void setTimeInfoDurationOutput(
			TimeInfoDurationOutput timeInfoDurationOutput) {
		this.timeInfoDurationOutput = timeInfoDurationOutput;
	}

	public ConflictResolution getTimeInfoUnitConflict() {
		return timeInfoUnitConflict;
	}

	public void setTimeInfoUnitConflict(ConflictResolution timeInfoUnitConflict) {
		this.timeInfoUnitConflict = timeInfoUnitConflict;
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

	public ConflictResolution getDescriptionConflict() {
		return descriptionConflict;
	}

	public void setDescriptionConflict(ConflictResolution descriptionConflict) {
		this.descriptionConflict = descriptionConflict;
	}

	public DescriptionOutput getDescriptionOutput() {
		return descriptionOutput;
	}

	public void setDescriptionOutput(DescriptionOutput descriptionOutput) {
		this.descriptionOutput = descriptionOutput;
	}

	public String getCustomDescriptionText() {
		return customDescriptionText;
	}

	public void setCustomDescriptionText(String customDescriptionText) {
		this.customDescriptionText = customDescriptionText;
	}

	private static final <E extends Enum<E>> E readEnumProperty(
			final Properties properties, final String propertyName, 
			final Class<E> enumClass) {
		String value = properties.getProperty(propertyName);
		if (value == null) {
			throw new IllegalArgumentException("value for property '" + 
					propertyName + "' not found");
		}
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("illegal value for property '"
					+ propertyName + "': " + value);
		}
	}
	
	private static final String readStringProperty(
			final Properties properties, final String propertyName) {
		String value = properties.getProperty(propertyName);
		if (value == null) {
			throw new IllegalArgumentException("value for property '" + 
					propertyName + "' not found");
		}
		return value;
	}
	
	private static final <E extends Enum<E>> void writeEnumProperty(
			final Properties properties, final String propertyName, 
			final E enumValue) {
		 if (enumValue == null) {
			 throw new IllegalArgumentException("value for property '" + 
					 propertyName + "' is null");
		 }
		 properties.setProperty(propertyName, enumValue.toString());
	}
	
	private static final void writeStringProperty(final Properties properties, 
			final String propertyName, final String stringValue) {
		if (stringValue == null) {
			 throw new IllegalArgumentException("value for property '" + 
					 propertyName + "' is null");
		 }
		properties.setProperty(propertyName, stringValue);
	}

}
