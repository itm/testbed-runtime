package de.uniluebeck.itm.tr.iwsn.nodeapi;


public interface NodeApiCallResult {

	/**
	 * Returns {@code true} if a reply from the sensor node with a result value of {@link
	 * de.uniluebeck.itm.tr.iwsn.nodeapi.ResponseType#COMMAND_SUCCESS} was received, {@code false} otherwise.
	 *
	 * @return see above
	 */
	boolean isSuccessful();

	/**
	 * {@code Null} if call was successful, the response code the node sent with the reply, indicating the type of failure
	 * otherwise.
	 *
	 * @return {@code null} in case of success, a byte value indicating type of failure otherwise
	 */
	byte getResponseType();

	/**
	 * Returns the payload that is attached to the reply message, may be {@code null}.
	 *
	 * @return the payload that is attached to the reply message, may be {@code null}
	 */
	byte[] getResponse();

}
