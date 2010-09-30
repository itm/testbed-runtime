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
public class ReservationView extends HorizontalLayout {

    private static final String RELOAD_BUTTON_TEXT = "Reload";
    private static final String RESERVE_BUTTON_TEXT = "Reserve";
    private final String CLEAR_ALL_BUTTON_TEXT = "Clear all";
    private static final String NETWORK_NODES_LABEL_TEXT = "Network";
    private static final String RESERVATION_NODES_LABEL_TEXT = "Reservation";
    public static final Object[] NATURAL_COL_ORDER = new Object[]{
            "prefix", "project", "testbed", "node"};
    private static final int TABLE_PAGE_LENGTH = 23;
    /* UI elements */
    final Label lblNetwork;
    final Label lblReservation;
    final Table tblNetwork;
    final Table tblReservation;
    final HorizontalLayout rightButtonBar;
    final Button btnReload;
    final Button btnReserve;
    final Button btnClearAll;
    final VerticalLayout left;
    final VerticalLayout right;

    public ReservationView() {
        setSizeFull();
        addStyleName(Reindeer.LAYOUT_WHITE);

        left = new VerticalLayout();
        left.setSpacing(true);
        left.setMargin(true);

        right = new VerticalLayout();
        right.setSpacing(true);
        right.setMargin(true);

        lblNetwork = new Label(NETWORK_NODES_LABEL_TEXT);
        lblNetwork.addStyleName(Reindeer.LABEL_H2);

        btnReload = new Button(RELOAD_BUTTON_TEXT);
        //btnReload.addStyleName(Reindeer.BUTTON_SMALL);

        tblNetwork = new Table();
        tblNetwork.setWidth(100, Table.UNITS_PERCENTAGE);
        tblNetwork.setSelectable(true);
        tblNetwork.setMultiSelect(true);
        tblNetwork.setPageLength(TABLE_PAGE_LENGTH);
        tblNetwork.setContainerDataSource(null);

        left.addComponent(lblNetwork);
        left.addComponent(tblNetwork);
        left.addComponent(btnReload);

        lblReservation = new Label(RESERVATION_NODES_LABEL_TEXT);
        lblReservation.addStyleName(Reindeer.LABEL_H2);

        rightButtonBar = new HorizontalLayout();
        rightButtonBar.setSpacing(true);

        btnReserve = new Button(RESERVE_BUTTON_TEXT);
        btnClearAll = new Button(CLEAR_ALL_BUTTON_TEXT);

        rightButtonBar.addComponent(btnClearAll);
        rightButtonBar.addComponent(btnReserve);

        tblReservation = new Table();
        tblReservation.setWidth(100, Table.UNITS_PERCENTAGE);
        tblReservation.setSelectable(true);
        tblReservation.setMultiSelect(true);
        tblReservation.setPageLength(TABLE_PAGE_LENGTH);
        tblReservation.setContainerDataSource(null);

        right.addComponent(lblReservation);
        right.addComponent(tblReservation);
        right.addComponent(rightButtonBar);

        addComponent(left);
        addComponent(right);
    }
}
