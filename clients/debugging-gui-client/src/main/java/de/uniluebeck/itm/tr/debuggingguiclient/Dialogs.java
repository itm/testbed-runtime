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

package de.uniluebeck.itm.tr.debuggingguiclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBContext;
import javax.xml.datatype.DatatypeConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.wsn.FlashPrograms;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;
import eu.wisebed.api.wsn.Send;
import eu.wisebed.ns.wiseml._1.Setup;
import eu.wisebed.ns.wiseml._1.Wiseml;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;


public class Dialogs {

    public static Set<String> showNodeUrnSelectionDialog() {
        Dialogs.SelectNodeURNsPanel selectNodeURNsPanel = new Dialogs.SelectNodeURNsPanel();
        Dialogs.InputDialog<Set<String>> dialog = new Dialogs.InputDialog<Set<String>>(
                "Select Node URNs",
                selectNodeURNsPanel
        );
        dialog.setVisible(true);
        return dialog.getResult();
    }

    public static void showTextDialog(final String text, boolean tidyXml) {
        String displayText = text;
        if (tidyXml) {
            try {
                JAXBContext jc = JAXBContext.newInstance(Wiseml.class);
                Wiseml wiseml = (Wiseml) jc.createUnmarshaller().unmarshal(new StringReader(text));
                displayText = StringUtils.jaxbMarshal(wiseml);
            } catch (Exception e) {
                // silently catch and display as normal string
            }
        }
        JTextArea textArea = new JTextArea(displayText);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        //textArea.setPreferredSize(new Dimension(800, 600));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        JOptionPane.showMessageDialog(null, scrollPane);
    }

    public static class InputDialog<T> extends JDialog {

        private AbstractResultPanel<T> contentPane;

        private JPanel panel;

        private JButton okButton;

        private JButton cancelButton;

        private T result = null;

