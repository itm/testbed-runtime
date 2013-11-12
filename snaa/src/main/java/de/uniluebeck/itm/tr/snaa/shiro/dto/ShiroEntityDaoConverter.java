package de.uniluebeck.itm.tr.snaa.shiro.dto;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Action;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Permission;
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

    public static List<ActionDto> toActionDtoList(List<Action> actions) {
        List<ActionDto> res = Lists.newArrayList();
        for ( Action action : actions ) {
            ActionDto dto = new ActionDto(action.getName());
            res.add(dto);
        }
        return res;
    }

    public static List<PermissionDto> toPermissionDtoList(List<Permission> permissions) {
        List<PermissionDto> res = Lists.newArrayList();
        for ( Permission permission : permissions ) {
            PermissionDto dto = new PermissionDto(  permission.getRole().getName(),
                                                    permission.getAction().getName(),
                                                    permission.getResourceGroup().getName());
            res.add(dto);
        }
        return res;
    }
}
