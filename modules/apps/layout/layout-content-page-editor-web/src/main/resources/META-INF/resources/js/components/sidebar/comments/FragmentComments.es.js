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

import PropTypes from 'prop-types';
import React from 'react';

import {FRAGMENTS_EDITOR_ITEM_TYPES} from '../../../utils/constants';
import {getConnectedReactComponent} from '../../../store/ConnectedComponent.es';
import {ConnectedAddCommentForm} from './AddCommentForm.es';
import FragmentComment from './FragmentComment.es';

const FragmentComments = props => (
	<div
		data-fragments-editor-item-id={props.fragmentEntryLinkId}
		data-fragments-editor-item-type={FRAGMENTS_EDITOR_ITEM_TYPES.fragment}
	>
		<h2 className="mb-2 sidebar-dt text-secondary">
			{props.fragmentEntryLinkName}
		</h2>

		<ConnectedAddCommentForm
			fragmentEntryLinkId={props.fragmentEntryLinkId}
		/>

		{props.fragmentEntryLinkComments.map(comment => (
			<FragmentComment key={comment.commentId} {...comment} />
		))}
	</div>
);

FragmentComments.propTypes = {
	fragmentEntryLinkId: PropTypes.string.isRequired,
	fragmentEntryLinkName: PropTypes.string
};

const ConnectedFragmentComments = getConnectedReactComponent(
	(state, ownProps) => {
		const fragmentEntryLink =
			state.fragmentEntryLinks[ownProps.fragmentEntryLinkId];

		return {
			fragmentEntryLinkComments: fragmentEntryLink.comments || [],
			fragmentEntryLinkName: fragmentEntryLink.name
		};
	},

	() => ({})
)(FragmentComments);

export {ConnectedFragmentComments, FragmentComments};
export default ConnectedFragmentComments;
