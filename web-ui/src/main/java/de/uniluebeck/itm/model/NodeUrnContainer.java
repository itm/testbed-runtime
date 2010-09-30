/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.model;

import com.vaadin.data.util.BeanItemContainer;

import java.io.Serializable;

/**
 * @author Sšnke Nommensen
 */
public class NodeUrnContainer extends BeanItemContainer<NodeUrn> implements Serializable {

    public NodeUrnContainer() throws InstantiationException,
            IllegalAccessException {
        super(NodeUrn.class);
    }
}
