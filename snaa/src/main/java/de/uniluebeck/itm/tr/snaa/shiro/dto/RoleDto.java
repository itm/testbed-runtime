package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;

import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement
public class RoleDto {

    private String name;

	public RoleDto() {
	}

	public RoleDto(String name) {
        this.name = checkNotNull(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final RoleDto roleDto = (RoleDto) o;

		return name.equals(roleDto.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
