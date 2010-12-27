package com.coalesenses.otap.core.cli;


import com.google.common.collect.Lists;

import java.util.List;

public class OtapConfig {

	String program;

	int channel = 12;

	boolean multihop = true;

	boolean force = false;

	List<Long> macs = Lists.newLinkedList();

	boolean all = false;

}
