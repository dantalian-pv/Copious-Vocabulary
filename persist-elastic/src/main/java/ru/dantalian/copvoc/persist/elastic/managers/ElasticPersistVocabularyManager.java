package ru.dantalian.copvoc.persist.elastic.managers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.dantalian.copvoc.persist.api.PersistException;
import ru.dantalian.copvoc.persist.api.PersistVocabularyManager;
import ru.dantalian.copvoc.persist.api.model.Language;
import ru.dantalian.copvoc.persist.api.model.Vocabulary;
import ru.dantalian.copvoc.persist.api.utils.LanguageUtils;
import ru.dantalian.copvoc.persist.elastic.model.DbVocabulary;
import ru.dantalian.copvoc.persist.elastic.orm.ElasticORM;
import ru.dantalian.copvoc.persist.elastic.orm.ElasticORMFactory;
import ru.dantalian.copvoc.persist.impl.model.PojoVocabulary;

@Service
public class ElasticPersistVocabularyManager implements PersistVocabularyManager {

	private static final String DEFAULT_INDEX = "vocabularies";

	@Autowired
	private ElasticPersistLanguageManager mLangManager;

	@Autowired
	private DefaultSettingsProvider settingsProvider;

	@Autowired
	private ElasticORMFactory ormFactory;

	private ElasticORM<DbVocabulary> orm;

	@PostConstruct
	public void init() {
		orm = ormFactory.newElasticORM(DbVocabulary.class, settingsProvider);
	}

	@Override
	public Vocabulary createVocabulary(final String aUser, final String aName, final String aDescription,
			final Language aSource, final Language aTarget) throws PersistException {
		final UUID uuid = UUID.randomUUID();
		final String source = LanguageUtils.asString(aSource);
		final String target = LanguageUtils.asString(aTarget);
		final DbVocabulary voc = new DbVocabulary(uuid, aName, aDescription, aUser, source, target, false);
		orm.add(DEFAULT_INDEX, voc, true);
		return asVocabulary(voc);
	}

	@Override
	public void updateVocabulary(final String aUser, final Vocabulary aVocabulary) throws PersistException {
		final DbVocabulary voc = asDbVocabulary(aVocabulary);
		orm.update(DEFAULT_INDEX, voc, true);
	}

	@Override
	public Vocabulary getVocabulary(final String aUser, final UUID aId) throws PersistException {
		return asVocabulary(orm.get(DEFAULT_INDEX, aId.toString()));
	}

	@Override
	public void shareUnshareVocabulary(final String aUser, final UUID aId, final boolean aShare)
			throws PersistException {
		final DbVocabulary dbVocabulary = orm.get(DEFAULT_INDEX, aId.toString());
		dbVocabulary.setShared(aShare);
		orm.update(DEFAULT_INDEX, dbVocabulary, true);
	}

	@Override
	public Vocabulary queryVocabulary(final String aUser, final String aName) throws PersistException {
		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		final BoolQueryBuilder query = QueryBuilders.boolQuery()
			.must(QueryBuilders.termQuery("user", aUser))
			.must(QueryBuilders.termQuery("name", aName));
		searchSourceBuilder.query(query);

		final SearchResponse search = orm.search(DEFAULT_INDEX, searchSourceBuilder);
		final Iterator<SearchHit> iterator = search.getHits().iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		final SearchHit hit = iterator.next();
		final Map<String, Object> src = hit.getSourceAsMap();
		Language source = LanguageUtils.asLanguage((String) src.get("source"));
		Language target = LanguageUtils.asLanguage((String) src.get("target"));
		source = mLangManager.getLanguage(source.getName(), source.getCountry(), source.getVariant());
		target = mLangManager.getLanguage(target.getName(), target.getCountry(), target.getVariant());
		return new PojoVocabulary(UUID.fromString(hit.getId()),
				(String) src.get("name"),
				(String) src.get("description"),
				(String) src.get("user"),
				source, target,
				(Boolean) src.get("shared"));
	}

	@Override
	public List<Vocabulary> listVocabularies(final String aUser) throws PersistException {
		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.termQuery("user", aUser));

		final SearchResponse search = orm.search(DEFAULT_INDEX, searchSourceBuilder);
		final List<Vocabulary> list = new LinkedList<>();
		search.getHits()
			.forEach(aItem -> {
				final Map<String, Object> src = aItem.getSourceAsMap();
				try {
					list.add(asVocabulary(orm.map(aItem.getId(), src)));
				} catch (final PersistException e) {
					throw new RuntimeException("Failed to convert " + aItem.getId(), e);
				}
			});
		return list;
	}

	@Override
	public List<Vocabulary> listSharedVocabularies(final String aUser) throws PersistException {
		final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.termQuery("shared", true));

		final SearchResponse search = orm.search(DEFAULT_INDEX, searchSourceBuilder);
		final List<Vocabulary> list = new LinkedList<>();
		search.getHits()
			.forEach(aItem -> {
				final Map<String, Object> src = aItem.getSourceAsMap();
				try {
					list.add(asVocabulary(orm.map(aItem.getId(), src)));
				} catch (final PersistException e) {
					throw new RuntimeException("Failed to convert " + aItem.getId(), e);
				}
			});
		return list;
	}

	@Override
	public void deleteVocabulary(final String aUser, final UUID aId) throws PersistException {
		orm.delete(DEFAULT_INDEX, aId.toString());
	}

	private Vocabulary asVocabulary(final DbVocabulary aDbCardVocabulary) throws PersistException {
		if (aDbCardVocabulary == null) {
			return null;
		}
		Language source = LanguageUtils.asLanguage(aDbCardVocabulary.getSource());
		Language target = LanguageUtils.asLanguage(aDbCardVocabulary.getTarget());
		source = mLangManager.getLanguage(source.getName(), source.getCountry(), source.getVariant());
		target = mLangManager.getLanguage(target.getName(), target.getCountry(), target.getVariant());
		return new PojoVocabulary(aDbCardVocabulary.getId(), aDbCardVocabulary.getName(),
				aDbCardVocabulary.getDescription(),
				aDbCardVocabulary.getUser(), source, target, Optional.ofNullable(aDbCardVocabulary.isShared())
				.orElse(Boolean.FALSE));
	}

	private DbVocabulary asDbVocabulary(final Vocabulary aVocabulary) {
		final String source = LanguageUtils.asString(aVocabulary.getSource());
		final String target = LanguageUtils.asString(aVocabulary.getTarget());
		return new DbVocabulary(aVocabulary.getId(), aVocabulary.getName(), aVocabulary.getDescription(),
				aVocabulary.getUser(), source, target, aVocabulary.isShared());
	}

}
