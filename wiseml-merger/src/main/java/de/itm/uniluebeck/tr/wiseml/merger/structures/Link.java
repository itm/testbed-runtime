package de.itm.uniluebeck.tr.wiseml.merger.structures;

import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

/**
 * Created by IntelliJ IDEA.
 * User: jacob
 * Date: Aug 23, 2010
 * Time: 7:03:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Link {

    private String source;
    private String target;

    private Boolean encrypted;
    private Boolean virtual;

    private DataType rssiDataType;
    private Unit rssiUnit;
    private String rssiDefaultValue;

    private List<LinkCapability> capabilities;

    

}
