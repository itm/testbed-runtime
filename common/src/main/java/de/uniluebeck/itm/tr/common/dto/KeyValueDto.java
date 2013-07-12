package de.uniluebeck.itm.tr.common.dto;

public class KeyValueDto {

	private String key;

	private String value;

	@SuppressWarnings("unused")
	public KeyValueDto() {
	}

	public KeyValueDto(final String key, final String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}
}
