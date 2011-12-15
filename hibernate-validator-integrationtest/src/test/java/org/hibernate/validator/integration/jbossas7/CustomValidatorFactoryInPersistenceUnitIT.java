/*
* JBoss, Home of Professional Open Source
* Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validator.integration.jbossas7;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidatorFactory;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;

/**
 * Tests the integration of Hibernate Validator in JBoss AS 7
 *
 * @author Hardy Ferentschik
 */
@RunWith(Arquillian.class)
public class CustomValidatorFactoryInPersistenceUnitIT {

	private static final Logger log = Logger.getLogger( CustomValidatorFactoryInPersistenceUnitIT.class );

	@Deployment
	public static Archive<?> createTestArchive() {
		return ShrinkWrap
				.create( WebArchive.class, CustomValidatorFactoryInPersistenceUnitIT.class.getSimpleName() + ".war" )
				.addClasses( User.class, MyValidationProvider.class, MyValidatorConfiguration.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsResource( "validation.xml", "META-INF/validation.xml" )
				.addAsResource( "javax.validation.spi.ValidationProvider", "META-INF/services/javax.validation.spi.ValidationProvider" )
				.addAsLibraries(
						DependencyResolvers.use( MavenDependencyResolver.class )
								.artifact( "log4j:log4j:1.2.16" )
								.resolveAs( JavaArchive.class )
				)
				.addAsResource( "log4j.properties" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.persistenceUnit( "default" )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.property( "hibernate.hbm2ddl.auto", "create-drop" )
				.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@PersistenceContext
	EntityManager em;

	@Test
	public void testValidatorFactoryPassedToPersistenceUnit() throws Exception {
		log.debug( "Running testValidatorFactoryPassedToPersistenceUnit..." );
		Map<String, Object> properties = em.getEntityManagerFactory().getProperties();
		Object obj = properties.get( "javax.persistence.validation.factory" );
		assertTrue( "There should be an object under this property", obj != null );
		ValidatorFactory factory = (ValidatorFactory) obj;
		assertTrue(
				"The Custom Validator implementation should be used",
				factory instanceof MyValidationProvider.DummyValidatorFactory
		);
		log.debug( "testValidatorFactoryPassedToPersistenceUnit completed" );
	}
}

