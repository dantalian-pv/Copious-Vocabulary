package ru.dantalian.copvoc.suggester.combined;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ru.dantalian.copvoc.persist.api.PersistCardFieldManager;
import ru.dantalian.copvoc.persist.api.PersistCardManager;
import ru.dantalian.copvoc.persist.api.PersistException;
import ru.dantalian.copvoc.persist.api.PersistVocabularyManager;
import ru.dantalian.copvoc.persist.api.model.Card;
import ru.dantalian.copvoc.persist.api.model.CardField;
import ru.dantalian.copvoc.persist.api.model.CardFieldContent;
import ru.dantalian.copvoc.persist.api.model.CardFiledType;
import ru.dantalian.copvoc.persist.api.model.Vocabulary;
import ru.dantalian.copvoc.persist.api.query.BoolExpressionBuilder;
import ru.dantalian.copvoc.persist.api.query.QueryBuilder;
import ru.dantalian.copvoc.persist.api.query.QueryFactory;
import ru.dantalian.copvoc.suggester.api.SuggestException;
import ru.dantalian.copvoc.suggester.api.SuggestQuery;
import ru.dantalian.copvoc.suggester.api.SuggestQueryType;
import ru.dantalian.copvoc.suggester.api.Suggester;
import ru.dantalian.copvoc.suggester.api.model.Pair;
import ru.dantalian.copvoc.suggester.api.model.Suggest;
import ru.dantalian.copvoc.suggester.combined.model.PojoSuggest;

@Component("cards")
@Order(10)
public class CardsSuggester implements Suggester {

	@Autowired
	private PersistCardManager cardManager;

	@Autowired
	private PersistCardFieldManager fieldManager;

	@Autowired
	private PersistVocabularyManager vocManager;

	@Override
	public String getName() {
		return "cards";
	}

	@Override
	public boolean accept(final Pair<String, String> aSourceTarget, final SuggestQueryType aType) {
		return aType == SuggestQueryType.STRING || aType == SuggestQueryType.TEXT;
	}

	@Override
	public List<Suggest> suggest(final String aUser, final SuggestQuery aQuery) throws SuggestException {
		try {
			final List<Suggest> suggests = new LinkedList<>();

			final Set<CardField> fields = new HashSet<>(fieldManager.listFields(aUser, null));
			final String key = aQuery.getWhere().getKey() == null || aQuery.getWhere().getKey().isEmpty()
					? null : aQuery.getWhere().getKey().toLowerCase();
			final QueryBuilder cardsQuery = QueryFactory.newCardsQuery();
			final BoolExpressionBuilder bool = QueryFactory.bool();
			final BoolExpressionBuilder nestedBool = QueryFactory.bool();
			for (final CardField field: fields) {
				if (key == null || field.getName().toLowerCase().contains(key)) {
					addQueryByType(aQuery, nestedBool, field);
				}
			}
			bool.must(nestedBool.build());
			bool.must(QueryFactory.term("sourceLang", aQuery.getSourceTarget().getKey() + "*", true));
			bool.must(QueryFactory.term("targetLang", aQuery.getSourceTarget().getValue() + "*", true));

			final List<String> sharedVocIds = vocManager.listSharedVocabularies(aUser)
				.stream()
				.map(aItem -> aItem.getId().toString())
				.filter(aItem-> {
					if (aQuery.getNot() == null || !"vocabulary_id".equals(aQuery.getNot().getKey())) {
						return true;
					}
					return !aItem.equals(aQuery.getNot().getValue());
				})
				.collect(Collectors.toList());

			bool.must(QueryFactory.terms("vocabulary_id", sharedVocIds));

			cardsQuery.where(bool.build());
			final List<Card> queryCards = cardManager.queryCards(aUser, cardsQuery.build()).getItems();
			for (final Card card: queryCards) {
				suggests.addAll(asSuggest(aUser, card, aQuery.getWhere().getKey(), aQuery.getType()));
			}
			return suggests;
		} catch (final PersistException e) {
			throw new SuggestException("Failed to query cards", e);
		}
	}

	private void addQueryByType(final SuggestQuery aQuery, final BoolExpressionBuilder bool, final CardField field) {
		switch (aQuery.getType()) {
			case FIELD:
				// Just ignore
				break;
			case STRING:
				bool.should(QueryFactory.term("content." + field.getName() + "_keyword", aQuery.getWhere().getValue() + "*", true));
				break;
			case TEXT:
				bool.should(QueryFactory.term("content." + field.getName() + "_text", aQuery.getWhere().getValue() + "*", true));
				break;
			default:
				throw new IllegalArgumentException("Unknown query type: " + aQuery.getType());
		}
	}

	private List<Suggest> asSuggest(final String aUser, final Card aCard, final String aKey, final SuggestQueryType aType)
			throws PersistException {
		final List<Suggest> suggests = new LinkedList<>();
		final Map<String, CardFieldContent> fieldsContent = aCard.getFieldsContent();
		final List<CardField> fields = fieldManager.listFields(aUser, aCard.getVocabularyId());
		CardField answer = null;
		for (final CardField field: fields) {
			if (field.getType() == CardFiledType.ANSWER) {
				answer = field;
				break;
			}
		}
		for (final Entry<String, CardFieldContent> entry: fieldsContent.entrySet()) {
			final String name = entry.getKey();
			if (!name.toLowerCase().contains(aKey.toLowerCase())) {
				continue;
			}
			final CardField field = fieldManager.getField(aUser, aCard.getVocabularyId(), name);
			final CardFieldContent content = entry.getValue();
			if (isFits(aType, field)) {
				final URI uri = URI.create("card://" + aCard.getVocabularyId() + "/" + aCard.getId() + "/" + name);
				final String description = answer != null && fieldsContent.get(answer.getName()) != null
						? fieldsContent.get(answer.getName()).getContent() : "";
				final Vocabulary voc = vocManager.getVocabulary(aUser, aCard.getVocabularyId());
				suggests.add(new PojoSuggest(uri, voc.getName(),
						name, content.getContent(), description, 1.0d));
			}
		}
		return suggests;
	}

	private boolean isFits(final SuggestQueryType aType, final CardField field) {
		switch (aType) {
			case STRING:
				return field.getType() == CardFiledType.ANSWER || field.getType() == CardFiledType.STRING;
			case TEXT:
				return field.getType() == CardFiledType.TEXT || field.getType() == CardFiledType.MARKUP;
			default:
				throw new IllegalArgumentException("Unknown query type " + aType);
		}
	}

}
