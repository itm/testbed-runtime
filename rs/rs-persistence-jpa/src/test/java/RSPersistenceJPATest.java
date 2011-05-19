/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistenceTest;
import de.uniluebeck.itm.tr.rs.persistence.jpa.RSPersistenceJPAFactory;
import eu.wisebed.api.rs.RSExceptionException;
import org.junit.Before;

import java.util.*;

public class RSPersistenceJPATest extends RSPersistenceTest {
	private final static TimeZone localTimeZone = TimeZone.getTimeZone("GMT");
	private static final Map<String, String> properties = new HashMap<String, String>() {{
		//Configure Apache
		put("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
		put("hibernate.connection.url", "jdbc:derby:target/default;create=true");
		put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
		//Configure Hibernate
		put("hibernate.ddl-generation.output-mode", "database");
		put("hibernate.hbm2ddl.auto", "create");
		put("hibernate.archive.autodetection", "class, hbm");
	}};

	@Before
	public void setUp() throws RSExceptionException {
		super.setUp();

		RSPersistence persistence = RSPersistenceJPAFactory.createInstance(properties, localTimeZone);
		super.setPersistence(persistence);
	}
}
