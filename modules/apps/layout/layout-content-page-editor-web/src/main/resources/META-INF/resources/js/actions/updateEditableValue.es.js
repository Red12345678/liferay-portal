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

import {
	disableSavingChangesStatusAction,
	enableSavingChangesStatusAction,
	updateLastSaveDateAction
} from './saveChanges.es';
import {
	EDITABLE_FRAGMENT_ENTRY_PROCESSOR,
	FREEMARKER_FRAGMENT_ENTRY_PROCESSOR
} from '../utils/constants';
import {
	deleteIn,
	setIn,
	updateIn
} from '../utils/FragmentsEditorUpdateUtils.es';
import {
	UPDATE_EDITABLE_VALUE_ERROR,
	UPDATE_EDITABLE_VALUE_LOADING,
	UPDATE_EDITABLE_VALUE_SUCCESS,
	UPDATE_FRAGMENT_ENTRY_LINK_CONTENT
} from './actions.es';
import {updateEditableValues} from '../utils/FragmentsEditorFetchUtils.es';
import debouncedAlert from '../utils/debouncedAlert.es';
import {prefixSegmentsExperienceId} from '../utils/prefixSegmentsExperienceId.es';
import {getFragmentEntryLinkContent} from '../reducers/fragments.es';

/**
 * @type {number}
 */
const UPDATE_EDITABLE_VALUES_DELAY = 1500;

/**
 * @param {function} dispatch
 * @param {string} fragmentEntryLinkId
 * @param {object} previousEditableValues
 * @param {object} nextEditableValues
 * @review
 */
const debouncedUpdateEditableValues = debouncedAlert(
	/**
	 * @param {function} dispatch
	 * @param {string} fragmentEntryLinkId
	 * @param {object} previousEditableValues
	 * @param {object} nextEditableValues
	 * @review
	 */
	(
		dispatch,
		fragmentEntryLinkId,
		previousEditableValues,
		nextEditableValues
	) => {
		updateEditableValues(fragmentEntryLinkId, nextEditableValues)
			.then(() => {
				dispatch(updateEditableValueSuccessAction());
				dispatch(disableSavingChangesStatusAction());
				dispatch(updateLastSaveDateAction());
			})
			.catch(() => {
				dispatch(
					updateEditableValueErrorAction(
						fragmentEntryLinkId,
						previousEditableValues
					)
				);

				dispatch(disableSavingChangesStatusAction());
			});
	},

	UPDATE_EDITABLE_VALUES_DELAY
);

/**
 * @param {number} fragmentEntryLinkId
 * @param {object} configurationValues
 * @param {number} segmentsExperienceId
 * @review
 */
function updateConfigurationValueAction(
	fragmentEntryLinkId,
	configurationValues,
	segmentsExperienceId
) {
	return function(dispatch, getState) {
		const state = getState();

		const prefixedSegmentsExperienceId = prefixSegmentsExperienceId(
			segmentsExperienceId
		);

		const previousEditableValues =
			state.fragmentEntryLinks[fragmentEntryLinkId].editableValues;

		const keyPath = prefixedSegmentsExperienceId
			? [
					FREEMARKER_FRAGMENT_ENTRY_PROCESSOR,
					prefixedSegmentsExperienceId
			  ]
			: [FREEMARKER_FRAGMENT_ENTRY_PROCESSOR];

		const nextEditableValues = setIn(
			previousEditableValues,
			keyPath,
			configurationValues
		);

		dispatch(
			updateEditableValueLoadingAction(
				fragmentEntryLinkId,
				nextEditableValues
			)
		);

		dispatch(enableSavingChangesStatusAction());

		updateEditableValues(fragmentEntryLinkId, nextEditableValues)
			.then(() => {
				dispatch(updateEditableValueSuccessAction());
				dispatch(disableSavingChangesStatusAction());
				dispatch(updateLastSaveDateAction());
				dispatch(
					updateFragmentEntryLinkContent(
						fragmentEntryLinkId,
						segmentsExperienceId
					)
				);
			})
			.catch(() => {
				dispatch(
					updateEditableValueErrorAction(
						fragmentEntryLinkId,
						previousEditableValues
					)
				);

				dispatch(disableSavingChangesStatusAction());
			});
	};
}

/**
 * @param {!object} data
 * @param {!string} data.fragmentEntryLinkId
 * @param {!string} data.editableValueContent
 * @param {!string} data.processor
 * @param {string} data.editableId
 * @param {string} data.editableValueId
 * @param {string} data.segmentsExperienceId
 * @return {function}
 * @review
 */
