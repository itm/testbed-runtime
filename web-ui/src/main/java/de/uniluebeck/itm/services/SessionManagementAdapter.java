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
package de.uniluebeck.itm.services;

import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.model.NodeUrn;
import de.uniluebeck.itm.model.NodeUrnContainer;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import eu.wisebed.testbed.api.wsn.v211.SessionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Soenke Nommensen
 */
public class SessionManagementAdapter {

    private SessionManagement sessionManagement;
    
    public SessionManagementAdapter(String url) {
    	sessionManagement = WSNServiceHelper.getSessionManagementService(url);
    }

    public List<String> getNetworkAsStringList() {
        return WiseMLHelper.getNodeUrns(sessionManagement.getNetwork());
    }

    public NodeUrnContainer getNetworkAsContainer() throws InstantiationException, IllegalAccessException {
        NodeUrnContainer container = new NodeUrnContainer();
        List<String> nodes = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork()); // getNetwork() liefert WiseML Beschreibung vom Testbed

        for (String s : nodes) {
            String[] n = s.split(":");
            if (n.length == 4) {
                container.addBean(new NodeUrn(n[0], n[1], n[2], n[3]));
            }
        }

        return container;
    }

    public List<NodeUrn> getNetworkAsList() throws InstantiationException, IllegalAccessException {
        List<NodeUrn> list = new ArrayList();
        List<String> nodes = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork());

        for (String s : nodes) {
            String[] n = s.split(":");
            if (n.length == 4) {
                list.add(new NodeUrn(n[0], n[1], n[2], n[3]));
            }
        }

        return list;
    }

    public String getNetworkAsString() throws InstantiationException, IllegalAccessException {
        return sessionManagement.getNetwork();
    }
}
