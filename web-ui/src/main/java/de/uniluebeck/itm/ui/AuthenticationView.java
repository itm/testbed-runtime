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

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

import de.uniluebeck.itm.model.NodeUrn;
import de.uniluebeck.itm.model.Testbed;

/**
 * @author Soenke Nommensen
 */
public class AuthenticationView extends VerticalLayout {

    private static final String COMMON_FIELD_WIDTH = "20em";

    private class UserFieldFactory extends DefaultFieldFactory {

        @Override
        public Field createField(Item item, Object propertyId, Component uiContext) {
            Field f = super.createField(item, propertyId, uiContext);
            if ("username".equals(propertyId)) {
                TextField tf = (TextField) f;
                tf.setRequired(true);
                tf.setRequiredError("Please enter a First Name");
                tf.setWidth(COMMON_FIELD_WIDTH);
                tf.addValidator(new StringLengthValidator(
                        "Username must be 3-25 characters", 3, 25, false));
            } else if ("password".equals(propertyId)) {
                TextField tf = (TextField) f;
                tf.setSecret(true);
                tf.setRequired(true);
                tf.setRequiredError("Please enter a password");
                tf.setWidth(COMMON_FIELD_WIDTH);
                tf.addValidator(new StringLengthValidator(
                        "Password must be 6-20 characters", 6, 20, false));
            }

            return f;
        }
    }
    private static final String AUTHENTICATION_BUTTON_LABEL = "Login";
    private static final String RELOAD_BUTTON_LABEL = "Reload";
    private static final String AUTHENTICATION_LABEL = "Authentication";
    private static final String CONNECT_BUTTON_LABEL = "Connect to Testbed...";
    /* UI elements */
    private final Button loginButton = new Button(AUTHENTICATION_BUTTON_LABEL);
    private final Button reloadButton = new Button(RELOAD_BUTTON_LABEL);
    private final Button connectButton = new Button(CONNECT_BUTTON_LABEL);
    private final ListSelect testBeds = new ListSelect("Testbeds");
    private final Table devices = new Table("Network");
    private final Form form = new Form();
    private final Window loginWindow = new Window();
    private final Panel detailsPanel = new Panel();

    public AuthenticationView() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        addStyleName(Reindeer.LAYOUT_WHITE);

        // Initialize the parent layout.
        final VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        initHeader(layout);

        final HorizontalLayout innerLayout = new HorizontalLayout();
        innerLayout.setWidth(100, UNITS_PERCENTAGE);
        innerLayout.setSpacing(true);
        initTestbedSelection(innerLayout);
        initDeviceTable(innerLayout);
        layout.addComponent(innerLayout);

        initAuthenticationWindow(loginWindow);
        addComponent(layout);
        setExpandRatio(layout, 1);
    }

    private void initDeviceTable(HorizontalLayout layout) {
        VerticalLayout innerLayout = new VerticalLayout();
        innerLayout.setSpacing(true);

        devices.setWidth(100, UNITS_PERCENTAGE);
        devices.setHeight(506, UNITS_PIXELS);
        devices.setContainerDataSource(new BeanItemContainer<NodeUrn>(NodeUrn.class));
        devices.setNullSelectionAllowed(false);
        innerLayout.addComponent(devices);
        innerLayout.addComponent(reloadButton);
        innerLayout.setComponentAlignment(reloadButton, Alignment.MIDDLE_RIGHT);

        layout.addComponent(innerLayout);
        layout.setExpandRatio(innerLayout, 0.75f);
    }

    private void initHeader(Layout layout) {
        final Label lblLogin = new Label(AUTHENTICATION_LABEL);
        lblLogin.addStyleName(Reindeer.LABEL_H2);
        layout.addComponent(lblLogin);
        layout.addComponent(new Label("Select a Testbed to connect."));
    }

    private void initTestbedSelection(HorizontalLayout layout) {
        VerticalLayout innerLayout = new VerticalLayout();
        innerLayout.setSpacing(true);
        testBeds.setWidth(100, UNITS_PERCENTAGE);
        testBeds.setHeight(250, UNITS_PIXELS);
        testBeds.setNullSelectionAllowed(false);
        testBeds.setImmediate(true);
        testBeds.setContainerDataSource(new BeanItemContainer<Testbed>(Testbed.class));
        innerLayout.addComponent(testBeds);

        detailsPanel.setWidth(100, UNITS_PERCENTAGE);
        detailsPanel.setHeight(250, UNITS_PIXELS);
        innerLayout.addComponent(detailsPanel);

        innerLayout.addComponent(connectButton);
        innerLayout.setComponentAlignment(connectButton, Alignment.MIDDLE_RIGHT);
        layout.addComponent(innerLayout);
        layout.setExpandRatio(innerLayout, 0.25f);
    }

    private void initAuthenticationWindow(Window loginWindow) {
        loginWindow.setModal(true);
        loginWindow.setWidth(365, UNITS_PIXELS);
        loginWindow.setHeight(210, UNITS_PIXELS);
        VerticalLayout panelLayout = (VerticalLayout) loginWindow.getContent();
        panelLayout.setSpacing(true);

        form.setWriteThrough(false);
        form.setInvalidCommitted(false);
        form.setFormFieldFactory(new UserFieldFactory());
        loginWindow.addComponent(form);
        loginWindow.addComponent(loginButton);
        panelLayout.setComponentAlignment(loginButton, Alignment.MIDDLE_RIGHT);
    }

    public Form getForm() {
        return form;
    }

    public void addTestBed(Testbed bed) {
        testBeds.addItem(bed);
    }

    public Button getSubmitButton() {
        return loginButton;
    }

    public Button getReloadButton() {
        return reloadButton;
    }

    public void setDeviceContainer(BeanItemContainer<?> container) {
        devices.setContainerDataSource(container);
    }

    public ListSelect getTestBedSelect() {
        return testBeds;
    }

    public void clear() {
        devices.removeAllItems();
    }

    public Button getConnectButton() {
        return connectButton;
    }

    public Window getLoginWindow() {
        return loginWindow;
    }

    public void setShowLoginWindow(boolean visible) {
        if (visible) {
            getWindow().addWindow(loginWindow);
            loginWindow.center();
        } else {
            getWindow().removeWindow(loginWindow);
        }
    }

    public void setDetailsText(String details) {
        detailsPanel.removeAllComponents();
        detailsPanel.addComponent(new Label(details));
    }
}
