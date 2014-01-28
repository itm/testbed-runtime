package de.uniluebeck.itm.tr.snaa.shiro.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement
public class UserDto {

    private String name;
    private Set<String> roles;
    private String password;

    public UserDto(String name, Set<String> roles) {
        this.name = name;
        this.roles = roles;
    }

    public UserDto(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public UserDto() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
