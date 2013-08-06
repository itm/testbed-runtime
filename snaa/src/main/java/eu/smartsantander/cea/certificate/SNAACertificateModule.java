/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.certificate;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import eu.wisebed.api.v3.snaa.SNAA;
import javax.persistence.EntityManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.subject.Subject;


public class SNAACertificateModule extends ShiroModule {

        
    public SNAACertificateModule() {
        
    }
    
    @Override
    protected void configureShiro() {
        
        requireBinding(CommonConfig.class);
	requireBinding(SNAAServiceConfig.class);
	requireBinding(EntityManager.class);
	requireBinding(ServicePublisher.class);
	requireBinding(ServedNodeUrnPrefixesProvider.class);
        
                             
        bindRealm().to(X509CertificateRealm.class).in(Singleton.class);
        
        bind(SNAACertificate.class).in(Scopes.SINGLETON);
        bind(SNAA.class).to(SNAACertificate.class);
        bind(SNAAService.class).to(SNAACertificate.class);

        expose(SNAACertificate.class);
	expose(SNAA.class);
	expose(SNAAService.class);
    }
    
    @Provides
    private Subject provideCurrentUser() {
	return SecurityUtils.getSubject();
    }
    
}
