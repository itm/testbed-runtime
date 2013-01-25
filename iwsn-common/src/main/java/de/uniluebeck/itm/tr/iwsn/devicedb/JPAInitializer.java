package de.uniluebeck.itm.tr.iwsn.devicedb;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;

@Singleton
public class JPAInitializer
{
 
 @Inject
 public JPAInitializer(final PersistService service)
 {
  service.start();
 }
}