package de.itm.uniluebeck.tr.wiseml.merger.internals;

/**
 * Created by IntelliJ IDEA.
 * User: kuypers
 * Date: 19.08.2010
 * Time: 18:24:51
 * To change this template use File | Settings | File Templates.
 */
public enum ReadingStage {

    Head,
    SetupProperties,
    SetupDefaultNodes,
    SetupDefaultLinks,
    SetupNodes,
    SetupLinks,
    Scenario,
    Trace,
    Tail,

}
