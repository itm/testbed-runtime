/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;

/**
 * @author Soenke Nommensen
 */
public class AuthenticationView extends VerticalLayout {

    private static final String USERNAME_LABEL = "Username";
    private static final String URN_LABEL = "URN (+)";
    private static final String PASSWORD_LABEL = "Password";
    private static final String AUTHENTICATION_BUTTON_LABEL = "Authenticate";
    private static final String AUTHENTICATION_LABEL = "Authentication";
    private static final String REQUIRED_ERROR_TEXT = "Please provide a valid %s!";
    private static final int COMMON_FIELD_WIDTH_EM = 20;
    
    /* UI elements */
    final Form frmAuthentication = new Form();
    final TextField txtUsername = new TextField(USERNAME_LABEL);
    final TextField txtUrn = new TextField(URN_LABEL);
    final TextField txtPassword = new TextField(PASSWORD_LABEL);
    final Button btnAuthenticate = new Button(AUTHENTICATION_BUTTON_LABEL);

    public AuthenticationView() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        addStyleName(Reindeer.LAYOUT_WHITE);

        final Label lblLogin = new Label(AUTHENTICATION_LABEL);
        lblLogin.addStyleName(Reindeer.LABEL_H2);

        addComponent(lblLogin);

        frmAuthentication.setSizeUndefined();
        frmAuthentication.setWriteThrough(false); // we want explicit 'apply'
        frmAuthentication.setInvalidCommitted(false); // no invalid values in login details

        //btnAuthenticate.addStyleName(Reindeer.BUTTON_SMALL);

        txtUsername.setWidth(COMMON_FIELD_WIDTH_EM, UNITS_EM);
        txtUsername.setRequired(true);
        txtUsername.setRequiredError(String.format(REQUIRED_ERROR_TEXT, USERNAME_LABEL));
        frmAuthentication.addField(USERNAME_LABEL, txtUsername);
        txtUsername.focus();

        txtUrn.setWidth(COMMON_FIELD_WIDTH_EM, UNITS_EM);
        txtUrn.setRequired(true);
        txtUrn.setRequiredError(String.format(REQUIRED_ERROR_TEXT, URN_LABEL));
        txtUrn.setValue("urn:wisebed:uzl1:");
        frmAuthentication.addField(URN_LABEL, txtUrn);

        txtPassword.setWidth(COMMON_FIELD_WIDTH_EM, UNITS_EM);
        txtPassword.setSecret(true);
        txtPassword.setRequired(true);
        txtPassword.setRequiredError(String.format(REQUIRED_ERROR_TEXT, PASSWORD_LABEL));
        frmAuthentication.addField(PASSWORD_LABEL, txtPassword);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setSpacing(true);
        footer.addComponent(btnAuthenticate);

        frmAuthentication.setFooter(footer);

        addComponent(frmAuthentication);
        setExpandRatio(frmAuthentication, 1);
        //setComponentAlignment(frmAuthentication, Alignment.MIDDLE_CENTER);
    }
}
