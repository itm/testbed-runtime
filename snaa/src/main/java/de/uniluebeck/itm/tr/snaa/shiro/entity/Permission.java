package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.*;

@Entity
@Table(name = "PERMISSIONS")
public class Permission implements java.io.Serializable {

	@EmbeddedId
	@AttributeOverrides({
			@AttributeOverride(name = "roleName", column = @Column(name = "ROLE_NAME", nullable = false, length = 150)),
			@AttributeOverride(name = "actionName",
					column = @Column(name = "ACTION_NAME", nullable = false, length = 30)),
			@AttributeOverride(name = "resourcegroupName", column = @Column(name = "RESOURCEGROUP_NAME", length = 40))
	})
	private PermissionId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ACTION_NAME", nullable = false, insertable = false, updatable = false)
	private Action action;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RESOURCEGROUP_NAME", insertable = false, updatable = false)
	private ResourceGroup resourceGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ROLE_NAME", nullable = false, insertable = false, updatable = false)
	private Role role;

	public Permission() {

	}

	public Permission(PermissionId id, Action action, Role role) {
		this.id = id;
		this.action = action;
		this.role = role;
	}

	public Permission(PermissionId id, Action action, ResourceGroup resourceGroup, Role role) {
		this.id = id;
		this.action = action;
		this.resourceGroup = resourceGroup;
		this.role = role;
	}

	public PermissionId getId() {
		return this.id;
	}

	public void setId(PermissionId id) {
		this.id = id;
	}

	public Action getAction() {
		return this.action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public ResourceGroup getResourceGroup() {
		return this.resourceGroup;
	}

	public void setResourceGroup(ResourceGroup resourceGroup) {
		this.resourceGroup = resourceGroup;
	}

	public Role getRole() {
		return this.role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
