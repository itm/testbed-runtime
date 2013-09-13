package de.uniluebeck.itm.tr.devicedb.entity;

import javax.persistence.*;

@Entity
@Cacheable
public class KeyValueEntity {
	
	@SuppressWarnings("unused")
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

	private String key_i;
	
	private String value_i;
	
	public KeyValueEntity() { }

	public KeyValueEntity(String key, String value) {
		this.key_i = key;
		this.value_i = value;
	}

	public String getKey() {
		return key_i;
	}

	public void setKey(String key) {
		this.key_i = key;
	}

	public String getValue() {
		return value_i;
	}

	public void setValue(String value) {
		this.value_i = value;
	}
	
}
