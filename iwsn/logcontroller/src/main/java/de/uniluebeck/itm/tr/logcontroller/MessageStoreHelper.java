package de.uniluebeck.itm.tr.logcontroller;

import de.uniluebeck.itm.tr.logcontroller.client.MessageStore;
import de.uniluebeck.itm.tr.logcontroller.client.MessageStore_Service;
import de.uniluebeck.itm.tr.util.FileUtils;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper for the MessageStore-Service
 */
public class MessageStoreHelper {
    private static Lock tmpFileMessageStoreLock = new ReentrantLock();
    private static File tmpFileMessageStore;

    /**
     * Returns the port to the MessageStore API.
     *
     * @param endpointUrl the endpoint URL to connect to
     * @return a {@link de.uniluebeck.itm.tr.logcontroller.client.MessageStore} instance that is
     *         connected to the Web Service endpoint
     */
    public static MessageStore getMessageStoreService(String endpointUrl) {
        QName qName = new QName("urn:MessageStore", "MessageStore");
        InputStream resourceStream = MessageStoreHelper.class.getClassLoader().getResourceAsStream("MessageStore_Service.wsdl");

        tmpFileMessageStoreLock.lock();
        try {
            if (tmpFileMessageStore == null) {
                try {
                    tmpFileMessageStore = FileUtils.copyToTmpFile(resourceStream, "tr.logcontroller.messagestore", "wsdl");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            tmpFileMessageStoreLock.unlock();
        }

        MessageStore_Service service;
        try {
            service = new MessageStore_Service(tmpFileMessageStore.toURI().toURL(), qName);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        MessageStore MessageStorePort = service.getMessageStorePort();

        Map<String, Object> ctxt = ((BindingProvider) MessageStorePort).getRequestContext();
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        return MessageStorePort;
    }
}
