package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PermissionDto {

    private String roleName;
    private String actionName;
    private String resourceGroupName;

    public PermissionDto(String roleName, String actionName, String resourceGroupName) {
        this.roleName = roleName;
        this.actionName = actionName;
        this.resourceGroupName = resourceGroupName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }
}
