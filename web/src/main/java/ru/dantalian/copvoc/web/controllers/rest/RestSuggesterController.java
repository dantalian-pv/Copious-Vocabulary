package ru.dantalian.copvoc.web.controllers.rest;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ru.dantalian.copvoc.suggester.api.SuggestException;
import ru.dantalian.copvoc.suggester.api.SuggestQueryBuilder;
import ru.dantalian.copvoc.suggester.api.SuggestQueryType;
import ru.dantalian.copvoc.suggester.api.Suggester;
import ru.dantalian.copvoc.suggester.api.model.Suggest;
import ru.dantalian.copvoc.suggester.api.query.SuggestQueryFactory;
import ru.dantalian.copvoc.web.controllers.rest.model.DtoSuggest;

@RestController
@RequestMapping(value = "/v1/api/suggester", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestSuggesterController {

	@Resource(name = "root")
	private Suggester suggester;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<DtoSuggest> suggest(final Principal aPrincipal,
			@RequestParam(value = "key", required = false) final String aKey,
			@RequestParam(value = "value", required = true) final String aValue,
			@RequestParam(value = "notKey", required = false) final String aNotKey,
			@RequestParam(value = "notValue", required = false) final String aNotValue,
			@RequestParam(value = "source", required = true) final String aSource,
			@RequestParam(value = "target", required = true) final String aTarget,
			@RequestParam(value = "type", required = false) final String aType)
			throws RestException {
		try {
			if (aValue == null || aValue.isEmpty()) {
				return Collections.emptyList();
			}
			final String user = aPrincipal.getName();
			final SuggestQueryBuilder builder = SuggestQueryFactory.newBuilder();
			if (aType != null && !aType.isEmpty()) {
				try {
					final SuggestQueryType type = SuggestQueryType.valueOf(aType.toUpperCase());
					builder.setType(type);
				} catch (final IllegalArgumentException e) {
					// just ignore
				}
			}
			builder.where(aKey, aValue);
			if (aNotKey != null && aNotValue != null) {
				builder.not(aNotKey, aNotValue);
			}
			builder.setSourceTarget(aSource, aTarget);
			final List<Suggest> suggests = suggester.suggest(user, builder.build());
			final List<DtoSuggest> list = new LinkedList<>();
			for (final Suggest sugg: suggests) {
				list.add(asDtoSuggest(sugg));
			}
			return list;
		} catch (final SuggestException e) {
			throw new RestException(e.getMessage(), e);
		}
	}

	private DtoSuggest asDtoSuggest(final Suggest aSuggest) {
		return new DtoSuggest(aSuggest.getSource().toString(),
				aSuggest.getGroup(),
				aSuggest.getKey(), aSuggest.getValue(), aSuggest.getDescription(), aSuggest.getRank());
	}

}
