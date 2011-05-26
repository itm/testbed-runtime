package de.uniluebeck.itm.motelist;


import com.google.common.collect.ImmutableList;
import de.uniluebeck.itm.tr.util.Listenable;

public interface DeviceObserver extends Runnable, Listenable<DeviceObserverListener> {

	ImmutableList<DeviceEvent> getMoteEvents();

}
