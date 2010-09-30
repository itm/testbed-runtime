/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.testbed.api.snaa.authorization;

import de.uniluebeck.itm.tr.util.MySQLConnection;
import eu.wisebed.testbed.api.snaa.v1.Action;
import eu.wisebed.testbed.api.snaa.v1.SNAAExceptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AttributeBasedAuthorization implements IUserAuthorization {

    private Map<String, String> attributes;

    private static String dbUser = "root";
    private static String dbPwd = "";
    private static String dbUrl = "jdbc:mysql://localhost:3306/snaportal";

    private MySQLConnection connection;

    private static final Logger log = LoggerFactory.getLogger(AttributeBasedAuthorization.class);

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean isAuthorized(Action action, UserDetails details) throws SNAAExceptionException {
        String uid = null;
        //check if user is authorised in Database
        try {
            //get uid

            List<Object> uidList = details.getUserDetails().get("personUniqueID");
            if (uidList == null) return false;

            uid = (String) uidList.get(0);

            connection = new MySQLConnection(dbUrl, dbUser, dbPwd);
            int user_id = getUserId(uid);
            int action_id = getActionId(action.getAction());

            getSubscriptionRole(user_id, action_id);

            connection.disconnect();

            //check authorization for attribute-Map
            for (Object key : details.getUserDetails().keySet()) {
                String regex = getRegex(key);
                if (regex != null) {
                    if (!compareValues(regex, details.getUserDetails().get(key))) throw new Exception();
                }
            }

        }
        catch (Exception e) {
            log.warn("Warning: User: " + uid + " not authorized for action: " + action.getAction());
            return false;
        }

        return true;
    }


    private String getRegex(Object key) {
        if (attributes == null) return null;
        for (Object keyRegex : attributes.keySet()) {
            String keyRegexString = (String) keyRegex;
            if (((String) key).matches(keyRegexString)) {
                return keyRegexString;
            }
        }
        return null;
    }

    private boolean compareValues(String regex, List<Object> cmpValues) {
        for (Object value : cmpValues) {
            if (!compareValue(regex, value)) {
                log.warn("no matching of: " + regex + " on " + value);
                return false;
            }
        }
        return true;
    }

    private boolean compareValue(String regex, Object value) {
        return (((String) value).matches(attributes.get(regex)));
    }

    private static String userIDQuery = "SELECT user_id FROM User WHERE user_uid = '{}'";
    private static String actionIDQuery = "SELECT action_id from Action WHERE action_name = '{}'";
    private static String subscriptionRole = "SELECT subscription_role FROM Subscription, ActionManager WHERE Subscription.subscription_role = ActionManager.role_id " +
                " AND Subscription.subscription_user = '{}' AND Subscription.subscription_state ='1' AND ActionManager.action_id = '{}'";

    //sql-connects
    private int getUserId(String user_uid) throws SQLException {
        return connection.getSingleInt(MessageFormatter.format(userIDQuery, user_uid.trim()), "user_id");
    }

    public int getActionId(String action) throws SQLException {
        return connection.getSingleInt(MessageFormatter.format(actionIDQuery, action.trim()), "action_id");
    }

    public int getSubscriptionRole(int userId, int actionId) throws Exception {
        return connection.getSingleInt(MessageFormatter.format(subscriptionRole, userId, actionId), "subscription_role");
    }
}
