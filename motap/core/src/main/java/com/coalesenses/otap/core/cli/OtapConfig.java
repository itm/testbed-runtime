package com.coalesenses.otap.core.cli;


import com.google.common.collect.Lists;

import java.util.List;

public class OtapConfig {

	public String program;

	public int channel = 12;

	public boolean multihop = true;

	public boolean force = false;

	public List<Long> macs = Lists.newLinkedList();

	public boolean all = false;

}
