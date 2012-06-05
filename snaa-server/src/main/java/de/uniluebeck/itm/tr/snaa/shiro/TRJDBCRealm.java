package de.uniluebeck.itm.tr.snaa.shiro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.util.JdbcUtils;

public class TRJDBCRealm extends JdbcRealm {
    /**
     * The default query used to retrieve account data for the user.
     */
    protected static final String DEFAULT_AUTHENTICATION_QUERY = "select password from users where name = ?";
    
    /**
     * The default query used to retrieve account data for the user when {@link #saltStyle} is COLUMN.
     */
    protected static final String DEFAULT_SALTED_AUTHENTICATION_QUERY = "select password, salt from users where name = ?";

    /**
     * The default query used to retrieve the roles that apply to a user.
     */
    protected static final String DEFAULT_USER_ROLES_QUERY = "select role_name from user_roles where user_name = ?";

    /**
     * The default query used to retrieve permissions that apply to a particular role.
     */
    protected static final String DEFAULT_PERMISSIONS_QUERY = "select action_name, resourcegroup_name from permissions where role_name = ?";
    
    @Override
    protected Set<String> getPermissions(Connection conn, String username, Collection<String> roleNames) throws SQLException {
        PreparedStatement ps = null;
        Set<String> permissions = new LinkedHashSet<String>();
        try {
            ps = conn.prepareStatement(permissionsQuery);
            for (String roleName : roleNames) {

                ps.setString(1, roleName);

                ResultSet rs = null;

                try {
                    // Execute query
                    rs = ps.executeQuery();

                    // Loop over results and add each returned role to a set
                    while (rs.next()) {

                        String permissionString = rs.getString(1) + ":" +rs.getString(2);

                        // Add the permission to the set of permissions
                        permissions.add(permissionString);
                    }
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }

            }
        } finally {
            JdbcUtils.closeStatement(ps);
        }

        return permissions;
    }
    
}
