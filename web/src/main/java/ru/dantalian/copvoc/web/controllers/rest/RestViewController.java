package ru.dantalian.copvoc.web.controllers.rest;

import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ru.dantalian.copvoc.persist.api.PersistException;
import ru.dantalian.copvoc.persist.api.PersistVocabularyManager;
import ru.dantalian.copvoc.persist.api.PersistVocabularyViewManager;
import ru.dantalian.copvoc.persist.api.model.Vocabulary;
import ru.dantalian.copvoc.persist.api.model.VocabularyView;
import ru.dantalian.copvoc.web.controllers.rest.model.DtoView;
import ru.dantalian.copvoc.web.controllers.rest.model.DtoVoid;
import ru.dantalian.copvoc.web.utils.DtoCodec;

@RestController
@RequestMapping(value = "/v1/api/views", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestViewController {

	@Autowired
	private PersistVocabularyManager vocPersist;

	@Autowired
	private PersistVocabularyViewManager viewPersist;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public DtoView getView(final Principal aPrincipal,
			@PathVariable(value = "id") final String aId)
			throws RestException {
		try {
			final String user = aPrincipal.getName();
			final Vocabulary voc = vocPersist.getVocabulary(user, UUID.fromString(aId));
			if (voc == null) {
				throw new PersistException("Vocabulary with id: " + aId + " not found");
			}
			final VocabularyView view = viewPersist.getVocabularyView(user, UUID.fromString(aId));
			return DtoCodec.asDtoView(view);
		} catch (final PersistException e) {
			throw new RestException(e.getMessage(), e);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public DtoVoid updateView(final Principal aPrincipal, @RequestBody final DtoView aView)
			throws RestException {
		try {
			final String user = aPrincipal.getName();
			final Vocabulary voc = vocPersist.getVocabulary(user, UUID.fromString(aView.getId()));
			if (voc == null) {
				throw new PersistException("Vocabulary with id: " + aView.getId() + " not found");
			}
			viewPersist.updateVocabularyView(user, UUID.fromString(aView.getId()),
					aView.getCss(), aView.getFront(), aView.getBack());
			return DtoVoid.INSTANCE;
		} catch (final PersistException e) {
			throw new RestException(e.getMessage(), e);
		}
	}

}
