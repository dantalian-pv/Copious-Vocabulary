package ru.dantalian.copvoc.persist.elastic.model;

import java.util.Map;
import java.util.UUID;

import ru.dantalian.copvoc.persist.elastic.model.annotations.Field;
import ru.dantalian.copvoc.persist.elastic.model.annotations.Id;
import ru.dantalian.copvoc.persist.elastic.model.annotations.SubField;
import ru.dantalian.copvoc.persist.elastic.model.codecs.UUIDCodec;

public class DbCard {

	@Id
	@Field(codec = UUIDCodec.class)
	private UUID id;

	@Field(name = "vocabulary_id", codec = UUIDCodec.class)
	private UUID vocabularyId;

	@Field
	private String sourceLang;

	@Field
	private String targetLang;

	@Field
	private String source;

	@Field(name = "content", type = "object", subtype = {
			@SubField(path_match="content.*_keyword"),
			@SubField(path_match="content.*_text", type = "text")
	})
	private Map<String, String> fieldsContent;

	@Field(name = "stats", type = "object", subtype = {
			@SubField(path_match="stats.*_long", type = "long"),
			@SubField(path_match="stats.*_double", type = "double"),
			@SubField(path_match="stats.*_date", type = "date")
	})
	private Map<String, Object> stats;

	public DbCard() {
	}

	public DbCard(final UUID aId, final UUID aVocabularyId, final String aSourceLang, final String aTargetLang,
			final String aSource,
			final Map<String, String> aFieldsContent, final Map<String, Object> aStats) {
		id = aId;
		vocabularyId = aVocabularyId;
		sourceLang = aSourceLang;
		targetLang = aTargetLang;
		source = aSource;
		fieldsContent = aFieldsContent;
		stats = aStats;
	}

	public UUID getId() {
		return id;
	}

	public void setId(final UUID aId) {
		id = aId;
	}

	public UUID getVocabularyId() {
		return vocabularyId;
	}

	public void setVocabularyId(final UUID aVocabularyId) {
		vocabularyId = aVocabularyId;
	}

	public String getSourceLang() {
		return sourceLang;
	}

	public void setSourceLang(final String aSourceLang) {
		sourceLang = aSourceLang;
	}

	public String getTargetLang() {
		return targetLang;
	}

	public void setTargetLang(final String aTargetLang) {
		targetLang = aTargetLang;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String aSource) {
		source = aSource;
	}

	public Map<String, String> getFieldsContent() {
		return fieldsContent;
	}

	public void setFieldsContent(final Map<String, String> aFieldsContent) {
		fieldsContent = aFieldsContent;
	}

	public Map<String, Object> getStats() {
		return stats;
	}

	public void setStats(final Map<String, Object> aStats) {
		stats = aStats;
	}

}
