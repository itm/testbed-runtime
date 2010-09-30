/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.ui;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;

/**
 * @author Soenke Nommensen
 */
public class ToolbarView extends HorizontalLayout {

    public ToolbarView() {
        setSizeUndefined();
        setSpacing(true);
        setMargin(true);

        Label lblWelcome = new Label("WISEBED Experimentation Facility");
        lblWelcome.addStyleName(Reindeer.LABEL_H1);

        addComponent(lblWelcome);
    }
}
