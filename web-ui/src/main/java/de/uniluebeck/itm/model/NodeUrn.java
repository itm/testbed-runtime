/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.model;

import java.io.Serializable;

/**
 * @author Sšnke Nommensen
 */
public class NodeUrn implements Serializable {

    private String _prefix;
    private String _project;
    private String _testbed;
    private String _node;

    public NodeUrn(String prefix, String project, String testbed, String node) {
        this._prefix = prefix;
        this._project = project;
        this._testbed = testbed;
        this._node = node;
    }

    @Override
    public String toString() {
        return getPrefix() + ":" + getProject() + ":" + getTestbed() + ":" + getNode() + ":";
    }

    public String getPrefix() {
        return _prefix;
    }

    public void setPrefix(String prefix) {
        this._prefix = prefix;
    }

    public String getProject() {
        return _project;
    }

    public void setProject(String project) {
        this._project = project;
    }

    public String getTestbed() {
        return _testbed;
    }

    public void setTestbed(String testbed) {
        this._testbed = testbed;
    }

    public String getNode() {
        return _node;
    }

    public void setNode(String node) {
        this._node = node;
    }

}
