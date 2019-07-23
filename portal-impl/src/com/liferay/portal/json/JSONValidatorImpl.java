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

package com.liferay.portal.json;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONValidator;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Pablo Carvalho
 * @deprecated As of Judson (7.1.x), with no direct replacement
 */
@Deprecated
public class JSONValidatorImpl implements JSONValidator {

	public JSONValidatorImpl(String json) {
	}

	@Override
	public boolean isValid(String json) {
		if (Validator.isNull(json)) {
			return false;
		}

		try {
			json = json.trim();

			if (json.startsWith("{")) {
				new JSONObjectImpl(json);

				return true;
			}
			else if (json.startsWith("[")) {
				new JSONArrayImpl(json);

				return true;
			}
		}
		catch (JSONException jsone) {
		}

		return false;
	}

}