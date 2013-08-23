/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class SAMLUtilities {
        
    public static Response getResponseObjectFromRequest(String resp) {
    Response res = null;
    try {
        DefaultBootstrap.bootstrap();
        ByteArrayInputStream is = new ByteArrayInputStream(resp.getBytes("UTF-8"));
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder;
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
                
        InputStream input = new ByteArrayInputStream(resp.getBytes("UTF-8"));
        Document document = documentBuilder.parse(input);
        Element element = document.getDocumentElement();
                
        UnmarshallerFactory unmarshallerFactory = org.opensaml.Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        XMLObject responeXmlObj = unmarshaller.unmarshall(element);
                
        res = (Response) responeXmlObj;
          
    } catch (ParserConfigurationException pe) {
        pe.printStackTrace();
    } catch (SAXException se) {
        se.printStackTrace();
    } catch (UnmarshallingException ue) {
        ue.printStackTrace();
    } catch (ConfigurationException ce) {
        ce.printStackTrace();
    } catch (Exception e) {
        e.printStackTrace();
    } 
    return res;
    }
       
    /****************************************
     * Get the attributs from SAML Assertion
     ***************************************/
    public static String getAttribute(Response response, String attributeName) {
        String value = null;
        List<Attribute> attrs = response.getAssertions().get(0).getAttributeStatements().get(0).getAttributes();
        Iterator<Attribute> it = attrs.iterator();
        while (it.hasNext()) {
            Attribute att = it.next();
            if (att.getName().equalsIgnoreCase(attributeName)) {
                List<XMLObject> attributeValues = att.getAttributeValues();
                for (int i=0; i< attributeValues.size(); i++) {
                    value = attributeValues.get(0).getDOM().getTextContent();
                }
            }
        }
        return value;
    }
}
