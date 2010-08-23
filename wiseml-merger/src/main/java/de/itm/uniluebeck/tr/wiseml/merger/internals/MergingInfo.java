package de.itm.uniluebeck.tr.wiseml.merger.internals;

import javax.xml.stream.XMLStreamReader;

import de.itm.uniluebeck.tr.wiseml.merger.config.MergerConfiguration;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Setup;

public class MergingInfo {
	
	private MergerConfiguration config;

    private XMLStreamReader[] readers;
    private Setup[] inputSetups;
    private Setup mergedSetup;

    private ReadingStage stage;
	
	public MergingInfo(MergerConfiguration config, XMLStreamReader[] readers) {
		super();
		this.config = config;
		this.readers = readers;
        this.stage = ReadingStage.Head;
	}

	public MergerConfiguration getConfig() {
		return config;
	}

	public XMLStreamReader[] getReaders() {
		return readers;
	}

    public ReadingStage getStage() {
        return stage;
    }

    public void nextStage() {
        if (stage.ordinal() < ReadingStage.values().length-1) {
            stage = ReadingStage.values()[stage.ordinal()+1];
        } else {
            throw new IllegalStateException("no more stages");
        }
    }

}
