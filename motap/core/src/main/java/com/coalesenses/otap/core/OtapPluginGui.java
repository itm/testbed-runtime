/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
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

package com.coalesenses.otap.core;



/**
 * @author Dennis Pfisterer
 */
public class OtapPluginGui {

	/**
	 * @author Daniela Krüger
	 */
	/*static class OtapDeviceSorter extends ViewerSorter {

		*//**
		 *
		 *//*
		public static enum Column {

			SELECT, ID, CHIP, STATUS, PROGRESS
		}

		*//**
		 *
		 *//*
		protected Column column;

		*//**
		 * @param column
		 *//*
		public void setColumn(Column column) {
			log.debug("Set Column:" + column);
			this.column = column;
		}

		*//**
		 * @return
		 *//*
		public Column getColumn() {
			log.debug("Get Column:" + column);
			return column;
		}

		*//**
		 * @param viewer
		 * @param obj1
		 * @param obj2
		 *
		 * @return
		 *//*
		public int compare(Viewer viewer, Object obj1, Object obj2) {
			int result = 0;
			TableViewer tableView = (TableViewer) viewer;
			OtapDevice node1 = (OtapDevice) obj1, node2 = (OtapDevice) obj2;
			//log.debug("Sorter:" + node1.getId() + " " + node2.getId());
			if (column == Column.SELECT) {
				TableItem item1 = null, item2 = null;
				for (TableItem item : tableView.getTable().getItems()) {
					if (((OtapDevice) item.getData()).equals(node1)) {
						item1 = item;
					}
					if (((OtapDevice) item.getData()).equals(node2)) {
						item2 = item;
					}
					if (item1 != null && item2 != null) {
						result = ((Boolean) item1.getChecked()).compareTo(((Boolean) item2.getChecked()));
						break;
					}
				}
			} else if (column == Column.ID) {
				result = ((Integer) node1.getId()).compareTo((Integer) node2.getId());
			} else if (column == Column.CHIP) {
				result = ((String) node1.getChipType().toString()).compareTo((String) node2.getChipType().toString());
			} else if (column == Column.STATUS) {
				result = ((String) node1.getStatusMessage()).compareTo((String) node2.getStatusMessage());
			} else if (column == Column.PROGRESS) {
				result = ((Double) node1.getProgress()).compareTo((Double) node2.getProgress());
			}


			return tableView.getTable().getSortDirection() == SWT.UP ? result : result * -1;
		}
	}

	*//**
	 * @author Daniela Krüger
	 *//*
	interface OtapDeviceContentProviderListener {

		public void inputChanged(OtapDeviceContentProvider provider, OtapDevice object);
	}

	*//**
	 * @author Daniela Krüger
	 *//*
	class OtapDeviceContentProvider implements IStructuredContentProvider {

		protected Set<OtapDeviceContentProviderListener> listeners = new HashSet<OtapDeviceContentProviderListener>();

		protected OtapDevice object;

		protected SortedSet<OtapDevice> nodes = new TreeSet<OtapDevice>();

		*//**
		 * @param listener
		 *//*
		public void addListener(OtapDeviceContentProviderListener listener) {
			listeners.add(listener);
		}

		*//**
		 * @param listener
		 *//*
		public void removeListener(OtapDeviceContentProviderListener listener) {
			listeners.remove(listener);
		}

		*//**
		 *
		 *//*
		public void dispose() {
		}

		*//**
		 * @param d
		 *//*
		public void removeDevice(OtapDevice d) {
			nodes.remove(d);
			int index = -1;
			int cur = 0;
			for (TableItem current : tableView.getTable().getItems()) {
				log.debug("item: " + current.getData() + " remove: " + d + " id: " + d.getId() + "(" + Tools
						.toHexString(d.getId()) + ")"
				);
				if (current.getData() != null && current.getData().equals(d)) {
					index = cur;
					break;
				}
				cur++;
			}
			if (index != -1) {
				tableView.getTable().remove(index);
			}
			macColumn.setText(nodes.size() + " Devices");

		}

		*//**
		 * @param viewer
		 * @param oldInput
		 * @param input
		 *//*
		public void inputChanged(Viewer viewer, Object oldInput, Object input) {

			// Cast to binary object
			object = (OtapDevice) input;

			if (input != null) {
				nodes.add(object);
			} else {
				nodes.clear();
				tableView.getTable().removeAll();
			}
			macColumn.setText(nodes.size() + " Devices");


			//log.debug("Notifying " + listeners.size() + " listeners");
			for (OtapDeviceContentProviderListener listener : listeners) {
				listener.inputChanged(this, object);
			}
		}

		*//**
		 * @param input
		 *
		 * @return
		 *//*
		public Object[] getElements(Object input) {
			return nodes.toArray();
		}

		*//**
		 * @return
		 *//*
		public Set<OtapDevice> getNodes() {
			return nodes;
		}
	}

	*//**
	 * @author Dennis Pfisterer
	 *//*
	class OtapDeviceLabelProvider implements ITableLabelProvider {

		*//**
		 *
		 *//*
		protected Set<ILabelProviderListener> listeners = new HashSet<ILabelProviderListener>();

		*//**
		 *
		 *//*
		public void dispose() {
		}

		*//**
		 * @param obj
		 * @param column
		 *
		 * @return
		 *//*
		public Image getColumnImage(Object obj, int column) {
			return null;
		}

		*//**
		 * @param obj
		 * @param str
		 *
		 * @return
		 *//*
		public boolean isLabelProperty(Object obj, String str) {
			return false;
		}

		*//**
		 * @param obj
		 * @param column
		 *
		 * @return
		 *//*
		public String getColumnText(Object obj, int column) {
			OtapDevice node = (OtapDevice) obj;

			if (column == 1) {
				return (Integer.toHexString(node.getId()));
			} else if (column == 2) {
				return String.valueOf(node.getChipType());
			} else if (column == 3) {
				return String.valueOf(node.getStatusMessage());
			} else if (column == 4) {
				return String.valueOf(Math.round(100 * node.getProgress())) + " %";
			} else {
				return "";
			}
		}

		*//**
		 * @param listener
		 *//*
		public void addListener(ILabelProviderListener listener) {
			listeners.add(listener);
		}

		*//**
		 * @param listener
		 *//*
		public void removeListener(ILabelProviderListener listener) {
			listeners.remove(listener);
		}
	}


	*//**
	 *
	 *//*
	class OpenOtapFileAction extends Action {

		*//**
		 *
		 *//*
		public OpenOtapFileAction() {
			setText("Open file...");
			setToolTipText("Open file");
			setImageDescriptor(IconTheme.lookupDescriptor("document-open"));
		}

		*//**
		 *
		 *//*
		public void run() {
			String oldFilename = Settings.instance().get(SettingsKey.otap_file);

			// Create file chooser dialog
			FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell());

			// Open the .bin file
			dialog.setText("Open .bin file");
			dialog.setFilterExtensions(new String[]{"*.bin"});
			dialog.setFileName(oldFilename);
			String filename = dialog.open();
			if (filename == null) {
				return;
			}

			// Save filename
			Settings.instance().put(SettingsKey.otap_file, filename);

			// Notify listeners
			firePropertyChange(SettingsKey.otap_file.name(), oldFilename, filename);
		}

		*//**
		 * @return
		 *//*
		public String getFilename() {
			return Settings.instance().get(SettingsKey.otap_file);
		}

		*//**
		 *
		 *//*
		public void openLastUsed() {
			firePropertyChange(SettingsKey.otap_file.name(), null, Settings.instance().get(SettingsKey.otap_file));
		}
	}

	*//**
	 *
	 *//*
	protected static Category log = Logging.get(OtapPluginGui.class);

	*//**
	 *
	 *//*
	private OtapPlugin otapPlugin = null;

	*//**
	 *
	 *//*
	protected CTabItem tabItem = null;

	*//**
	 *
	 *//*
	private Composite mainPanel = null;

	*//** *//*
	private TableViewer tableView;

	protected TableColumn macColumn;

	protected TableColumn chipColumn;

	protected TableColumn statusColumn;

	protected TableColumn progressColumn;

	protected TableColumn selectColumn;

	*//** *//*
	private Button otapControlButton = null;

	private Button motapEnableButton = null;

	*//** *//*
	private Button presenceDetectButton = null;

	private Combo channelSelectionBox;

	private Button selectAllButton = null;

	private Button unselectAllButton = null;


	*//** *//*
	private Button openFilebutton = null;

	private OpenOtapFileAction openOtapFileAction = new OpenOtapFileAction();

	*//** *//*
	private CLabel filenameLabel;

	*//** *//*
	private Label statusLabel;

	private Label maxRetriesLabel;

	private Label timeoutMultiplierLabel;

	private Scale timeoutMultiplierScale;

	*//** *//*
	private Set<OtapDevice> selectedDevices = new HashSet<OtapDevice>();

	*//** *//*
	private OtapDeviceSorter otapDeviceSorter = new OtapDeviceSorter();

	*//** *//*
	private OtapDeviceLabelProvider labelProvider = new OtapDeviceLabelProvider();

	*//** *//*
	private OtapDeviceContentProvider contentProvider = new OtapDeviceContentProvider();

	private Text radioKeyInput = null;

	private Button radioKeyButton = null;


	private Text otapKeyInput = null;

	private Button otapKeyButton = null;
	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	OtapPluginGui(OtapPlugin op) {
		this.otapPlugin = op;

		tabItem = otapPlugin.getTabItem(otapPlugin.getName());
		tabItem.setImage(IconTheme.lookup("network-wireless"));

		mainPanel = otapPlugin.getTabContainer(true);

		initGui();
		openOtapFileAction.openLastUsed();
		updateState();
	}

	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	void initGui() {
		// Get tool bar manager
		//otapPlugin.addToolBarAction(openOtapFileAction);

		GridLayout layout = new GridLayout(1, false);
		mainPanel.setLayout(layout);

		// Filename group
		{
			Group fileGroup = new Group(mainPanel, SWT.NONE);
			fileGroup.setText("Filename");
			fileGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

			GridLayout groupLayout = new GridLayout(2, false);
			fileGroup.setLayout(groupLayout);

			filenameLabel = new CLabel(fileGroup, SWT.NONE);
			filenameLabel.setLayoutData(new GridData(GridData.FILL_BOTH));

			openFilebutton = new Button(fileGroup, SWT.NONE);
			openFilebutton.setImage(openOtapFileAction.getImageDescriptor().createImage());
			openFilebutton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					openOtapFileAction.run();
					filenameLabel.pack();
				}
			}
			);

			Settings.instance().addSettingsListener(new SettingsAdapter() {
				public void settingChanged(SettingsKey key, String value) {
					otapPlugin.loadBinProgram(value);
					updateState();
				}
			}, SettingsKey.otap_file
			);
		}

		// Status information
		{
			Group statusGroup = new Group(mainPanel, SWT.NONE);
			statusGroup.setText("Status");
			statusGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

			GridLayout groupLayout = new GridLayout(1, false);
			statusGroup.setLayout(groupLayout);

			statusLabel = new Label(statusGroup, SWT.NONE);
			statusLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
		}

		// Progress table
		{
			Group tableGroup = new Group(mainPanel, SWT.NONE);
			tableGroup.setText("Progress");
			tableGroup.setLayoutData(
					new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL)
			);

			GridLayout groupLayout = new GridLayout(2, false);
			tableGroup.setLayout(groupLayout);

			// Table box
			Composite box = new Composite(tableGroup, SWT.NONE);
			box.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.FILL_VERTICAL));
			box.setLayout(new FillLayout());

			tableView = new TableViewer(box, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.CHECK);
			tableView.setContentProvider(contentProvider);
			tableView.setLabelProvider(labelProvider);
			tableView.setSorter(otapDeviceSorter);
			tableView.getTable().setHeaderVisible(true);
			tableView.getTable().setLinesVisible(true);

			tableView.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent arg0) {
					updateState();
				}
			}
			);

			selectColumn = new TableColumn(tableView.getTable(), SWT.NONE);
			selectColumn.setText("Select");
			selectColumn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedDevices = getSelectedDevices();
					int direction = tableView.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
					tableView.getTable().setSortDirection(direction);
					tableView.getTable().setSortColumn(selectColumn);
					otapDeviceSorter.setColumn(OtapDeviceSorter.Column.SELECT);
					tableView.refresh();
					setSelectedDevices(selectedDevices);
				}
			}
			);
			selectColumn.pack();

			macColumn = new TableColumn(tableView.getTable(), SWT.NONE);
			macColumn.setText("MAC Address");
			macColumn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedDevices = getSelectedDevices();
					int direction = tableView.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
					tableView.getTable().setSortDirection(direction);
					otapDeviceSorter.setColumn(OtapDeviceSorter.Column.ID);
					tableView.refresh();
					setSelectedDevices(selectedDevices);
				}
			}
			);
			macColumn.pack();

			chipColumn = new TableColumn(tableView.getTable(), SWT.NONE);
			chipColumn.setText("Chip          ");
			chipColumn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedDevices = getSelectedDevices();
					int direction = tableView.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
					tableView.getTable().setSortDirection(direction);
					tableView.getTable().setSortColumn(chipColumn);
					otapDeviceSorter.setColumn(OtapDeviceSorter.Column.CHIP);
					tableView.refresh();
					setSelectedDevices(selectedDevices);
				}
			}
			);
			chipColumn.pack();

			statusColumn = new TableColumn(tableView.getTable(), SWT.NONE);
			statusColumn.setText("Status             ");
			statusColumn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedDevices = getSelectedDevices();
					int direction = tableView.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
					tableView.getTable().setSortDirection(direction);
					tableView.getTable().setSortColumn(statusColumn);
					otapDeviceSorter.setColumn(OtapDeviceSorter.Column.STATUS);
					tableView.refresh();
					setSelectedDevices(selectedDevices);

				}
			}
			);
			statusColumn.pack();

			progressColumn = new TableColumn(tableView.getTable(), SWT.NONE);
			progressColumn.setText("Progress");
			progressColumn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedDevices = getSelectedDevices();
					int direction = tableView.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
					tableView.getTable().setSortDirection(direction);
					tableView.getTable().setSortColumn(progressColumn);
					otapDeviceSorter.setColumn(OtapDeviceSorter.Column.PROGRESS);
					tableView.refresh();
					setSelectedDevices(selectedDevices);
				}
			}
			);
			progressColumn.pack();


			Composite buttonBox = new Composite(tableGroup, SWT.NONE);
			GridLayout gridLayout = new GridLayout(1, false);
			buttonBox.setLayout(gridLayout);
			GridData buttonBoxLayoutData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			buttonBox.setLayoutData(buttonBoxLayoutData);

			//Toggle multihop support
			{
				motapEnableButton = new Button(buttonBox, SWT.CHECK);
				motapEnableButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				motapEnableButton.setText("Multihop programming");
				motapEnableButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						handleMotapEnableButton();
					}
				}
				);
			}

			// Channel selection push down menue
			{
				Group channelGroup = new Group(buttonBox, SWT.NONE);
				channelGroup.setText("Radio channel: ");
				channelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

				GridLayout channelgroupLayout = new GridLayout(2, false);
				channelGroup.setLayout(channelgroupLayout);

				channelSelectionBox = new Combo(channelGroup, SWT.READ_ONLY);
				Integer selected = Settings.instance().getInt(SettingsKey.otap_channel);
				otapPlugin.setChannel(selected);
				int index = 0, i = 0;
				for (Integer channel : otapPlugin.getChannels()) {
					channelSelectionBox.add(channel.toString());
					if (channel.intValue() == selected.intValue()) {
						index = i;
					}
					i++;
				}
				channelSelectionBox.select(index);
				channelSelectionBox.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						Settings.instance().put(SettingsKey.otap_channel, channelSelectionBox.getText());
						otapPlugin.setChannel(Integer.parseInt(channelSelectionBox.getText().trim()));
					}
				}
				);

			}

			//Toggle presence detect
			{
				presenceDetectButton = new Button(buttonBox, SWT.PUSH);
				presenceDetectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				//updateState();
				presenceDetectButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						handlePresenceDetectControlButton();
					}
				}
				);
			}

			//Select all
			{
				selectAllButton = new Button(buttonBox, SWT.PUSH);
				selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				selectAllButton.setText("Select all");
				selectAllButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						for (TableItem item : tableView.getTable().getItems()) {
							item.setChecked(true);
						}
						updateState();
					}
				}
				);
			}

			//Unselect all
			{
				unselectAllButton = new Button(buttonBox, SWT.PUSH);
				unselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				unselectAllButton.setText("Unselect all");
				unselectAllButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						for (TableItem item : tableView.getTable().getItems()) {
							item.setChecked(false);
						}
						updateState();
					}
				}
				);
			}

			//Retries
			{
				*//*
				maxRetriesScale = new Scale(buttonBox, SWT.HORIZONTAL);
				maxRetriesLabel = new Label(buttonBox, SWT.NONE);

				maxRetriesScale.setSelection(Settings.instance().getInt(Settings.SettingsKey.max_retries));
				maxRetriesScale.setMinimum(1);
				maxRetriesScale.setMaximum(100);

				maxRetriesLabel.setText("Maximum retries: " + String.valueOf(maxRetriesScale.getSelection()));

				maxRetriesScale.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						Settings.instance().putInt(Settings.SettingsKey.max_retries, maxRetriesScale.getSelection());
						maxRetriesLabel.setText("Maximum retries: " + String.valueOf(maxRetriesScale.getSelection()));
						maxRetriesLabel.pack();
						maxRetriesLabel.update();
					}
				});*//*
			}
			//Timeout Multiplier
			{
				*//*
				timeoutMultiplierScale = new Scale(buttonBox, SWT.HORIZONTAL);
				timeoutMultiplierScale.setMaximum(255);
				timeoutMultiplierScale.setMinimum(1);
				timeoutMultiplierScale.setPageIncrement(10);
				timeoutMultiplierScale.setSelection(Settings.instance().getInt(Settings.SettingsKey.timeout_multiplier));

				timeoutMultiplierLabel = new Label(buttonBox, SWT.NONE);
				timeoutMultiplierLabel.setText("Timeout multiplier: " + String.valueOf(timeoutMultiplierScale.getSelection()));

				timeoutMultiplierScale.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						Settings.instance().putInt(Settings.SettingsKey.timeout_multiplier, timeoutMultiplierScale.getSelection());
						timeoutMultiplierLabel.setText("Timeout multiplier: " + String.valueOf(timeoutMultiplierScale.getSelection()));
						timeoutMultiplierLabel.pack();
						timeoutMultiplierLabel.update();
					}
				});
				*//*
			}

			//Start/Stop Button
			{
				otapControlButton = new Button(buttonBox, SWT.PUSH);
				otapControlButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				//updateState();
				otapControlButton.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						handleOtapInitControlButton();
					}
				}
				);
			}

//			// TODO strebel: Remove after testing
//			// Fill Table Button
//			{
//				Button fillTable = new Button(buttonBox, SWT.PUSH);
//				fillTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//				fillTable.setText("Fill Table (test)");
//				fillTable.addListener(SWT.Selection, new Listener() {
//					public void handleEvent(Event event) {
//						handleFillTableButton();
//					}
//				});
//			}


		}

		// ---------------------------------------------------------------------
		// -------  Set the radio default key ----------------------------------
		// ---------------------------------------------------------------------
		Group stdKeyGroup = new Group(mainPanel, SWT.NONE);
		stdKeyGroup.setText("Default Radio Key");
		stdKeyGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		GridLayout groupLayout = new GridLayout(2, false);
		stdKeyGroup.setLayout(groupLayout);

		radioKeyInput = new Text(stdKeyGroup, SWT.BORDER);
		radioKeyInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radioKeyInput.addVerifyListener(new KeyValidator(radioKeyInput, 32));


		radioKeyButton = new Button(stdKeyGroup, SWT.PUSH);
		//radioKeyButton.setLayoutData(new GridData(GridData.NONE));
		radioKeyButton.setText("Set radio key");
		updateState();
		radioKeyButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// TODO we need an appropriate handler here
				handleRadioKeyButton();
			}
		}
		);


		radioKeyInput.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				if (radioKeyInput.getText().length() == 32 || radioKeyInput.getText().length() == 0) {
					radioKeyButton.setEnabled(true);
				} else {
					radioKeyButton.setEnabled(false);

				}
			}

		}
		);


		// ---------------------------------------------------------------------
		// -------  Set the radio default key ----------------------------------
		// ---------------------------------------------------------------------
		Group otapKeyGroup = new Group(mainPanel, SWT.NONE);
		otapKeyGroup.setText("Otap Key");
		otapKeyGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		groupLayout = new GridLayout(2, false);
		otapKeyGroup.setLayout(groupLayout);


		otapKeyButton = new Button(otapKeyGroup, SWT.CHECK);
		//otapKeyButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		otapKeyButton.setText("Use OTAP key:");
		otapKeyButton.setSelection(false);
		otapKeyButton.setEnabled(false);
		otapKeyButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				if (otapKeyButton.getSelection()) {
					otapPlugin.setOtapKey(otapKeyInput.getText(), true);
				} else {
					otapPlugin.setOtapKey(null, false);
				}
				log.debug("otap key selection = " + otapKeyButton.getSelection() + " key =" + otapKeyInput.getText());
			}

		}
		);
		updateState();
		otapKeyInput = new Text(otapKeyGroup, SWT.BORDER);
		otapKeyInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		otapKeyInput.addVerifyListener(new KeyValidator(otapKeyInput, 32));
		otapKeyInput.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				if (otapKeyInput.getText().length() == 32) {
					otapKeyButton.setEnabled(true);
				} else {
					otapKeyButton.setEnabled(false);
					if (otapKeyButton.getSelection()) {
						otapKeyButton.setSelection(false);
						otapPlugin.setOtapKey(null, false);
					}
				}
			}

		}
		);

	}


	//-------------------------------------------------------------------------
	*//**
	 *
	 *//*
	*//*
	private void packTableView() {
		// Update table layout
		TableColumn[] c = new TableColumn[] { tableView.getTable().getColumn(0), tableView.getTable().getColumn(1), tableView.getTable().getColumn(2), tableView.getTable().getColumn(3) };

		tableView.getTable().layout();
		int w = tableView.getControl().getSize().x;
		c[0].setWidth(Math.max(0, 1 * w / 6 - 6));
		c[1].setWidth(Math.max(0, 1 * w / 6 - 6));
		c[2].setWidth(Math.max(0, 2 * w / 6 - 6));
		c[3].setWidth(Math.max(0, 2 * w / 6 - 6));
		tableView.getTable().layout();
	}
*//*
	//-------------------------------------------------------------------------
	*//**
	 *
	 *//*
	*//*
	protected TableItem getItemForDevice(OtapDevice device) {
		TableItem item = null;
		for (TableItem current : tableView.getTable().getItems())
			if (current.getData("device") != null && current.getData("device").equals(device))
				item = current;

		return item;
	}*//*

	//-------------------------------------------------------------------------
	*//**
	 *
	 *//**//*
	public void addDevice(final OtapDevice device) {
		mainPanel.getDisplay().asyncExec(new Runnable() {
			public void run() {
				TableItem item = getItemForDevice(device);

				if (item == null) {
					
					// Create table item
					item = new TableItem(tableView.getTable(), SWT.NONE);
					item.setData("device", device);
					
					// Create progress bar
					ProgressBar progress = new ProgressBar(tableView.getTable(), SWT.NONE);
					item.setData("progress", progress);
					
					TableEditor editor = new TableEditor(tableView.getTable());
					editor.grabHorizontal = editor.grabVertical = true;
					editor.setEditor(progress, item, 3);
					item.setData("editor", editor);

					updateDevice(device);
				}
			}
		});
	}*//*

	//-------------------------------------------------------------------------
	*//**
	 *
	 *//**//*
	public void removeDevice(final OtapDevice device) {

		mainPanel.getDisplay().asyncExec(new Runnable() {
			public void run() {
				TableItem item = getItemForDevice(device);

				if (item != null) {
					ProgressBar progress = (ProgressBar) item.getData("progress");
					progress.dispose();

					TableEditor editor = (TableEditor) item.getData("editor");
					editor.dispose();
					item.dispose();
					//contentProvider.inputChanged(tableView, device, device);
					packTableView();
				} else
					log.debug("Unable to remove device: " + device);
			}
		});

	}
*//*
	//-------------------------------------------------------------------------
	*//**
	 *
	 *//**//*
	public void updateDevice(final OtapDevice device) {
		mainPanel.getDisplay().asyncExec(new Runnable() {

			public void run() {
				TableItem item = getItemForDevice(device);
				if (item != null) {
					item.setText(new String[] { String.format("0x%04x", device.getId()), ChipType.toString(device.getChipType()) , device.getStatusMessage(), });
					if (device.programmable())
						item.setForeground( new Color(mainPanel.getDisplay(),0,0,0));
					else
						item.setForeground( new Color(mainPanel.getDisplay(),160,160,160));
					
					ProgressBar progress = (ProgressBar) item.getData("progress");
					progress.setSelection((int) (device.getProgress() * 100));
					//contentProvider.inputChanged(tableView, null, device);
					packTableView();
				}
				else {
					// TODO: this may result in an INFINITE LOOP if there is no Item which can be used -> problem?
					addDevice(device);
					log.debug("Could not update device " + device.getId() + " -> added");
				}
			}
		});
	}*//*
	// -------------------------------------------------------------------------

	*//**
	 * @changed strebel
	 *//*
	public void addDevice(final OtapDevice device) {
		mainPanel.getDisplay().asyncExec(new Runnable() {
			public void run() {
				selectedDevices = getSelectedDevices();
				tableView.setInput(device);
				setSelectedDevices(selectedDevices);
			}
		}
		);
	}

	//--------------------------------------------------------------------------

	*//**
	 *
	 *//*
	public void removeDevice(final OtapDevice d) {
		mainPanel.getDisplay().asyncExec(new Runnable() {
			public void run() {
				selectedDevices = getSelectedDevices();
				contentProvider.removeDevice(d);
				setSelectedDevices(selectedDevices);
			}
		}
		);
	}
	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	public void updateDevice(final OtapDevice device) {
		mainPanel.getDisplay().asyncExec(new Runnable() {
			public void run() {
				tableView.setInput(device);
			}
		}
		);
	}
	//-------------------------------------------------------------------------

	*//**
	 * @changed strebel
	 *//*
	public Set<OtapDevice> getSelectedDevices() {
		final Set<OtapDevice> devices = new HashSet<OtapDevice>();

		mainPanel.getDisplay().syncExec(new Runnable() {

			public void run() {
				for (TableItem item : tableView.getTable().getItems()) {
					if (item.getChecked()) {
						devices.add((OtapDevice) item.getData());
					}
				}
			}

		}
		);

		return devices;
	}

	//-------------------------------------------------------------------------

	*//**
	 * @author strebel
	 *//*
	public void setSelectedDevices(final Set<OtapDevice> devices) {
		mainPanel.getDisplay().syncExec(new Runnable() {
			public void run() {
				for (TableItem item : tableView.getTable().getItems()) {
					for (OtapDevice device : devices) {
						if (((OtapDevice) item.getData()).equals(device)) {
							item.setChecked(true);
						}
					}
				}
			}
		}
		);
	}

	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	public Set<OtapDevice> getSelectedAndProgrammableDevices() {
		final Set<OtapDevice> devices = new HashSet<OtapDevice>();

		mainPanel.getDisplay().syncExec(new Runnable() {

			public void run() {
				*//*
				for (TableItem item : tableView.getTable().getItems())
					if (item.getChecked() && item.getData("device") != null)
					{
						if ( ((OtapDevice) item.getData("device")).programmable())
							devices.add((OtapDevice) item.getData("device"));
					}
					*//*
				for (TableItem item : tableView.getTable().getItems()) {
					log.debug("get selected " + ((OtapDevice) item.getData()).getId());
					if (item.getChecked()) {
						devices.add((OtapDevice) item.getData());
					}
				}
			}
		}
		);

		return devices;
	}

	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	public Set<OtapDevice> getDevices() {
		final Set<OtapDevice> devices = new HashSet<OtapDevice>();

		mainPanel.getDisplay().syncExec(new Runnable() {

			public void run() {
				*//*for (TableItem item : tableView.getTable().getItems())
					if (item.getData("device") != null)
						devices.add((OtapDevice) item.getData("device"));*//*
				for (TableItem item : tableView.getTable().getItems()) {
					log.debug("get " + ((OtapDevice) item.getData()).getId());
					if (item.getData() != null) {
						devices.add((OtapDevice) item.getData());
					}
				}
			}
		}
		);
		return devices;
	}

	//-------------------------------------------------------------------------

	*//**
	 * Set the text according to the state
	 *//*
	void updateState() {
		if (!mainPanel.isDisposed()) {
			mainPanel.getDisplay().syncExec(new Runnable() {

				public void run() {

					//Update the Presence detect Button
					if (presenceDetectButton != null && otapControlButton != null) {
						statusLabel.setText(otapPlugin.getOtapStatusText());
						statusLabel.pack(true);
						statusLabel.getParent().layout();

						switch (otapPlugin.getState()) {
							case None:
								motapEnableButton.setEnabled(true);
								if (otapPlugin.getProgramFilename() != null && !""
										.equals(otapPlugin.getProgramFilename())) {
									presenceDetectButton.setEnabled(true);
									presenceDetectButton.setText("Start presence detection");
								} else {
									presenceDetectButton.setEnabled(false);
									presenceDetectButton.setText("[Select bin file first]");
								}
								presenceDetectButton.pack();
								channelSelectionBox.setEnabled(true);
								openFilebutton.setEnabled(true);
								selectAllButton.setEnabled(false);
								unselectAllButton.setEnabled(false);
								otapControlButton.setEnabled(false);
								otapControlButton.setText("[Presence detect first]");
								if (otapPlugin.getProgramFilename() != null) {
									filenameLabel.setText(otapPlugin.getProgramFilename());
								} else {
									filenameLabel.setText("");
								}
								filenameLabel.pack();
								filenameLabel.getParent().layout();

								if (otapKeyInput != null) {
									if (otapKeyInput.getText() != null) {
										if (otapKeyInput.getText().length() == 32) {
											if (otapKeyButton != null) {
												otapKeyButton.setEnabled(true);
											}
										}
									}
								}
								if (otapKeyInput != null) {
									otapKeyInput.setEnabled(true);
								}

								if (radioKeyInput != null) {
									if (radioKeyInput.getText() != null) {
										if ((radioKeyInput.getText().length() == 32) || (radioKeyInput.getText()
												.length() == 0)) {
											if (radioKeyButton != null) {
												radioKeyButton.setEnabled(true);
											}
										}
									}
								}
								if (radioKeyInput != null) {
									radioKeyInput.setEnabled(true);
								}

								break;

							case PresenceDetect:
								motapEnableButton.setEnabled(false);
								presenceDetectButton.setEnabled(true);
								presenceDetectButton.setText("Stop presence detection");
								presenceDetectButton.pack();
								channelSelectionBox.setEnabled(false);
								openFilebutton.setEnabled(false);
								selectAllButton.setEnabled(true);
								unselectAllButton.setEnabled(true);
								if (getSelectedAndProgrammableDevices().size() > 0) {
									otapControlButton.setEnabled(true);
									otapControlButton.setText("Start programming");
								} else {
									otapControlButton.setEnabled(false);
									otapControlButton.setText("[Select device(s) first]");
								}
								otapKeyButton.setEnabled(false);
								otapKeyInput.setEnabled(false);
								radioKeyButton.setEnabled(false);
								radioKeyInput.setEnabled(false);
								break;

							case OtapInit:
								log.debug("state=otapInit");
							case Otap:
								log.debug("state=otap");
								motapEnableButton.setEnabled(false);
								presenceDetectButton.setEnabled(false);
								presenceDetectButton.setText("[Presence detection disabled]");
								presenceDetectButton.pack();
								channelSelectionBox.setEnabled(false);
								openFilebutton.setEnabled(false);
								selectAllButton.setEnabled(false);
								unselectAllButton.setEnabled(false);
								otapControlButton.setEnabled(true);
								otapControlButton.setText("Stop programming");
								break;
						}
					}
				}
			}
			);
		}

	}

	//	-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	protected void handleMotapEnableButton() {
		if (otapPlugin.getMultihopSupportState()) {
			otapPlugin.setMultihopSupportState(false);
		} else {
			otapPlugin.setMultihopSupportState(true);
		}

		log.debug("Multihop support button clicked, MH supported: " + otapPlugin.getMultihopSupportState());

	}
	//	-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	private synchronized void handlePresenceDetectControlButton() {
		log.debug("Presence detection button clicked, state(" + otapPlugin.getState() + ")");

		switch (otapPlugin.getState()) {
			case PresenceDetect:
				otapPlugin.setPresenceDetectState(false);
				break;

			default:
				otapPlugin.getPresenceDetect().clearDeviceList();
				tableView.setInput(null);
				otapPlugin.setPresenceDetectState(true);
				break;
		}

		updateState();
	}

	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	private synchronized void handleOtapInitControlButton() {
		log.debug("Otap button clicked, state(" + otapPlugin.getState() + ")");

		switch (otapPlugin.getState()) {
			case None:

			case PresenceDetect:
				otapPlugin.setParticipatingDeviceList(getSelectedAndProgrammableDevices());
				otapPlugin.otapStart();
				break;

			case OtapInit:
			case Otap:
				otapPlugin.otapStop();
				break;
		}

		updateState();
	}

	//-------------------------------------------------------------------------

	*//**
	 *
	 *//*
	private synchronized void handleRadioKeyButton() {
		//log.debug("Otap button clicked, state(" + otapPlugin.getState() + ")");

		switch (otapPlugin.getState()) {
			case None:
				otapPlugin.setRadioKey(radioKeyInput.getText());
				break;
		}

		updateState();
	}

//	//--------------------------------------------------------------------------
//	*//**
//	 * @author strebel
//	 * TODO strebel remove after testing
//	 *//*
//	private synchronized void handleFillTableButton() {
//		Random random = new Random();
//		OtapDevice device = new OtapDevice();
//		device.setApplicationID(1);
//		device.setSoftwareRevision(random.nextInt(12));
//		device.setId(random.nextInt(0xFFFE));
//		device.setProgress(random.nextDouble());
//		device.setStatusMessage("OK");
//		device.setChipType(ChipType.JN513XR1);
//		addDevice(device);
//	}

	public void setStatus(String message) {
		statusLabel.setText(message);

	}

	public void warnOtapFunctionality(final String s) {
		if (!mainPanel.isDisposed()) {
			mainPanel.getDisplay().syncExec(new Runnable() {

				public void run() {

					MessageDialog.openWarning(mainPanel.getShell(), "Warning", s);
				}
			}
			);
		}
	}*/

}
