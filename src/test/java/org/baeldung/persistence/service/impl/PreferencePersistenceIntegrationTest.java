package org.baeldung.persistence.service.impl;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import org.baeldung.common.AbstractServicePersistenceIntegrationTest;
import org.baeldung.persistence.TestJpaConfig;
import org.baeldung.persistence.dao.PreferenceRepository;
import org.baeldung.persistence.model.Preference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestJpaConfig.class }, loader = AnnotationConfigContextLoader.class)
public class PreferencePersistenceIntegrationTest extends AbstractServicePersistenceIntegrationTest<Preference> {

	@Autowired
	private PreferenceRepository preferenceRepository;

	// tests

	@Test
	public final void whenContextIsBootstrapped_thenNoExceptions() {
		//
	}

	@Test
	public final void whenEntityIsCreated_thenNoExceptions() {
		getApi().save(createNewEntity());
	}

	@Test(expected = DataIntegrityViolationException.class)
	public final void whenInvalidEntityIsCreated_thenDataException() {
		getApi().save(createInvalidNewEntity());
	}

	// API - protected

	@Override
	protected final PreferenceRepository getApi() {
		return preferenceRepository;
	}

	@Override
	protected final void invalidate(final Preference entity) {
		entity.setEmail(null);
	}

	@Override
	protected final Preference createNewEntity() {
		final Preference preference = new Preference();
		preference.setEmail(randomAlphabetic(6) + "@gmail.com");
		preference.setSubreddit("java");
		return preference;
	}

}