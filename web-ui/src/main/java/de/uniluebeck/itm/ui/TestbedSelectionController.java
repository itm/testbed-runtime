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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.LoginForm.LoginEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.uniluebeck.itm.common.UiUtil;
import de.uniluebeck.itm.exception.AuthenticationException;
import de.uniluebeck.itm.model.NodeUrnContainer;
import de.uniluebeck.itm.model.TestbedConfiguration;
import de.uniluebeck.itm.services.SNAAServiceAdapter;
import de.uniluebeck.itm.services.SessionManagementAdapter;
import de.uniluebeck.itm.services.TestbedConfigurationService;
import de.uniluebeck.itm.services.XmlFileTestbedConfigurationService;

/**
 * @author Soenke Nommensen
 */
public final class TestbedSelectionController implements Controller {

    private List<TestbedConfiguration> testbeds;
    private final TestbedSelectionView view = new TestbedSelectionView();
    private TestbedConfiguration currentTestbedConfiguration = null;

    public TestbedSelectionController() {
        view.getConnectButton().addListener(new ClickListener() {

            public void buttonClick(ClickEvent event) {
                onConnectButtonClick();
            }
        });
        view.getTestbedConfigurationSelect().addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                TestbedConfiguration testbedConfiguration = (TestbedConfiguration) event.getProperty().getValue();
                onSelectTestbedConfiguration(testbedConfiguration);
            }
        });
        view.getReloadButton().addListener(new ClickListener() {

            public void buttonClick(ClickEvent event) {
                if (currentTestbedConfiguration != null) {
                    onLoadNetwork(currentTestbedConfiguration.getSessionmanagementEndointUrl());
                }
            }
        });
        view.getLoginForm().addListener(new LoginForm.LoginListener() {

            public void onLogin(LoginEvent event) {
                connectToTestBed(event.getLoginParameter("username"), event.getLoginParameter("password"));
            }
        });

        loadConfigurations();

        view.getConnectButton().setEnabled(false);
        view.getReloadButton().setEnabled(false);
    }
    
	private void loadConfigurations() {
		TestbedConfigurationService service = new XmlFileTestbedConfigurationService();
		testbeds = service.getConfigurations();
		for (TestbedConfiguration bed : testbeds) {
			view.addTestBed(bed);
		}
	}

    private void onConnectButtonClick() {
        view.getLoginWindow().setCaption("Login to " + currentTestbedConfiguration.getName());
        view.setShowLoginWindow(true);
    }

    private void connectToTestBed(String username, String password) {
        final List<String> exceptions = new ArrayList<String>(currentTestbedConfiguration.getUrnPrefixList().size());
        for (final String urn : currentTestbedConfiguration.getUrnPrefixList()) {
            try {
                authenticate(urn, username, password);
            } catch (AuthenticationException e) {
                exceptions.add(urn + " - " + e.getMessage());
            }
        }

        if (exceptions.isEmpty()) {
            view.setShowLoginWindow(false);
            UiUtil.showNotification(UiUtil.createNotificationCenteredTop(
                    "Authentication successful", "User: \"" + username
                    + "\" authenticated for: \"" + currentTestbedConfiguration.getName() + "\"",
                    Window.Notification.TYPE_HUMANIZED_MESSAGE));
        } else {
            String title = "Authentication error";
            String msg = "";
            for (String exception : exceptions) {
                msg = msg + "<br/>" + exception;
            }
            UiUtil.showNotification(UiUtil.createNotificationCenteredTop(
                    title, msg, Window.Notification.TYPE_WARNING_MESSAGE));
        }
    }

    private void onSelectTestbedConfiguration(TestbedConfiguration testbedConfiguration) {
        currentTestbedConfiguration = testbedConfiguration;

        String details = testbedConfiguration.getDescription();

        view.setDetailsText(details);
        view.getConnectButton().setEnabled(currentTestbedConfiguration != null);
        onLoadNetwork(testbedConfiguration.getSessionmanagementEndointUrl());
    }

    private void onLoadNetwork(String url) {
        view.getReloadButton().setEnabled(false);
        SessionManagementAdapter sessionManagementAdapter = new SessionManagementAdapter(url);
        try {
            NodeUrnContainer container = sessionManagementAdapter.getNetworkAsContainer();
            view.setDeviceContainer(container);
        } catch (InstantiationException ex) {
            UiUtil.showExceptionNotification(ex);
        } catch (IllegalAccessException ex) {
            UiUtil.showExceptionNotification(ex);
        }
        view.getReloadButton().setEnabled(true);
    }

    /**
     * @return Reference to the view
     */
    public VerticalLayout view() {
        return view;
    }

    /**
     * Starts authentication utilizing the SNAAServiceAdapter.
     * @throws AuthenticationException
     */
    private void authenticate(String urn, String username, String password) throws AuthenticationException {
        SNAAServiceAdapter snaaServiceAdapter = new SNAAServiceAdapter(currentTestbedConfiguration.getSnaaEndpointUrl());
        snaaServiceAdapter.addAuthenticationTriple(username, urn, password);
        snaaServiceAdapter.authenticate();
    }
}
