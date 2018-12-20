package ru.dantalian.copvoc.persist.sqlite.managers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import ru.dantalian.copvoc.persist.api.PersistCardFieldManager;
import ru.dantalian.copvoc.persist.api.PersistException;
import ru.dantalian.copvoc.persist.api.model.CardField;
import ru.dantalian.copvoc.persist.api.model.CardFiledType;
import ru.dantalian.copvoc.persist.impl.model.PojoCardField;
import ru.dantalian.copvoc.persist.sqlite.model.DbCardField;
import ru.dantalian.copvoc.persist.sqlite.model.mappers.DbCardFieldMapper;

@Service
public class SqlitePersistCardFieldManager implements PersistCardFieldManager {

	@Autowired
	private JdbcTemplate db;

	@Autowired
	private DbCardFieldMapper mapper;

	@Override
	public CardField createField(final UUID aBatchId, final String aName,
			final CardFiledType aType) throws PersistException {
		try {
			db.update("INSERT INTO card_field (batch_id, name, \"type\") "
					+ " VALUES (?, ?, ?)",
					aBatchId.toString(), aName, aType.name());
			return toCardField(new DbCardField(aBatchId, aName, aType));
		} catch (final DataAccessException e) {
			throw new PersistException("Failed to create a field", e);
		}
	}

	@Override
	public CardField getField(final UUID aBatchId, final String aName) throws PersistException {
		try {
			final List<DbCardField> list = db.query("select * from card_field "
					+ " WHERE batch_id = ?"
					+ " AND name = ?",
					new Object[] {
							aBatchId.toString(),
							aName
					},
					mapper);
			return toCardField(CollectionUtils.lastElement(list));
		} catch (final DataAccessException e) {
			throw new PersistException("Failed to get a field by batch_id: " + aBatchId + " name: " + aName, e);
		}
	}

	@Override
	public void deleteField(final UUID aBatchId, final String aName) throws PersistException {
		try {
			db.update("DELETE FROM card_field "
					+ " WHERE batch_id = ?"
					+ " AND name = ?",
					aBatchId.toString(),
					aName);
		} catch (final DataAccessException e) {
			throw new PersistException("Failed to delete a field", e);
		}
	}

	@Override
	public List<CardField> listFields(final UUID aBatchId) throws PersistException {
		try {
			final List<DbCardField> list = db.query("select * from card_field where batch_id = ?",
					new Object[] {
							aBatchId.toString()
					},
					mapper);
			return list.stream()
					.map(this::toCardField)
					.collect(Collectors.toList());
		} catch (final DataAccessException e) {
			throw new PersistException("Failed to list fields for batchId " + aBatchId, e);
		}
	}

	private CardField toCardField(final DbCardField aDbCardField) {
		return new PojoCardField(aDbCardField.getBatchId(), aDbCardField.getName(),
				aDbCardField.getType());
	}

}
