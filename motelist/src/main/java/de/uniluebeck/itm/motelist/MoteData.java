package de.uniluebeck.itm.motelist;

public class MoteData {

    public final MoteType moteType;

    public final String port;

    public final String reference;

    public MoteData(MoteType moteType, String port, String reference) {
        this.moteType = moteType;
        this.port = port;
        this.reference = reference;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MoteData");
        sb.append("{moteType=").append(moteType);
        sb.append(", port='").append(port).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append('}');
        return sb.toString();
    }
}