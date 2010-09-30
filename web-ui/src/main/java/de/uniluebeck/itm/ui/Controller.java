/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.ui;

import com.vaadin.ui.AbstractComponent;

import java.io.Serializable;

/**
 * @author Sšnke Nommensen
 */
public interface Controller extends Serializable {
    public AbstractComponent view();
}
