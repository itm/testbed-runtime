package de.uniluebeck.itm.motelist;

import org.apache.commons.lang.SystemUtils;

import javax.annotation.Nullable;
import java.util.Map;

public class MoteListFactory {

    /**
     * Creates a new OS-specific instance of the {@link de.uniluebeck.itm.motelist.MoteList} tool.
     *
     * @param telosBReferenceToMACMap a mapping between USB chip IDs and MAC addresses. If not {@code null} the map that
     *                                is passed will be used to resolve Telos B motes' MAC addresses by looking up their
     *                                USB chip ID and looking in this map to resolve the corresponding MAC address. May
     *                                be {@code null}. If the map is {@code null} MAC address resolution for Telos B
     *                                motes will result in a {@link RuntimeException}.
     * @return an instance of the {@link de.uniluebeck.itm.motelist.MoteList} tool
     */
    public static MoteList create(@Nullable Map<String, String> telosBReferenceToMACMap) {
        if (SystemUtils.IS_OS_MAC_OSX) {
            return new MoteListMacOS(telosBReferenceToMACMap);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return new MoteListWin32(telosBReferenceToMACMap);
        } else if (SystemUtils.IS_OS_LINUX) {
            return new MoteListLinux(telosBReferenceToMACMap);
        } else {
            throw new RuntimeException("Only Linux, Mac OS X and Windows are supported by this version!");
        }
    }

}
