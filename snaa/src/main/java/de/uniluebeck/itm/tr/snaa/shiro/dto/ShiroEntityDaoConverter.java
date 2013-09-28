package de.uniluebeck.itm.tr.snaa.shiro.dto;

import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Role;
import de.uniluebeck.itm.tr.snaa.shiro.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ShiroEntityDaoConverter {

    public static UserListDto userList(List<User> users) {

        List<UserDto> res = new ArrayList<UserDto>();
        for ( User user : users ) {
            UserDto dto = new UserDto( user.getName(), toDto(user.getRoles()) );
            res.add(dto);
        }
        return new UserListDto(res);
    }

    public static Set<RoleDto> toDto(Set<Role> roles) {
        Set<RoleDto> res = Sets.newHashSet();
        for ( Role role : roles ) {
            res.add(new RoleDto(role.getName()));
        }
        return res;
    }
}