function updateEditableValueAction(data) {
	return updateEditableValuesAction(
		data.fragmentEntryLinkId,
		data.editableId,
		[
			{
				content: data.editableValueContent,
				editableValueId: data.editableValueId
			}
		],
		data.segmentsExperienceId,
		data.processor
	);
}

/**
 * @param {string} fragmentEntryLinkId
 * @param {string} editableId
 * @param {Array<{editableValueId: string, content: string}>} editableValues
 * @param {string} [editableValueSegmentsExperienceId='']
 * @return {function}
 * @review
 */
function updateEditableValuesAction(
	fragmentEntryLinkId,
	editableId,
	editableValues,
	editableValueSegmentsExperienceId = '',
	processor = EDITABLE_FRAGMENT_ENTRY_PROCESSOR
) {
	return function(dispatch, getState) {
		const state = getState();

		const previousEditableValues =
			state.fragmentEntryLinks[fragmentEntryLinkId].editableValues;

		let keysTreeArray = [processor];

		if (editableId) {
			keysTreeArray = [...keysTreeArray, editableId];
		}

		if (editableValueSegmentsExperienceId) {
			keysTreeArray = [
				...keysTreeArray,
				editableValueSegmentsExperienceId
			];
		}

		let nextEditableValues = previousEditableValues;

		editableValues.forEach(editableValue => {
			if (!editableValue.content) {
				nextEditableValues = deleteIn(
					nextEditableValues,
					editableValue.editableValueId
						? [...keysTreeArray, editableValue.editableValueId]
						: keysTreeArray
				);
			} else {
				nextEditableValues = setIn(
					nextEditableValues,
					editableValue.editableValueId
						? [...keysTreeArray, editableValue.editableValueId]
						: keysTreeArray,
					editableValue.content
				);
			}

			if (editableValue.editableValueId === 'mappedField') {
				nextEditableValues = updateIn(
					nextEditableValues,
					keysTreeArray,
					previousEditableValue => {
						const nextEditableValue = Object.assign(
							{},
							previousEditableValue
						);

						[
							'config',
							state.defaultSegmentsEntryId,
							...Object.keys(state.availableLanguages),
							...Object.keys(state.availableSegmentsEntries)
						].forEach(key => {
							delete nextEditableValue[key];
						});

						return nextEditableValue;
					}
				);
			}
		});

		dispatch(
			updateEditableValueLoadingAction(
				fragmentEntryLinkId,
				nextEditableValues
			)
		);

		dispatch(enableSavingChangesStatusAction());

		debouncedUpdateEditableValues(
			dispatch,
			fragmentEntryLinkId,
			previousEditableValues,
			nextEditableValues
		);
	};
}

/**
 * @param {string} fragmentEntryLinkId
 * @param {object} editableValues
 * @param {Date} [date=new Date()]
 * @return {object}
 * @review
 */
function updateEditableValueErrorAction(
	fragmentEntryLinkId,
	editableValues,
	date = new Date()
) {
	return {
		date,
		editableValues,
		fragmentEntryLinkId,
		type: UPDATE_EDITABLE_VALUE_ERROR
	};
}

/**
 * @param {string} fragmentEntryLinkId
 * @param {object} editableValues
 * @return {object}
 * @review
 */
function updateEditableValueLoadingAction(fragmentEntryLinkId, editableValues) {
	return {
		editableValues,
		fragmentEntryLinkId,
		type: UPDATE_EDITABLE_VALUE_LOADING
	};
}

/**
 * @param {Date} [date=new Date()]
 * @return {object}
 * @review
 */
function updateEditableValueSuccessAction(date = new Date()) {
	return {
		date,
		type: UPDATE_EDITABLE_VALUE_SUCCESS
	};
}

/**
 * @param {number} fragmentEntryLinkId
 * @param {number} segmentsExperienceId
 * @review
 */
function updateFragmentEntryLinkContent(
	fragmentEntryLinkId,
	segmentsExperienceId
) {
	return function(dispatch, getState) {
		const state = getState();

		const fragmentEntryLink = state.fragmentEntryLinks[fragmentEntryLinkId];

		getFragmentEntryLinkContent(
			state.renderFragmentEntryURL,
			fragmentEntryLink,
			state.portletNamespace,
			segmentsExperienceId
		).then(response => {
			const {fragmentEntryLinkId, content} = response;

			dispatch({
				fragmentEntryLinkContent: content,
				fragmentEntryLinkId,
				type: UPDATE_FRAGMENT_ENTRY_LINK_CONTENT
			});
		});
	};
}

export {
	updateConfigurationValueAction,
	updateFragmentEntryLinkContent,
	updateEditableValueAction,
	updateEditableValuesAction,
	updateEditableValueSuccessAction
};
