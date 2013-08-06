/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.saml;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Condition;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.OneTimeUse;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.schema.XSString;


public class SAMLGenerator {
    
    private XMLObjectBuilderFactory builderFactory = null;
    
    public SAMLGenerator() {
    }
    
    public XMLObjectBuilderFactory getBuilderFactory() {
        if (this.builderFactory == null) {
            try {
                DefaultBootstrap.bootstrap();
                this.builderFactory = Configuration.getBuilderFactory();
            } catch (ConfigurationException ex) {
                Logger.getLogger(SAMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return builderFactory; 
    }
      
            
    /******************************************
     * Create the NameIdentifier
     ******************************************/
    public NameID createNameID(SAMLInput input) {
        SAMLObjectBuilder nameIDBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        NameID nameID = (NameID) nameIDBuilder.buildObject();
        nameID.setValue(input.getNameID());
        nameID.setNameQualifier(input.getNameQualifier());
        nameID.setFormat(NameID.UNSPECIFIED);
        return nameID;
    }

    /*******************************************
     * Create the subject confirmation
     ******************************************/
    public SubjectConfirmation createSubjectConfirmation(int timeToConfirm) {
        SAMLObjectBuilder confirmationMethodBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        SubjectConfirmationData confirmationMethod = (SubjectConfirmationData) confirmationMethodBuilder.buildObject();
        DateTime now = new DateTime();
        confirmationMethod.setNotBefore(now);
        confirmationMethod.setNotOnOrAfter(now.plusMinutes(timeToConfirm));

        SAMLObjectBuilder subjectConfirmationBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) subjectConfirmationBuilder.buildObject();
        subjectConfirmation.setSubjectConfirmationData(confirmationMethod);

        return subjectConfirmation;
    }
   
   /*********************************************
    * Create the Subject
    *********************************************/
    public Subject createSubject(NameID nameID, SubjectConfirmation subjectConfirmation) {
        SAMLObjectBuilder sujectBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        Subject subject = (Subject) sujectBuilder.buildObject();
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        return subject;
    }
   
   /**********************************************
    * Create the authentification statement
    **********************************************/
   public AuthnStatement createAuthnStatement(SAMLInput input) {
       SAMLObjectBuilder authnStatementBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME);
       AuthnStatement authnStatement = (AuthnStatement) authnStatementBuilder.buildObject();
       
       DateTime now = new DateTime();
       authnStatement.setAuthnInstant(now);
       authnStatement.setSessionIndex(input.getSessionID());
       authnStatement.setSessionNotOnOrAfter(now.plusMinutes(input.getMaxSessionTimeout()));
       
       // TODO: can add the authentification context
       
       return authnStatement;
   }
   
   /*********************************************
    * Create attribute statement
    ********************************************/
   public AttributeStatement createAttributeStatement(SAMLInput input) {
       SAMLObjectBuilder attributeStatementBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
       AttributeStatement attributeStatement = (AttributeStatement) attributeStatementBuilder.buildObject();
       
       // Create attribute statement
       Map attributes = input.getAttributes();
       if(attributes != null){
	        	
           Set<String> keySet = (Set<String>)attributes.keySet();
	        	 for (String key : keySet) {
	        		 Attribute attrFirstName = buildStringAttribute(key, (String)attributes.get(key));
	        		 attributeStatement.getAttributes().add(attrFirstName);
			}
	         }
       
       return attributeStatement;
   }

   /**************************************
    * Build SAML attribute of type String
    **************************************/
    private Attribute buildStringAttribute(String name, String value) {
    
        SAMLObjectBuilder attributeBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
        Attribute attributeFirstName = (Attribute) attributeBuilder.buildObject();
        attributeFirstName.setName(name);
        
        // Set custom attribute
        XMLObjectBuilder stringBuilder = getBuilderFactory().getBuilder(XSString.TYPE_NAME);
        XSString attributeValueFirstName = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attributeValueFirstName.setValue(value);
        
        
        attributeFirstName.getAttributeValues().add(attributeValueFirstName);
        return attributeFirstName;
    }
   
    
    /**************************************
     * Create do-not cach condition
     **************************************/
    public Conditions createDoNotCatchCondition() {
        SAMLObjectBuilder doNotCacheConditionBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(OneTimeUse.DEFAULT_ELEMENT_NAME);
        Condition condition = (Condition) doNotCacheConditionBuilder.buildObject();
        
        SAMLObjectBuilder conditionsBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
        Conditions conditions = (Conditions) conditionsBuilder.buildObject();
        conditions.getConditions().add(condition);
        
        return conditions;
    }
    
    /***************************************
     * Create Issuer
     **************************************/
    public Issuer createIssuer(SAMLInput input) {
        SAMLObjectBuilder issuerBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        Issuer issuer = (Issuer) issuerBuilder.buildObject();
        issuer.setValue(input.getIssuer());
        return issuer;
       
    }

    /****************************************
     * Create SAML Assertion
     ****************************************/
    public Assertion createAssertion(SAMLInput input) {
        try {
            SAMLObjectBuilder assertionBuilder = (SAMLObjectBuilder) getBuilderFactory().getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
            Assertion assertion = (Assertion) assertionBuilder.buildObject();
            assertion.setIssuer(createIssuer(input));
            DateTime now = new DateTime();
            assertion.setIssueInstant(now);
            
            assertion.setVersion(SAMLVersion.VERSION_20);
            
            assertion.getAuthnStatements().add(createAuthnStatement(input));
            assertion.getAttributeStatements().add(createAttributeStatement(input));
            assertion.setConditions(createDoNotCatchCondition());
            return assertion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
