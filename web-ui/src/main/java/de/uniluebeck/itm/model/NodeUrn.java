/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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
package de.uniluebeck.itm.model;

import java.io.Serializable;

/**
 * @author Soenke Nommensen
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
