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
package de.uniluebeck.itm.ui;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import de.uniluebeck.itm.common.UiUtil;
import de.uniluebeck.itm.exception.AuthenticationException;
import de.uniluebeck.itm.ws.SNAAServiceAdapter;

/**
 * @author Sönke Nommensen
 */
public final class AuthenticationController implements Controller {

    private static AuthenticationController _instance = null;
    private final AuthenticationView _view;

    private AuthenticationController() {
        _view = new AuthenticationView();
        _view.btnAuthenticate.addListener(
                new Button.ClickListener() {

                    public void buttonClick(ClickEvent event) {
                        _view.frmAuthentication.commit();
                        startSNAAAuthentication();
                    }
                });
    }

    /**
     * @return Singelton reference
     */
    public static AuthenticationController get() {
        if (_instance == null) {
            _instance = new AuthenticationController();
        }
        return _instance;
    }

    /**
     * @return Reference to the view
     */
    public VerticalLayout view() {
        return _view;
    }

    /**
     * Starts authentication utilizing the SNAAServiceAdapter.
     */
    private void startSNAAAuthentication() {
        SNAAServiceAdapter snaaServiceAdapter = new SNAAServiceAdapter();
        snaaServiceAdapter.addAuthenticationTriple(
                getUsername(), getUrn(), getPassword());

        boolean success = true;
        try {
            snaaServiceAdapter.authenticate();
        } catch (AuthenticationException ex) {
            UiUtil.showNotification(
                    UiUtil.createNotificationCenteredTop(
                            "Login failed",
                            ex.getMessage(),
                            Window.Notification.TYPE_WARNING_MESSAGE));
            success = false;
        }

        if (success) {
            UiUtil.showNotification(
                    UiUtil.createNotificationCenteredTop(
                            "Authentication successful",
                            "User: \"" + getUsername()
                                    + "\" authenticated for: \""
                                    + getUrn() + "\"",
                            Window.Notification.TYPE_HUMANIZED_MESSAGE));
        }
    }

    private String getUsername() {
        return _view.txtUsername.getValue().toString().trim();
    }

    private String getPassword() {
        return _view.txtPassword.getValue().toString().trim();
    }

    private String getUrn() {
        return _view.txtUrn.getValue().toString().trim();
    }
}
