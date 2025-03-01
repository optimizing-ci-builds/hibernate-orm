/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.enumerated;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class EnumeratedSmokeTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void prepare() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void release() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	/**
	 * I personally have been unable to repeoduce the bug as reported in HHH-10402.  This test
	 * is equivalent to what the reporters say happens, but these tests pass fine.
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-10402" )
	public void testEnumeratedTypeResolutions() {
		final MetadataImplementor mappings = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( EntityWithEnumeratedAttributes.class )
				.buildMetadata();
		mappings.validate();

		final JdbcTypeRegistry jdbcTypeRegistry = mappings.getTypeConfiguration().getJdbcTypeRegistry();
		final PersistentClass entityBinding = mappings.getEntityBinding( EntityWithEnumeratedAttributes.class.getName() );

		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "notAnnotated" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "noEnumType" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "ordinalEnumType" ), EnumType.ORDINAL );
		validateEnumMapping( jdbcTypeRegistry, entityBinding.getProperty( "stringEnumType" ), EnumType.STRING );
	}

	private void validateEnumMapping(JdbcTypeRegistry jdbcRegistry, Property property, EnumType expectedJpaEnumType) {
		assertThat( property.getType(), instanceOf( CustomType.class ) );
		final CustomType<Object> customType = (CustomType<Object>) property.getType();
		assertThat( customType.getUserType(), instanceOf( org.hibernate.type.EnumType.class ) );
		final org.hibernate.type.EnumType hibernateMappingEnumType = (org.hibernate.type.EnumType) customType.getUserType();
		assertThat( hibernateMappingEnumType.isOrdinal(), is(expectedJpaEnumType==EnumType.ORDINAL) );
		final int expectedJdbcTypeCode = jdbcRegistry.getDescriptor(
				expectedJpaEnumType == EnumType.ORDINAL ?
						Types.SMALLINT :
						Types.VARCHAR
		).getJdbcTypeCode();
		assertThat(
				hibernateMappingEnumType.getSqlType(),
				is( expectedJdbcTypeCode )
		);
	}

	@Entity
	public static class EntityWithEnumeratedAttributes {
		@Id
		public Integer id;
		public Gender notAnnotated;
		@Enumerated
		public Gender noEnumType;
		@Enumerated(EnumType.ORDINAL)
		public Gender ordinalEnumType;
		@Enumerated(EnumType.STRING)
		public Gender stringEnumType;
	}

	public static enum Gender {
		MALE, FEMALE, UNKNOWN;
	}
}
