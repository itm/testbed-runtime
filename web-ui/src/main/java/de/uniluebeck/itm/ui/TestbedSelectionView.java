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

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import de.uniluebeck.itm.model.NodeUrn;
import de.uniluebeck.itm.model.TestbedConfiguration;
import de.uniluebeck.itm.ui.components.UiLoginForm;

/**
 * @author Soenke Nommensen
 */
public class TestbedSelectionView extends VerticalLayout {

    private static final String RELOAD_BUTTON_LABEL = "Reload";
    private static final String TESTBED_SELECTION_LABEL = "Testbed Selection";
    private static final String CONNECT_BUTTON_LABEL = "Connect to Testbed...";
    private final Button btnReload = new Button(RELOAD_BUTTON_LABEL);
    private final Button btnConnect = new Button(CONNECT_BUTTON_LABEL);
    private final ListSelect lstTestbedConfigurations = new ListSelect();
    private final Table tblTestbedInfo = new Table();
    private final LoginForm frmLogin = new UiLoginForm();
    private final Window wdwLogin = new Window();
    private final Panel pnlDetails = new Panel();

    public TestbedSelectionView() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        addStyleName(Reindeer.LAYOUT_WHITE);

        // Initialize the parent layout.
        final VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        final HorizontalLayout innerLayout = new HorizontalLayout();
        innerLayout.setWidth(100, UNITS_PERCENTAGE);
        innerLayout.setSpacing(true);
        innerLayout.setMargin(true);
        initTestbedSelection(innerLayout);
        initTestbedInfos(innerLayout);
        layout.addComponent(innerLayout);

        initLoginWindow(wdwLogin);

        addComponent(layout);
        setExpandRatio(layout, 1);
    }

    private void initTestbedInfos(HorizontalLayout layout) {
        VerticalLayout innerLayout = new VerticalLayout();
        innerLayout.setSpacing(true);

        tblTestbedInfo.setWidth(100, UNITS_PERCENTAGE);
        tblTestbedInfo.setHeight(506, UNITS_PIXELS);

        Label lblTestbedInfo = new Label("Testbed Info");
        lblTestbedInfo.addStyleName(Reindeer.LABEL_H2);
        innerLayout.addComponent(lblTestbedInfo);

        tblTestbedInfo.setSizeFull();
        tblTestbedInfo.setContainerDataSource(new BeanItemContainer<NodeUrn>(NodeUrn.class));
        tblTestbedInfo.setNullSelectionAllowed(false);
        innerLayout.addComponent(tblTestbedInfo);

        innerLayout.addComponent(btnReload);
        innerLayout.setComponentAlignment(btnReload, Alignment.MIDDLE_RIGHT);

        layout.addComponent(innerLayout);
        layout.setExpandRatio(innerLayout, 0.75f);
    }

    private void initTestbedSelection(HorizontalLayout layout) {
        VerticalLayout innerLayout = new VerticalLayout();
        innerLayout.setSpacing(true);

        final Label lblTestbedSelection = new Label(TESTBED_SELECTION_LABEL);
        lblTestbedSelection.addStyleName(Reindeer.LABEL_H2);
        innerLayout.addComponent(lblTestbedSelection);

        lstTestbedConfigurations.setWidth(100, UNITS_PERCENTAGE);
        lstTestbedConfigurations.setHeight(250, UNITS_PIXELS);
        lstTestbedConfigurations.setNullSelectionAllowed(false);
        lstTestbedConfigurations.setImmediate(true);
        lstTestbedConfigurations.setContainerDataSource(new BeanItemContainer<TestbedConfiguration>(TestbedConfiguration.class));
        innerLayout.addComponent(lstTestbedConfigurations);

        pnlDetails.setWidth(100, UNITS_PERCENTAGE);
        pnlDetails.setHeight(250, UNITS_PIXELS);
        innerLayout.addComponent(pnlDetails);

        innerLayout.addComponent(btnConnect);
        innerLayout.setComponentAlignment(btnConnect, Alignment.MIDDLE_RIGHT);
        layout.addComponent(innerLayout);
        layout.setExpandRatio(innerLayout, 0.25f);
    }

    private void initLoginWindow(Window window) {
        window.setModal(true);
        window.setWidth(345, UNITS_PIXELS);
        window.setHeight(190, UNITS_PIXELS);

        VerticalLayout panelLayout = (VerticalLayout) wdwLogin.getContent();

        window.addComponent(frmLogin);
        panelLayout.setComponentAlignment(frmLogin, Alignment.MIDDLE_CENTER);
    }

    public void addTestBed(TestbedConfiguration testbedConfiguration) {
        lstTestbedConfigurations.addItem(testbedConfiguration);
    }

    public Button getReloadButton() {
        return btnReload;
    }

    public void setDeviceContainer(BeanItemContainer<?> container) {
        tblTestbedInfo.setContainerDataSource(container);
    }

    public ListSelect getTestbedConfigurationSelect() {
        return lstTestbedConfigurations;
    }

    public void clear() {
        tblTestbedInfo.removeAllItems();
    }

    public Button getConnectButton() {
        return btnConnect;
    }

    public Window getLoginWindow() {
        return wdwLogin;
    }

    public void setShowLoginWindow(boolean visible) {
        if (visible) {
            getWindow().addWindow(wdwLogin);
            wdwLogin.center();
        } else {
            getWindow().removeWindow(wdwLogin);
        }
    }

    public void setDetailsText(String details) {
        pnlDetails.removeAllComponents();
        pnlDetails.addComponent(new Label(details));
    }

    LoginForm getLoginForm() {
        return frmLogin;
    }
}
