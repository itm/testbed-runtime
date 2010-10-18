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
import java.util.Arrays;
import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Form;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.uniluebeck.itm.common.UiUtil;
import de.uniluebeck.itm.exception.AuthenticationException;
import de.uniluebeck.itm.model.NodeUrnContainer;
import de.uniluebeck.itm.model.TestBed;
import de.uniluebeck.itm.ws.SNAAServiceAdapter;
import de.uniluebeck.itm.ws.SessionManagementAdapter;

/**
 * @author Soenke Nommensen
 */
public final class AuthenticationController implements Controller {

	public interface Display {
		
		void addTestBed(TestBed bed);
		
		ListSelect getTestBedSelect();
		
		Form getForm();
		
		Button getSubmitButton();
		
		Button getReloadButton();
		
		Button getConnectButton();
		
		Window getLoginWindow();
		
		void setShowLoginWindow(boolean visible);
		
		void setDeviceContainer(BeanItemContainer<?> container);
		
		void clear();
		
		void setDetailsText(String details);
		
		VerticalLayout getControl();
	}
	
	private static final List<TestBed> TESTBEDS = new ArrayList<TestBed>();

	private final Display display;
	
	private TestBed currentTestBed = null;

    static {
    	TestBed bed = new TestBed("Wisebed Fedaration Testbed");
    	bed.setDescription("Wisebed Fedaration Testbed Description");
    	bed.setSessionmanagementUrl("http://wisebed.itm.uni-luebeck.de:8888/sessions?wsdl");
    	bed.setSnaaUrl("http://wisebed.itm.uni-luebeck.de:8890/snaa?wsdl");
    	bed.getUrnPrefixList().add("urn:wisebed:uzl1:");
    	TESTBEDS.add(bed);
    }
	
	public AuthenticationController(final Display display) {
		display.getSubmitButton().addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				onSubmitButtonClick();
			}
		});
		display.getConnectButton().addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				onConnectButtonClick();
			}
		});
		display.getTestBedSelect().addListener(new Property.ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				TestBed bed = (TestBed) event.getProperty().getValue();
				onSelectTestBed(bed);
			}
		});
		display.getReloadButton().addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (currentTestBed != null) {
					onLoadNetwork(currentTestBed.getSessionmanagementUrl());
				}
			}
		});

		this.display = display;
		
		for (TestBed bed : TESTBEDS) {
			display.addTestBed(bed);
		}
		
		display.getConnectButton().setEnabled(false);
		display.getReloadButton().setEnabled(false);
	}
	
	private void onConnectButtonClick() {
		display.getForm().setItemDataSource(new BeanItem<TestBed>(currentTestBed), Arrays.asList(new String[] {"username", "password"}));
		display.getLoginWindow().setCaption("Login to " + currentTestBed.getName());
		display.setShowLoginWindow(true);
	}
	
	private void onSubmitButtonClick() {
		boolean success = true;
		final Form form = display.getForm();
		try {
			form.commit();
		} catch(Exception e) {
			display.getLoginWindow().setHeight(240, Sizeable.UNITS_PIXELS);
			success = false;
		}
		if (success) {
			connectToTestBed(currentTestBed);
		}
	}
	
	private void connectToTestBed(TestBed bed) {
		final String username = bed.getUsername();
		final String password = bed.getPassword();
		final List<String> exceptions = new ArrayList<String>(bed.getUrnPrefixList().size());
		for (final String urn : bed.getUrnPrefixList()) {
			try {
				authenticate(urn, username, password);
			} catch (AuthenticationException e) {
				exceptions.add(urn + " - " + e.getMessage());
			}
		}
		
		if (exceptions.isEmpty()) {
			display.setShowLoginWindow(false);
			UiUtil.showNotification(UiUtil.createNotificationCenteredTop(
					"Authentication successful", "User: \"" + username
				  + "\" authenticated for: \"" + currentTestBed.getName() + "\"",
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
	
	private void onSelectTestBed(TestBed bed) {
		currentTestBed = bed;
		
		String details = bed.getDescription();
		
		display.setDetailsText(details);
		display.getConnectButton().setEnabled(currentTestBed != null);
		onLoadNetwork(bed.getSessionmanagementUrl());
	}
	
	private void onLoadNetwork(String url) {
		display.getReloadButton().setEnabled(false);
		SessionManagementAdapter sessionManagementAdapter = new SessionManagementAdapter(url);
        try {
        	NodeUrnContainer container = sessionManagementAdapter.getNetworkAsContainer();
            display.setDeviceContainer(container);
        } catch (InstantiationException ex) {
        	UiUtil.showExceptionNotification(ex);
        } catch (IllegalAccessException ex) {
        	UiUtil.showExceptionNotification(ex);
        }
        display.getReloadButton().setEnabled(true);
	}

	/**
	 * @return Reference to the view
	 */
	public VerticalLayout view() {
		return display.getControl();
	}

	/**
	 * Starts authentication utilizing the SNAAServiceAdapter.
	 * @throws AuthenticationException 
	 */
	private void authenticate(String urn, String username, String password) throws AuthenticationException {
		SNAAServiceAdapter snaaServiceAdapter = new SNAAServiceAdapter(currentTestBed.getSnaaUrl());
		snaaServiceAdapter.addAuthenticationTriple(username, urn, password);
		snaaServiceAdapter.authenticate();
	}
}
