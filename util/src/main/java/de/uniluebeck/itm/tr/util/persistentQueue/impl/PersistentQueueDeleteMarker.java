package de.uniluebeck.itm.tr.util.persistentQueue.impl;

import java.io.Serializable;

/**
 * Helper class for {@link com.gaborcselle.persistent.PersistentQueue}.
 * Instances represent that the first element of a list has been deleted.
 *
 * @author Gabor Cselle
 * @version 1.0
 */
public class PersistentQueueDeleteMarker implements Serializable {
    public final static long serialVersionUID = 1;

    public PersistentQueueDeleteMarker() {
        super();
    }
}
