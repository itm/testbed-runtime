package de.itm.uniluebeck.tr.wiseml.merger.internals;

public enum WiseMLTag {
	
	wiseml(false),
	setup(false),
	origin(false),
//	coordinate(false),
	x(true),
	y(true),
	z(true),
	phi(true),
	theta(true),
	timeinfo(false),
	start(true),
	end(true),
	duration(true),
	unit(true),
	interpolation(true),
	coordinateType(true),
	description(true),
	defaults(false),
	node(false),
	position(false),
	gateway(true),
	programDetails(true),
	nodeType(true),
	capability(false),
	name(true),
	dataType(true),
	capabilityDefaultValue("default", true),
	link(false),
	encrypted(true),
	virtual(true),
	rssi(true), // TODO ?
	scenario(false),
	timestamp(true),
	enableNode(false),
	disableNode(false),
	enableLink(false),
	disableLink(false),
	data(true),
	trace(false),
	;
	
	private String localName;
	private boolean textOnly;
	
	private WiseMLTag(final String localName, boolean textOnly) {
		this.localName = localName;
		this.textOnly = textOnly;
	}
	
	private WiseMLTag(final boolean textOnly) {
		this.textOnly = textOnly;
		this.localName = this.name();
	}
	
	public boolean isTextOnly() {
		return textOnly;
	}
	
	public String getLocalName() {
		return localName;
	}

}
