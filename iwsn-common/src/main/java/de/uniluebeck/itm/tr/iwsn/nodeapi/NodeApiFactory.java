package de.uniluebeck.itm.tr.iwsn.nodeapi;

import java.util.concurrent.TimeUnit;

public interface NodeApiFactory {

	NodeApi create(String nodeUrn, NodeApiDeviceAdapter deviceAdapter, long defaultTimeout, TimeUnit defaultTimeUnit);
}
