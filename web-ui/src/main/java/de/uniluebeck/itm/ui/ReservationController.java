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

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.TargetDetails;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.Window;
import de.uniluebeck.itm.common.UiUtil;
import de.uniluebeck.itm.model.NodeUrnContainer;
import de.uniluebeck.itm.ws.SessionManagementAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Soenke Nommensen
 */
public class ReservationController implements Controller {

    private final ReservationView view;
    private final Set<Object> markedRows = new HashSet<Object>();

    public ReservationController() {
        view = new ReservationView();

        view.tblNetwork.setContainerDataSource(initEmptyNodeUrnContainer());
        view.tblNetwork.setDragMode(Table.TableDragMode.MULTIROW);
        view.tblNetwork.setDropHandler(createTableDropHandler());
        view.tblNetwork.addListener(createTableValueChangeListener());

        view.tblReservation.setContainerDataSource(
                initEmptyNodeUrnContainer());
        view.tblReservation.setDragMode(Table.TableDragMode.MULTIROW);
        view.tblReservation.setDropHandler(createTableDropHandler());
        view.tblReservation.addListener(createTableValueChangeListener());

        view.btnReload.addListener(createReloadButtonListener());
        view.btnClearAll.addListener(createClearAllButtonListener());
    }

    /**
     * @return Reference to the view
     */
    public ReservationView view() {
        return view;
    }

    private ClickListener createReloadButtonListener() {
        return new Button.ClickListener() {

            public void buttonClick(Button.ClickEvent event) {
                SessionManagementAdapter sessionManagementAdapter = new SessionManagementAdapter();
                NodeUrnContainer container;
                try {
                    container = sessionManagementAdapter.getNetworkAsContainer();
                    view.tblNetwork.setContainerDataSource(container);
                } catch (InstantiationException ex) {
                    UiUtil.showNotification(
                            UiUtil.createNotificationCenteredTop(
                                    "Instantiation error", "<br/>" + ex.getMessage(),
                                    Window.Notification.TYPE_WARNING_MESSAGE));
                } catch (IllegalAccessException ex) {
                    UiUtil.showNotification(
                            UiUtil.createNotificationCenteredTop(
                                    "Illegal access error", "<br/>" + ex.getMessage(),
                                    Window.Notification.TYPE_ERROR_MESSAGE));
                }
            }
        };
    }

    private ClickListener createClearAllButtonListener() {
        return new Button.ClickListener() {

            public void buttonClick(ClickEvent event) {
                view.tblReservation.getContainerDataSource().removeAllItems();
            }
        };
    }

    private DropHandler createTableDropHandler() {
        return new DropHandler() {

            public void drop(DragAndDropEvent event) {
                TableTransferable transferable = (TableTransferable) event.getTransferable();
                TargetDetails targetDetails = event.getTargetDetails();

                if (!(transferable.getSourceContainer() instanceof NodeUrnContainer)) {
                    return;
                }

                if (markedRows.isEmpty()) {
                    ((Table) targetDetails.getTarget()).getContainerDataSource().
                            addItem(transferable.getItemId());
                    transferable.getSourceContainer().removeItem(
                            transferable.getItemId());
                } else {
                    for (Object markedRow : markedRows) {
                        ((Table) targetDetails.getTarget()).getContainerDataSource().addItem(markedRow);
                        transferable.getSourceContainer().removeItem(markedRow);
                    }
                    markedRows.clear();
                }
            }

            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }
        };
    }

    private Table.ValueChangeListener createTableValueChangeListener() {
        return new Table.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                /*
                 * In multiselect mode, a Set of itemIds is returned,
                 * in singleselect mode the itemId is returned directly
                 */
                Set<?> value = (Set<?>) event.getProperty().getValue();
                if (null != value && !value.isEmpty()) {
                    markedRows.clear();
                    for (Object o : value) {
                        markedRows.add(o);
                    }
                }
            }
        };
    }

    private NodeUrnContainer initEmptyNodeUrnContainer() {
        NodeUrnContainer container = null;
        try {
            container = new NodeUrnContainer();
        } catch (InstantiationException ex) {
            UiUtil.showNotification(
                    UiUtil.createNotificationCenteredTop(
                            "Instantiation error", "<br/>" + ex.getMessage(),
                            Window.Notification.TYPE_WARNING_MESSAGE));
        } catch (IllegalAccessException ex) {
            UiUtil.showNotification(
                    UiUtil.createNotificationCenteredTop(
                            "Illegal access error", "<br/>" + ex.getMessage(),
                            Window.Notification.TYPE_ERROR_MESSAGE));
        }
        return container;
    }
}