        private ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == okButton) {
                    result = contentPane.getResult();
                    setVisible(false);
                } else if (e.getSource() == cancelButton) {
                    result = null;
                    setVisible(false);
                }
            }
        };

        public InputDialog(String title, AbstractResultPanel<T> contentPane) {

            super((Window) null, title);
            setModal(true);
            setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

            this.contentPane = contentPane;
            panel = new JPanel(new GridBagLayout());

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 3;
            panel.add(contentPane, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(new JLabel(), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(actionListener);
            panel.add(cancelButton, c);

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 1;
            okButton = new JButton("OK");
            okButton.addActionListener(actionListener);
            panel.add(okButton, c);

            setContentPane(panel);

        }

        public T getResult() {
            return result;
        }

        @Override
        public void setVisible(boolean b) {
            if (b) {
                pack();
            }
            super.setVisible(b);
        }
    }

    public static abstract class AbstractResultPanel<T> extends JPanel {

        protected AbstractResultPanel(LayoutManager layout) {
            super(layout);
        }

        public abstract T getResult();

    }

    public static class SelectNodeURNsPanel extends AbstractResultPanel<Set<String>> {

        private JTable table;

        private JTextField sessionManagementEndpointURLTextField;

        private MyTableModel tableModel;

        private static class MyTableModel extends AbstractTableModel {

            private String[] tableColumns = new String[]{"urn", "type"};

            private Object[][] tableData;

            private MyTableModel(final Object[][] tableData) {
                this.tableData = tableData;
            }

            @Override
            public String getColumnName(final int column) {
                return tableColumns[column];
            }

            @Override
            public int findColumn(final String columnName) {
                return "urn".equals(columnName) ? 0 : 1;
            }

            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                return String.class;
            }

            @Override
            public int getRowCount() {
                return tableData.length;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(final int rowIndex, final int columnIndex) {
                return tableData[rowIndex][columnIndex];
            }

            public void setTableData(final Object[][] tableData) {
                this.tableData = tableData;
                fireTableDataChanged();
            }

        }

        public SelectNodeURNsPanel() {

            super(new FlowLayout());

            JPanel panel = new JPanel(new GridBagLayout());
            add(panel);

            GridBagConstraints c;

            tableModel = new MyTableModel(new Object[0][]);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            panel.add(new JLabel("Session Management Endpoint URL"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            sessionManagementEndpointURLTextField = new JTextField();
            sessionManagementEndpointURLTextField.setColumns(40);
            panel.add(sessionManagementEndpointURLTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            panel.add(new JLabel(), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            JButton refreshTableButton = new JButton("Refresh table");
            refreshTableButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {

                    try {

                        String endpointUrl = sessionManagementEndpointURLTextField.getText();
                        SessionManagement smService = WSNServiceHelper.getSessionManagementService(endpointUrl);
                        setTableData(smService.getNetwork());

                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(null, e1.getMessage());
                    }

                }
            }
            );
            panel.add(refreshTableButton, c);

            table = new JTable(tableModel);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            JScrollPane scrollPane = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            panel.add(scrollPane, c);

            TableColumn column;
            for (int i = 0; i < table.getColumnCount(); i++) {
                column = table.getColumnModel().getColumn(i);
                if (i == 0) {
                    column.setPreferredWidth(100);
                } else {
                    column.setPreferredWidth(50);
                }
            }

        }

        private void setTableData(String wisemlString) throws Exception {

            JAXBContext jc = JAXBContext.newInstance(Wiseml.class);
            Wiseml wiseml = (Wiseml) jc.createUnmarshaller().unmarshal(new StringReader(wisemlString));

            List<Setup.Node> nodes = wiseml.getSetup().getNode();
            Object[][] tableData = new Object[nodes.size()][];

            int i = 0;
            for (Setup.Node node : nodes) {
                tableData[i] = new Object[2];
                tableData[i][0] = node.getId();
                tableData[i][1] = node.getNodeType();
                i++;
            }

            tableModel.setTableData(tableData);

        }

        @Override
        public Set<String> getResult() {
            int[] selectedRows = table.getSelectedRows();
            Set<String> selectedNodeUrns = new HashSet<String>(selectedRows.length);
            for (int selectedRow : selectedRows) {
                selectedNodeUrns.add((String) tableModel.getValueAt(selectedRow, 0));
            }
            return selectedNodeUrns;
        }

    }

    public static class FlashProgramsPanel extends AbstractResultPanel<FlashPrograms> {

        private FieldHelper.StringListJTextField programIndicesTextField;

        private FieldHelper.StringListJTextField nodeUrnsTextField;

        private JTabbedPane programsTabs;

        private JButton addButton;

        private JButton removeButton;

        private ActionListener addRemoveActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == addButton) {
                    int index = programsTabs.getTabCount();
                    programsTabs.add("Program " + index, new ProgramPanel());
                } else if (e.getSource() == removeButton) {
                    programsTabs.remove(programsTabs.getTabCount() - 1);
                }
            }
        };

        public FlashProgramsPanel() {

            super(new GridBagLayout());

            // nodeUrns: List<String>
            // programIndices: List<Integer>
            // programs: List<Program>

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Node URNs"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            nodeUrnsTextField = new FieldHelper.StringListJTextField();
            add(nodeUrnsTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("Program indices"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            programIndicesTextField = new FieldHelper.StringListJTextField("0");
            add(programIndicesTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            add(new JLabel(), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            JPanel addRemovePanel = new JPanel(new FlowLayout());
            addButton = new JButton("+");
            addButton.addActionListener(addRemoveActionListener);
            removeButton = new JButton("-");
            removeButton.addActionListener(addRemoveActionListener);
            addRemovePanel.add(addButton);
            addRemovePanel.add(removeButton);
            add(addRemovePanel, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.BOTH;
            programsTabs = new JTabbedPane();
            programsTabs.addTab("Program " + 0, new ProgramPanel());
            add(programsTabs, c);

        }

        @Override
        public FlashPrograms getResult() {
            FlashPrograms flashPrograms = new FlashPrograms();
            flashPrograms.getNodeIds().addAll(nodeUrnsTextField.getValue());
            List<Program> programs = new ArrayList<Program>();
            for (int i = 0; i < programsTabs.getTabCount(); i++) {
                ProgramPanel programPanel = (ProgramPanel) programsTabs.getComponentAt(i);
                programs.add(programPanel.getResult());
            }
            flashPrograms.getPrograms().addAll(programs);
            List<String> indices = programIndicesTextField.getValue();
            for (String index : indices) {
                flashPrograms.getProgramIndices().add(Integer.parseInt(index));
            }
            return flashPrograms;
        }

    }

    public static class ProgramPanel extends AbstractResultPanel<Program> {

        private static final Logger log = LoggerFactory.getLogger(ProgramPanel.class);

        private JFileChooser programFileChooser;

        private ProgramMetaDataPanel programMetaDataPanel;

        public ProgramPanel() {

            super(new GridBagLayout());

            // program: byte[]
            // metaData: ProgramMetaDataPanel

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            programFileChooser = new JFileChooser();
            add(programFileChooser, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            programMetaDataPanel = new ProgramMetaDataPanel();
            add(programMetaDataPanel, c);

        }

        @Override
        public Program getResult() {

            Program program = new Program();
            File programFile = programFileChooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(programFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream dis = new DataInputStream(bis);

                long length = programFile.length();
                byte[] binaryData = new byte[(int) length];
                dis.readFully(binaryData);

                program.setProgram(binaryData);
                program.setMetaData(programMetaDataPanel.getResult());

                return program;


            } catch (FileNotFoundException e) {
                log.error("FileNotFoundException while reading file {}", programFile.getAbsolutePath(), e);
            } catch (IOException e) {
                log.error("IOException while reading file {}", programFile.getAbsolutePath(), e);
            }

            return null;

        }

    }

    public static class ProgramMetaDataPanel extends AbstractResultPanel<ProgramMetaData> {

        private JTextField versionTextField;

        private JTextField nameTextField;

        private JTextField platformTextField;

        private JTextField otherTextField;

        public ProgramMetaDataPanel() {

            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "ProgramMetaData"));

            // version: String
            // name: String
            // platform: String
            // other: String

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Version"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            versionTextField = new JTextField();
            add(versionTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("Name"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            nameTextField = new JTextField();
            Dimension preferredSize = nameTextField.getPreferredSize();
            nameTextField.setPreferredSize(new Dimension(400, (int) preferredSize.getHeight()));
            add(nameTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            add(new JLabel("Platform"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            platformTextField = new JTextField();
            add(platformTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 3;
            add(new JLabel("Other"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            otherTextField = new JTextField();
            add(otherTextField, c);

        }

        @Override
        public ProgramMetaData getResult() {
            ProgramMetaData programMetaData = new ProgramMetaData();
            programMetaData.setName(nameTextField.getText());
            programMetaData.setOther(otherTextField.getText());
            programMetaData.setPlatform(platformTextField.getText());
            programMetaData.setVersion(versionTextField.getText());
            return programMetaData;
        }

    }

    public static class RequestStatusPanel extends AbstractResultPanel<RequestStatus> {

        private static final Logger log = LoggerFactory.getLogger(Dialogs.RequestStatusPanel.class);

        private JTextArea statusListTextArea;

        private JTextField requestIdTextField;

        public RequestStatusPanel() {

            super(new GridBagLayout());

            // nodeIds: List<String>
            // message: Message

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Request ID"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.requestIdTextField = new JTextField();
            add(requestIdTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("One Status per line,\n denoted by \"nodeId,value,msg\""), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            this.statusListTextArea = new JTextArea(10, 50);
            add(statusListTextArea, c);

        }

        @Override
        public RequestStatus getResult() {
            BufferedReader reader = new BufferedReader(new StringReader(statusListTextArea.getText()));
            String line;
            List<Status> statusList = new ArrayList<Status>();
            try {
                while ((line = reader.readLine()) != null) {
                    String[] args = line.split(",");
                    Status status = new Status();
                    status.setNodeId(args[0]);
                    status.setValue(Integer.parseInt(args[1]));
                    status.setMsg(args[2]);
                    statusList.add(status);
                }
            } catch (Exception e) {
                log.error("IOException while reading input {}", e);
                return null;
            }
            RequestStatus requestStatus = new RequestStatus();
            requestStatus.setRequestId(requestIdTextField.getText());
            requestStatus.getStatus().addAll(statusList);
            return requestStatus;
        }

    }

    public static class SendMessagePanel extends AbstractResultPanel<Send> {

        private FieldHelper.StringListJTextField nodeUrnsTextField;

        private MessagePanel messagePanel;

        public SendMessagePanel() {

            super(new GridBagLayout());

            // nodeIds: List<String>
            // message: Message

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Node URNs"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            nodeUrnsTextField = new FieldHelper.StringListJTextField();
            add(nodeUrnsTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            messagePanel = new MessagePanel();
            add(messagePanel, c);

        }

        @Override
        public Send getResult() {
            Send send = new Send();
            send.setMessage(messagePanel.getResult());
            send.getNodeIds().addAll(nodeUrnsTextField.getValue());
            return send;
        }

    }

    public static class MessagePanel extends AbstractResultPanel<Message> {

        private JTextField sourceNodeUrnTextField;

        private JTabbedPane tabs;

        private FieldHelper.XMLGregorianCalendarDateChooserPanel timestampTextField;

        private BinaryMessagePanel binaryMessagePanel;

        public MessagePanel() {

            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Message"));

            // sourceNodeId: String
            // timestamp: XMLGregorianCalendar
            // textmessage: TextMessage
            // binaryMessage: BinaryMessage

            GridBagConstraints c;
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Source node URN"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            sourceNodeUrnTextField = new JTextField();
            add(sourceNodeUrnTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("Timestamp"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            timestampTextField = new FieldHelper.XMLGregorianCalendarDateChooserPanel();
            add(timestampTextField, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            tabs = new JTabbedPane();
            tabs.setPreferredSize(new Dimension(800, 300));
            add(tabs, c);

            binaryMessagePanel = new BinaryMessagePanel();
            tabs.add("BinaryMessage", binaryMessagePanel);

        }

        @Override
        public Message getResult() {

            Message msg = new Message();
            msg.setSourceNodeId(sourceNodeUrnTextField.getText());
            try {
                msg.setTimestamp(timestampTextField.getValue());
            } catch (DatatypeConfigurationException e) {
                //TODO display some error on the GUI
            }

            msg.setBinaryData(binaryMessagePanel.getResult());

            return msg;

        }

    }

    public static class BinaryMessagePanel extends AbstractResultPanel<byte[]> {

        private FieldHelper.ByteArrayJTextArea byteArrayTextArea;

        private FieldHelper.ByteJTextField byteJTextField;

        public BinaryMessagePanel() {

            super(new GridBagLayout());

            // binaryData: byte[]
            // binaryType: Byte

            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Binary data (HEX)"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            byteArrayTextArea = new FieldHelper.ByteArrayJTextArea();
            Dimension preferredSize1 = byteArrayTextArea.getPreferredSize();
            byteArrayTextArea.setPreferredSize(new Dimension(400, 200));
            add(byteArrayTextArea, c);

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            add(new JLabel("Binary type (HEX)"), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            byteJTextField = new FieldHelper.ByteJTextField();
            Dimension preferredSize = byteJTextField.getPreferredSize();
            byteJTextField.setPreferredSize(new Dimension(50, (int) preferredSize.getHeight()));
            add(byteJTextField, c);

        }

        @Override
        public byte[] getResult() {
			return byteArrayTextArea.getValue(16);
        }

    }

    public static class StringListPanel extends AbstractResultPanel<List<String>> {

        private FieldHelper.StringListJTextField urnListTextField;

        public StringListPanel() {

            super(new GridLayout(1, 2));

            {
                add(new JLabel("Node URNs (comma separated)"));

                urnListTextField = new FieldHelper.StringListJTextField();
                Dimension preferredSize = urnListTextField.getPreferredSize();
                urnListTextField.setPreferredSize(new Dimension(400, (int) preferredSize.getHeight()));
                add(urnListTextField);
            }

        }

        @Override
        public List<String> getResult() {
            return urnListTextField.getValue();
        }

    }

}
