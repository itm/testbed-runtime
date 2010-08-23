package de.itm.uniluebeck.tr.wiseml.merger.structures;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

import java.util.Map;

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

    private boolean encrypted;
    private boolean virtual;

    private DataType rssiDataType;
    private Unit rssiUnit;
    private String rssiDefaultValue;

    private Map<String, LinkCapability> capabilities;

}
