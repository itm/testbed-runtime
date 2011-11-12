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

package eu.wisebed.testbed.api.snaa.authorization.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.sql.SQLException;

public class ShibbolethDataSource implements AuthorizationDataSource {

    private static final Logger log = LoggerFactory.getLogger(ShibbolethDataSource.class);

    private MySQLConnection connection;
    private String dbUser;
    private String dbPwd;
    private String dbUrl;

    private static String userIDQuery = "SELECT user_id FROM User WHERE user_uid = '{}'";
    private static String actionIDQuery = "SELECT action_id from Action WHERE action_name = '{}'";
    private static String subscriptionRole = "SELECT subscription_role FROM Subscription, ActionManager WHERE Subscription.subscription_role = ActionManager.role_id " +
            " AND Subscription.subscription_user = '{}' AND Subscription.subscription_state ='1' AND ActionManager.action_id = '{}'";

    //sql-connects

    public int getUserId(String user_uid) throws SQLException {
        return connection.getSingleInt(MessageFormatter.format(userIDQuery, user_uid.trim()).getMessage(), "user_id");
    }

    public int getActionId(String action) throws SQLException {
        return connection.getSingleInt(MessageFormatter.format(actionIDQuery, action.trim()).getMessage(), "action_id");
    }

    public int getSubscriptionRole(int userId, int actionId) throws Exception {
        return connection.getSingleInt(MessageFormatter.format(subscriptionRole, userId, actionId).getMessage(), "subscription_role");
    }


    @Override
    public void setUsername(String username) {
        this.dbUser = username;
    }

    @Override
    public void setPassword(String password) {
        this.dbPwd = password;
    }

    @Override
    public void setUrl(String url) {
        this.dbUrl = url;
    }

    @Override
    public boolean isAuthorized(String puid, String action) throws Exception {
        try {
            connection = new MySQLConnection(dbUrl, dbUser, dbPwd);

            int user_id = getUserId(puid);
            int action_id = getActionId(action);

            //get role for user and action
            //if no role found for user and action a NullPointerException is thrown
            getSubscriptionRole(user_id, action_id);

            return true;
        }
        catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
        finally {
            connection.disconnect();
        }
    }
}
