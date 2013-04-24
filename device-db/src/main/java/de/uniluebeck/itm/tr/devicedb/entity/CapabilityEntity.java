package de.uniluebeck.itm.tr.devicedb.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;

@Entity(name="Capability")
public class CapabilityEntity {
	
	private static final Function<Capability,CapabilityEntity> CAP_TO_ENTITY_FUNCTION =
			new Function<Capability, CapabilityEntity>() {
				@Override
				public CapabilityEntity apply(Capability capability) {
					return new CapabilityEntity(capability);
				}
			};
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
			
	@Column(nullable=false)
	private String name;
	
	@Column(nullable=false)
	private String defaultValue;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private Dtypes datatype;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private Units unit;
	
	public CapabilityEntity() { }
	
	public CapabilityEntity(Capability capability) {
		this.name = capability.getName();
		this.defaultValue = capability.getDefault();
		this.datatype = capability.getDatatype();
		this.unit = capability.getUnit();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Dtypes getDatatype() {
		return datatype;
	}

	public void setDatatype(Dtypes datatype) {
		this.datatype = datatype;
	}

	public Units getUnit() {
		return unit;
	}

	public void setUnit(Units unit) {
		this.unit = unit;
	}

	public static Set<CapabilityEntity> fromCapabilitySet(
			Set<Capability> capabilities) {
		
		if (capabilities==null) return null;
		
		Collection<CapabilityEntity> coll = Collections2.transform(capabilities, CAP_TO_ENTITY_FUNCTION);
		Set<CapabilityEntity> set = new HashSet<CapabilityEntity>(coll);
		
		return set;
		
	}
	
	public Capability toCapability() {
		Capability cap = new Capability();
		cap.setDatatype(datatype);
		cap.setDefault(defaultValue);
		cap.setName(name);
		cap.setUnit(unit);
		
		return cap;
	}
	
}
