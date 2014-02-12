package de.uniluebeck.itm.tr.snaa.shiro.dto;

import com.google.common.base.Function;
import de.uniluebeck.itm.tr.snaa.shiro.entity.*;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

public class DtoConverter {

	public static final Function<RoleDto, Role> DTO_TO_ROLE_FUNCTION = new Function<RoleDto, Role>() {
		@Override
		public Role apply(final RoleDto roleDto) {
			final Role role = new Role();
			role.setName(roleDto.getName());
			return role;
		}
	};

	public static final Function<Role, RoleDto> ROLE_TO_DTO_FUNCTION = new Function<Role, RoleDto>() {
		@Override
		public RoleDto apply(final Role role) {
			final RoleDto roleDto = new RoleDto();
			roleDto.setName(role.getName());
			return roleDto;
		}
	};

	public static final Function<User, UserDto> USER_TO_DTO_FUNCTION = new Function<User, UserDto>() {
		@Override
		public UserDto apply(final User user) {
			final UserDto userDto = new UserDto();
			userDto.setName(user.getName());
			userDto.setRoles(convertRoleSet(user.getRoles()));
			return userDto;
		}
	};

	public static final Function<Action, ActionDto> ACTION_TO_DTO_FUNCTION = new Function<Action, ActionDto>() {
		@Override
		public ActionDto apply(final Action action) {
			return new ActionDto(action.getName());
		}
	};

	public static final Function<Permission, PermissionDto> PERMISSION_TO_DTO_FUNCTION =
			new Function<Permission, PermissionDto>() {
				@Override
				public PermissionDto apply(final Permission permission) {
					return new PermissionDto(permission.getRole().getName(),
							permission.getAction().getName(),
							permission.getResourceGroup().getName()
					);
				}
			};

	public static final Function<UrnResourceGroup, String> URN_RESOURCE_GROUP_TO_URN_STRING_FUNCTION =
			new Function<UrnResourceGroup, String>() {
				@Override
				public String apply(final UrnResourceGroup input) {
					return input.getId().getUrn();
				}
			};

	public static final Function<ResourceGroup, ResourceGroupDto> RESOURCE_GROUP_TO_DTO_FUNCTION =
			new Function<ResourceGroup, ResourceGroupDto>() {
				@Override
				public ResourceGroupDto apply(final ResourceGroup input) {
					final ResourceGroupDto dto = new ResourceGroupDto();
					dto.setName(input.getName());
					dto.setNodeUrns(newArrayList(
							transform(input.getUrnResourceGroups(), URN_RESOURCE_GROUP_TO_URN_STRING_FUNCTION)
					)
					);
					return dto;
				}
			};

	public static Set<RoleDto> convertRoleSet(final Set<Role> roles) {
		return newHashSet(transform(roles, ROLE_TO_DTO_FUNCTION));
	}

	public static List<ActionDto> convertActionList(final List<Action> actions) {
		return newArrayList(transform(actions, ACTION_TO_DTO_FUNCTION));
	}

	public static List<PermissionDto> convertPermissionList(final List<Permission> permissions) {
		return newArrayList(transform(permissions, PERMISSION_TO_DTO_FUNCTION));
	}

	public static List<UserDto> convertUserList(final List<User> users) {
		return newArrayList(transform(users, USER_TO_DTO_FUNCTION));
	}

	public static UserDto convertUser(final User user) {
		return USER_TO_DTO_FUNCTION.apply(user);
	}

	public static List<ResourceGroupDto> convertResourceGroupList(final List<ResourceGroup> resourceGroups) {
		return newArrayList(transform(resourceGroups, RESOURCE_GROUP_TO_DTO_FUNCTION));
	}
}
