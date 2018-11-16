package ru.dantalian.copvoc.core.managers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ru.dantalian.copvac.persist.api.PersistException;
import ru.dantalian.copvac.persist.api.PersistLanguageManager;
import ru.dantalian.copvac.persist.api.model.Language;
import ru.dantalian.copvac.persist.impl.model.personal.PojoLanguage;
import ru.dantalian.copvoc.core.CoreConstants;
import ru.dantalian.copvoc.core.CoreException;

@Singleton
public class LanguageManager implements Closeable {

	@Inject
	private PersistLanguageManager languagePersist;

	public List<Language> initLanguages() throws CoreException {
		try {
			final ObjectMapper om = new ObjectMapper();
			try (InputStream langStream = this.getClass().getClassLoader()
					.getResourceAsStream(CoreConstants.DEFAULT_LANGUAGES)) {
				final ArrayNode arr = (ArrayNode) om.readTree(langStream).get("laguages");
				for (final JsonNode node: arr) {
					final PojoLanguage lang = om.treeToValue(node, PojoLanguage.class);
					languagePersist.updateLanguage(lang.getName(), lang.getCountry(), lang.getVariant(), lang.getText());
				}
			}
			return languagePersist.listLanguages(Optional.empty(), Optional.empty(), Optional.empty());
		} catch (final PersistException | IOException e) {
			throw new CoreException("Failed to init languages", e);
		}
	}

	public List<Language> listLanguages(final Optional<String> aName, final Optional<String> aCountry,
			final Optional<String> aVariant) throws CoreException {
		try {
			return languagePersist.listLanguages(aName, aCountry, aVariant);
		} catch (final PersistException e) {
			throw new CoreException("Failed to list languages", e);
		}
	}

	public Language getLanguage(final String aName, final String aCountry, final String aVariant)
			throws CoreException {
		try {
			return languagePersist.getLanguage(aName, aCountry, aVariant);
		} catch (final PersistException e) {
			throw new CoreException("Failed to get language", e);
		}
	}

	public Language createLanguage(final String aName, final String aCountry, final String aVariant,
			final String aText) throws CoreException {
		try {
			return languagePersist.createLanguage(aName, aCountry, aVariant, aText);
		} catch (final PersistException e) {
			throw new CoreException("Failed to create language", e);
		}
	}

	@Override
	public void close() throws IOException {
		languagePersist.close();
	}

}