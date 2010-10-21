/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.ui;

import com.vaadin.ui.AbstractComponent;
import de.uniluebeck.itm.common.UiUtil;
import de.uniluebeck.itm.common.XmlFormatter;
import de.uniluebeck.itm.services.SessionManagementAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author soenke
 */
public class XmlController implements Controller {

    final XmlView view;

    public XmlController() {
        view = new XmlView();

        SessionManagementAdapter sessionManagementAdapter = new SessionManagementAdapter("http://wisebed.itm.uni-luebeck.de:8888/sessions?wsdl");
        String text = "";
        try {
            text = sessionManagementAdapter.getNetworkAsString();
        } catch (InstantiationException ex) {
            UiUtil.showExceptionNotification(ex);
        } catch (IllegalAccessException ex) {
            UiUtil.showExceptionNotification(ex);
        }

        setText(new XmlFormatter().format(text));
    }

    public AbstractComponent view() {
        return view;
    }

    public static void setText(String text) {
        XmlView.setText(text);
    }

}
