/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
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

package de.uniluebeck.itm.tr.debuggingguiclient.wsn;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class WSNClientView extends JPanel {

    private JTextField endpointUrlTextField;

    private JButton areNodesAliveButton;

    private JButton defineNetworkButton;

    private JButton describeCapabilitiesButton;

    private JButton destroyVirtualLinkButton;

    private JButton disableNodeButton;

    private JButton disablePhysicalLinkButton;

    private JButton enableNodeButton;

    private JButton enablePhysicalLinkButton;

    private JButton flashProgramsButton;

    private JButton getFiltersButton;

    private JButton getNeighborhoodButton;

    private JButton getNetworkButton;

    private JButton getPropertyValueOfButton;

    private JButton getVersionButton;

    private JButton resetNodesButton;

    private JButton sendButton;

    private JButton setStartTimeButton;

    private JButton setVirtualLinkButton;

    public WSNClientView() {

        super(new FlowLayout());
        ((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

        JPanel panel = new JPanel(new GridLayout(11, 2));

        {
            panel.add(new JLabel("WSN API Endpoint URL"));
            endpointUrlTextField = new JTextField();
            panel.add(endpointUrlTextField);
        }
        {
            // checkAreNodesAlive
            areNodesAliveButton = new JButton("checkAreNodesAlive()");
            panel.add(areNodesAliveButton);
        }
        {
            // defineNetwork
            defineNetworkButton = new JButton("defineNetwork()");
            defineNetworkButton.setEnabled(false);
            panel.add(defineNetworkButton);
        }
        {
            // describeCapabilitiesButton
            describeCapabilitiesButton = new JButton("describeCapabilities()");
            describeCapabilitiesButton.setEnabled(false);
            panel.add(describeCapabilitiesButton);
        }
        {
            // destroyVirtualLinkButton
            destroyVirtualLinkButton = new JButton("destroyVirtualLink()");
            destroyVirtualLinkButton.setEnabled(false);
            panel.add(destroyVirtualLinkButton);
        }
        {
            // disableNodeButton
            disableNodeButton = new JButton("disableNode()");
            disableNodeButton.setEnabled(false);
            panel.add(disableNodeButton);
        }
        {
            // disablePhysicalLinkButton
            disablePhysicalLinkButton = new JButton("disablePhysicalLink()");
            disablePhysicalLinkButton.setEnabled(false);
            panel.add(disablePhysicalLinkButton);
        }
        {
            // enableNode
            enableNodeButton = new JButton("enableNode()");
            enableNodeButton.setEnabled(false);
            panel.add(enableNodeButton);
        }
        {
            // enablePhysicalLink
            enablePhysicalLinkButton = new JButton("enablePhysicalLink()");
            enablePhysicalLinkButton.setEnabled(false);
            panel.add(enablePhysicalLinkButton);
        }
        {
            // flashProgramsButton
            flashProgramsButton = new JButton("flashPrograms()");
            panel.add(flashProgramsButton);
        }
        {
            // getFilters
            getFiltersButton = new JButton("getFilters()");
            getFiltersButton.setEnabled(false);
            panel.add(getFiltersButton);
        }
        {
            // getNeighborhoodButton
            getNeighborhoodButton = new JButton("getNeighborhood()");
            getNeighborhoodButton.setEnabled(false);
            panel.add(getNeighborhoodButton);
        }
        {
            // getNetwork
            getNetworkButton = new JButton("getNetwork()");
            panel.add(getNetworkButton);
        }
        {
            // getPropertyValueOf
            getPropertyValueOfButton = new JButton("getPropertyValueOf()");
            getPropertyValueOfButton.setEnabled(false);
            panel.add(getPropertyValueOfButton);
        }
        {
            // getVersion
            getVersionButton = new JButton("getVersion()");
            panel.add(getVersionButton);
        }
        {
            // resetNodes
            resetNodesButton = new JButton("resetNodes()");
            panel.add(resetNodesButton);
        }
        {
            // send
            sendButton = new JButton("send()");
            panel.add(sendButton);
        }
        {
            // setStartTime
            setStartTimeButton = new JButton("setStartTime()");
            setStartTimeButton.setEnabled(false);
            panel.add(setStartTimeButton);
        }
        {
            // setVirtualLink
            setVirtualLinkButton = new JButton("setVirtualLink()");
            setVirtualLinkButton.setEnabled(false);
            panel.add(setVirtualLinkButton);
        }

        add(panel);

    }

    public JButton getGetVersionButton() {
        return getVersionButton;
    }

    public JTextField getEndpointUrlTextField() {
        return endpointUrlTextField;
    }

    public JButton getAreNodesAliveButton() {
        return areNodesAliveButton;
    }

    public JButton getDefineNetworkButton() {
        return defineNetworkButton;
    }

    public JButton getDescribeCapabilitiesButton() {
        return describeCapabilitiesButton;
    }

    public JButton getDestroyVirtualLinkButton() {
        return destroyVirtualLinkButton;
    }

    public JButton getDisableNodeButton() {
        return disableNodeButton;
    }

    public JButton getDisablePhysicalLinkButton() {
        return disablePhysicalLinkButton;
    }

    public JButton getEnableNodeButton() {
        return enableNodeButton;
    }

    public JButton getEnablePhysicalLinkButton() {
        return enablePhysicalLinkButton;
    }

    public JButton getFlashProgramsButton() {
        return flashProgramsButton;
    }

    public JButton getGetFiltersButton() {
        return getFiltersButton;
    }

    public JButton getGetNeighborhoodButton() {
        return getNeighborhoodButton;
    }

    public JButton getGetNetworkButton() {
        return getNetworkButton;
    }

    public JButton getGetPropertyValueOfButton() {
        return getPropertyValueOfButton;
    }

    public JButton getResetNodesButton() {
        return resetNodesButton;
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public JButton getSetStartTimeButton() {
        return setStartTimeButton;
    }

    public JButton getSetVirtualLinkButton() {
        return setVirtualLinkButton;
    }
}
