/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.ui;

import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 *
 * @author soenke
 */
public class XmlView extends VerticalLayout {

    static TextField txtXml;

    public XmlView() {
        setSizeFull();
        setMargin(true);
        setSpacing(true);

        txtXml = new TextField();
        txtXml.setRows(50);
        txtXml.setColumns(80);

        addComponent(txtXml);
    }

    public static void setText(String text) {
        txtXml.setValue(text);
    }
}
