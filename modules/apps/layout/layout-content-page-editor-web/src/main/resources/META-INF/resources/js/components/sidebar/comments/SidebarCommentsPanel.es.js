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

import {Component} from 'metal-component';
import React from 'react';
import ReactDOM from 'react-dom';
import Soy from 'metal-soy';

import {getConnectedComponent} from '../../../store/ConnectedComponent.es';
import {ConnectedSidebarComments} from './SidebarComments.es';
import {StoreContext} from '../../../store/StoreContext.es';
import templates from './SidebarCommentsPanel.soy';

class SidebarCommentsPanel extends Component {
	disposed() {
		ReactDOM.unmountComponentAtNode(this.refs.app);
	}

	syncStore(store, prevStore) {
		if (store && store !== prevStore) {
			this._mountApp();
		}
	}

	_mountApp() {
		ReactDOM.unmountComponentAtNode(this.refs.app);

		ReactDOM.render(
			<StoreContext.Provider value={this.store}>
				<ConnectedSidebarComments />
			</StoreContext.Provider>,
			this.refs.app
		);
	}
}

const ConnectedSidebarCommentsPanel = getConnectedComponent(
	SidebarCommentsPanel,
	[]
);

Soy.register(ConnectedSidebarCommentsPanel, templates);

export {ConnectedSidebarCommentsPanel, SidebarCommentsPanel};
export default ConnectedSidebarCommentsPanel;
