package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ResourceGroupDto {

	private String name;

	private List<String> nodeUrns;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<String> getNodeUrns() {
		return nodeUrns;
	}

	public void setNodeUrns(final List<String> nodeUrns) {
		this.nodeUrns = nodeUrns;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ResourceGroupDto that = (ResourceGroupDto) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
