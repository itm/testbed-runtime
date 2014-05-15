package org.apache.cxf.jaxrs.client;

import java.util.List;

public abstract class JAXRSClientFactoryPlus {

	private JAXRSClientFactoryPlus() {
	}

	public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, String username, String password) {
		JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, null);
		bean.setServiceClass(cls);
		bean.setProviders(providers);
		bean.setUsername(username);
		bean.setPassword(password);
		return bean.create(cls);
	}
}
