package de.uniluebeck.itm.tr.devicedb.entity;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import eu.wisebed.wiseml.Capability;
import eu.wisebed.wiseml.Dtypes;
import eu.wisebed.wiseml.Units;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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
	@SuppressWarnings("unused")
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
		return new HashSet<CapabilityEntity>(Collections2.transform(capabilities, CAP_TO_ENTITY_FUNCTION));
	}
	
	public Capability toCapability() {
		Capability cap = new Capability();
		cap.setDatatype(getDatatype());
		cap.setDefault(getDefaultValue());
		cap.setName(getName());
		cap.setUnit(getUnit());
		return cap;
	}
	
}
