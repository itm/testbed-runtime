package de.uniluebeck.itm.tr.iwsn.gateway.events;


public class GatewayFailureEvent {

    private final String message;
    private final Throwable cause;

    public GatewayFailureEvent(final String message, final Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
