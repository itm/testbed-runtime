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

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

/**
 * @author Soenke Nommensen
 */
public class UiView extends Window {

    private static final String AUTHENTICATION_TAB_LABEL = "Authentication";
    private static final String RESERVATION_TAB_LABEL = "Reservation";
    final VerticalLayout screen;
    final HorizontalLayout toolbar;
    final TabView tabs;
    final VerticalLayout authentication;
    final HorizontalLayout reservation;

    public UiView(AbstractComponent toolbar, AbstractComponent tabs,
            AbstractComponent authentication, AbstractComponent reservation) {
        super("WISEBED Web UI");

        screen = new VerticalLayout();
        screen.setSizeFull();
        screen.setSpacing(true);
        screen.setMargin(true);
        screen.addStyleName(Reindeer.LAYOUT_BLUE);

        setContent(screen);

        /* Init sub-views */
        this.toolbar = (HorizontalLayout) toolbar;
        this.tabs = (TabView) tabs;
        this.authentication = (VerticalLayout) authentication;
        this.reservation = (HorizontalLayout) reservation;

        this.tabs.addTab(this.authentication, AUTHENTICATION_TAB_LABEL, null);
        this.tabs.addTab(this.reservation, RESERVATION_TAB_LABEL, null);

        screen.addComponent(this.toolbar);
        screen.addComponent(this.tabs);
        screen.setExpandRatio(this.tabs, 1);
    }

}
