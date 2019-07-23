/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.talend.runtime.writer;

import com.liferay.talend.common.schema.SchemaUtils;
import com.liferay.talend.common.schema.constants.SchemaConstants;
import com.liferay.talend.runtime.LiferaySink;
import com.liferay.talend.tliferayoutput.Action;
import com.liferay.talend.tliferayoutput.TLiferayOutputProperties;

import java.io.IOException;
import java.io.StringReader;

import java.net.URI;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.avro.converter.AvroConverter;
import org.talend.daikon.avro.converter.string.StringStringConverter;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.daikon.i18n.GlobalI18N;
import org.talend.daikon.i18n.I18nMessageProvider;
import org.talend.daikon.i18n.I18nMessages;

/**
 * @author Zoltán Takács
 * @author Igor Beslic
 */
public class LiferayWriter
	implements WriterWithFeedback<Result, IndexedRecord, IndexedRecord> {

	public LiferayWriter(
		LiferayWriteOperation writeOperation, RuntimeContainer runtimeContainer,
		TLiferayOutputProperties tLiferayOutputProperties) {

		_liferayWriteOperation = writeOperation;
		_runtimeContainer = runtimeContainer;
		_tLiferayOutputProperties = tLiferayOutputProperties;

		_dieOnError = tLiferayOutputProperties.dieOnError.getValue();
		_liferaySink = writeOperation.getSink();
		_rejectWrites = new ArrayList<>();
		_rejectSchema = SchemaUtils.createRejectSchema(
			tLiferayOutputProperties.getSchema());
		_successWrites = new ArrayList<>();
	}

	@Override
	public void cleanWrites() {
		_successWrites.clear();
		_rejectWrites.clear();
	}

	@Override
	public Result close() throws IOException {
		return _result;
	}

	public void doDelete(IndexedRecord indexedRecord) throws IOException {
		URI resourceURI = _tLiferayOutputProperties.resource.getEndpointURI();

		_liferaySink.doDeleteRequest(
			_runtimeContainer, resourceURI.toASCIIString());
	}

	public void doInsert(IndexedRecord indexedRecord) throws IOException {
		URI resourceURI = _tLiferayOutputProperties.resource.getEndpointURI();

		_liferaySink.doPostRequest(
			_runtimeContainer, resourceURI.toASCIIString(),
			_createEndpointRequestPayload(indexedRecord));
	}

	public void doUpdate(IndexedRecord indexedRecord) throws IOException {
		URI resourceURI = _tLiferayOutputProperties.resource.getEndpointURI();

		_liferaySink.doPatchRequest(
			_runtimeContainer, resourceURI.toASCIIString(),
			_createEndpointRequestPayload(indexedRecord));
	}

	@Override
	public Iterable<IndexedRecord> getRejectedWrites() {
		return Collections.unmodifiableCollection(_rejectWrites);
	}

	@Override
	public Iterable<IndexedRecord> getSuccessfulWrites() {
		return Collections.unmodifiableCollection(_successWrites);
	}

	@Override
	public WriteOperation<Result> getWriteOperation() {
		return _liferayWriteOperation;
	}

	@Override
	public void open(String uId) throws IOException {
		_result = new Result(uId);
	}

	@Override
	public void write(Object object) throws IOException {
		if (!(object instanceof IndexedRecord)) {
			IllegalArgumentException iae = new IllegalArgumentException(
				"Method argument is null");

			if (object != null) {
				iae = new IllegalArgumentException(
					String.format(
						"Method expected argument instance of %s but actual " +
							"instance passed was %s",
						IndexedRecord.class, object.getClass()));
			}

			_handleFailedIndexedRecord(iae);

			_logger.error("Unable to write record " + object);

			return;
		}

		IndexedRecord indexedRecord = (IndexedRecord)object;
		cleanWrites();

		Action action = _tLiferayOutputProperties.getConfiguredAction();

		try {
			if (Action.Delete == action) {
				doDelete(indexedRecord);
			}
			else if (Action.Insert == action) {
				doInsert(indexedRecord);
			}
			else if (Action.Update == action) {
				doUpdate(indexedRecord);
			}
			else {
				throw TalendRuntimeException.createUnexpectedException(
					"Unsupported write action " + action);
			}

			_handleSuccessRecord(indexedRecord);
		}
		catch (Exception e) {
			if (_logger.isDebugEnabled()) {
				_logger.debug(e.getMessage(), e);
			}

			_handleFailedIndexedRecord(indexedRecord, e);
		}

		_result.totalCount++;
	}

	protected static final I18nMessages i18nMessages;

	static {
		I18nMessageProvider i18nMessageProvider =
			GlobalI18N.getI18nMessageProvider();

		i18nMessages = i18nMessageProvider.getI18nMessages(LiferayWriter.class);
	}

	private JsonObject _createEndpointRequestPayload(
			IndexedRecord indexedRecord)
		throws IOException {

		Schema schema = _tLiferayOutputProperties.getSchema();

		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

		Map<String, JsonObjectBuilder> nestedJsonObjectBuilders =
			new HashMap<>();

		for (Schema.Field field : schema.getFields()) {
			String fieldName = field.name();

			Schema.Field dataField = _getByName(fieldName, indexedRecord);

			if ((dataField == null) ||
				(indexedRecord.get(dataField.pos()) == null)) {

				if (!AvroUtils.isNullable(field.schema())) {
					_handleFailedIndexedRecord(
						indexedRecord,
						new UnsupportedOperationException(
							"Missing required field " + fieldName));
				}

				continue;
			}

			Schema fieldSchema = AvroUtils.unwrapIfNullable(field.schema());

			JsonObjectBuilder currentJsonObjectBuilder = objectBuilder;

			if (_isNestedFieldName(fieldName)) {
				String[] nameParts = fieldName.split("_");

				if (!nestedJsonObjectBuilders.containsKey(nameParts[0])) {
					nestedJsonObjectBuilders.put(
						nameParts[0], Json.createObjectBuilder());
				}

				currentJsonObjectBuilder = nestedJsonObjectBuilders.get(
					nameParts[0]);

				fieldName = nameParts[1];
			}

			int fieldPos = dataField.pos();

			if (AvroUtils.isSameType(fieldSchema, AvroUtils._boolean())) {
				currentJsonObjectBuilder.add(
					fieldName, (boolean)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._bytes())) {
				Base64.Encoder encoder = Base64.getEncoder();

				currentJsonObjectBuilder.add(
					fieldName,
					encoder.encodeToString(
						(byte[])indexedRecord.get(fieldPos)));
			}
			else if (AvroUtils.isSameType(
						fieldSchema, AvroUtils._logicalTimestamp()) ||
					 AvroUtils.isSameType(fieldSchema, AvroUtils._date())) {

				currentJsonObjectBuilder.add(
					fieldName, (Long)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._double())) {
				currentJsonObjectBuilder.add(
					fieldName, (double)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._float())) {
				currentJsonObjectBuilder.add(
					fieldName, (float)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._int())) {
				currentJsonObjectBuilder.add(
					fieldName, (int)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._long())) {
				currentJsonObjectBuilder.add(
					fieldName, (long)indexedRecord.get(fieldPos));
			}
			else if (AvroUtils.isSameType(fieldSchema, AvroUtils._string())) {
				String stringFieldValue = (String)indexedRecord.get(fieldPos);

				if (Objects.equals("true", field.getProp("oas.dictionary")) ||
					Objects.equals("Dictionary", fieldSchema.getName()) ||
					_isJsonObjectFormattedString(stringFieldValue) ||
					_isJsonArrayFormattedString(stringFieldValue)) {

					StringReader stringReader = new StringReader(
						stringFieldValue);

					JsonReader jsonReader = Json.createReader(stringReader);

					currentJsonObjectBuilder.add(
						fieldName, jsonReader.readValue());

					jsonReader.close();

					continue;
				}

				currentJsonObjectBuilder.add(fieldName, stringFieldValue);
			}
			else {
				_handleFailedIndexedRecord(
					indexedRecord,
					new UnsupportedOperationException(
						i18nMessages.getMessage(
							"error.unsupported.field.schema", fieldName,
							fieldSchema.getType())));
			}
		}

		for (Map.Entry<String, JsonObjectBuilder> nestedJsonObjectBuilder :
				nestedJsonObjectBuilders.entrySet()) {

			objectBuilder.add(
				nestedJsonObjectBuilder.getKey(),
				nestedJsonObjectBuilder.getValue());
		}

		return objectBuilder.build();
	}

	private Schema.Field _getByName(String name, IndexedRecord indexedRecord) {
		return SchemaUtils.getField(name, indexedRecord.getSchema());
	}

	private void _handleFailedIndexedRecord(Exception exception)
		throws IOException {

		_handleFailedIndexedRecord(null, exception);
	}

	private void _handleFailedIndexedRecord(
			IndexedRecord failedIndexedRecord, Exception exception)
		throws IOException {

		if (_dieOnError) {
			throw new IOException(exception);
		}

		_result.rejectCount++;

		if (failedIndexedRecord == null) {
			_logger.error("Unable to reject null instance of indexed record");

			return;
		}

		Schema failedIndexedRecordSchema = failedIndexedRecord.getSchema();

		if (!_matchesRejectSchemaSize(failedIndexedRecordSchema)) {
			_logger.error("Reject schema was not setup properly");

			return;
		}

		List<Schema.Field> failedIndexedRecordSchemaFields =
			failedIndexedRecordSchema.getFields();

		IndexedRecord rejectIndexedRecord = new GenericData.Record(
			_rejectSchema);

		for (Schema.Field field : failedIndexedRecordSchemaFields) {
			Schema.Field rejectField = _rejectSchema.getField(field.name());

			if (rejectField != null) {
				int pos = rejectField.pos();

				rejectIndexedRecord.put(
					pos, failedIndexedRecord.get(field.pos()));
			}
		}

		Schema.Field errorField = _rejectSchema.getField(
			SchemaConstants.FIELD_ERROR_MESSAGE);

		rejectIndexedRecord.put(
			errorField.pos(),
			_stringStringConverter.convertToAvro(exception.getMessage()));

		_rejectWrites.add(rejectIndexedRecord);
	}

	private void _handleSuccessRecord(IndexedRecord indexedRecord) {
		_result.successCount++;
		_successWrites.add(indexedRecord);
	}

	private boolean _isJsonArrayFormattedString(String value) {
		if (value.startsWith("[") && value.endsWith("]")) {
			return true;
		}

		return false;
	}

	private boolean _isJsonObjectFormattedString(String value) {
		if (value.startsWith("{") && value.endsWith("}")) {
			return true;
		}

		return false;
	}

	private boolean _isNestedFieldName(String fieldName) {
		if (fieldName.contains("_")) {
			return true;
		}

		return false;
	}

	private boolean _matchesRejectSchemaSize(Schema failedIndexedRecordSchema) {
		List<Schema.Field> rejectSchemaFields = _rejectSchema.getFields();

		if (rejectSchemaFields.isEmpty()) {
			return false;
		}

		List<Schema.Field> failedIndexedRecordSchemaFields =
			failedIndexedRecordSchema.getFields();

		int availableFieldsCount =
			failedIndexedRecordSchemaFields.size() +
				_tLiferayOutputProperties.getRejectSchemaExtraFieldsCount();

		if (availableFieldsCount != rejectSchemaFields.size()) {
			return false;
		}

		return true;
	}

	private static final Logger _logger = LoggerFactory.getLogger(
		LiferayWriter.class);

	private static final AvroConverter<String, String> _stringStringConverter =
		new StringStringConverter();

	private final boolean _dieOnError;
	private final LiferaySink _liferaySink;
	private final LiferayWriteOperation _liferayWriteOperation;
	private final Schema _rejectSchema;
	private final List<IndexedRecord> _rejectWrites;
	private Result _result;
	private final RuntimeContainer _runtimeContainer;
	private final List<IndexedRecord> _successWrites;
	private final TLiferayOutputProperties _tLiferayOutputProperties;

}