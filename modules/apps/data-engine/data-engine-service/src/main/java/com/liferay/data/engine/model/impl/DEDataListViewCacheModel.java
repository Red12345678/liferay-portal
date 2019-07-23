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

package com.liferay.data.engine.model.impl;

import com.liferay.data.engine.model.DEDataListView;
import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.CacheModel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Date;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The cache model class for representing DEDataListView in entity cache.
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
@ProviderType
public class DEDataListViewCacheModel
	implements CacheModel<DEDataListView>, Externalizable {

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof DEDataListViewCacheModel)) {
			return false;
		}

		DEDataListViewCacheModel deDataListViewCacheModel =
			(DEDataListViewCacheModel)obj;

		if (deDataListViewId == deDataListViewCacheModel.deDataListViewId) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return HashUtil.hash(0, deDataListViewId);
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(23);

		sb.append("{uuid=");
		sb.append(uuid);
		sb.append(", deDataListViewId=");
		sb.append(deDataListViewId);
		sb.append(", groupId=");
		sb.append(groupId);
		sb.append(", companyId=");
		sb.append(companyId);
		sb.append(", userId=");
		sb.append(userId);
		sb.append(", userName=");
		sb.append(userName);
		sb.append(", createDate=");
		sb.append(createDate);
		sb.append(", modifiedDate=");
		sb.append(modifiedDate);
		sb.append(", deDataRecordQueryId=");
		sb.append(deDataRecordQueryId);
		sb.append(", ddmStructureId=");
		sb.append(ddmStructureId);
		sb.append(", name=");
		sb.append(name);
		sb.append("}");

		return sb.toString();
	}

	@Override
	public DEDataListView toEntityModel() {
		DEDataListViewImpl deDataListViewImpl = new DEDataListViewImpl();

		if (uuid == null) {
			deDataListViewImpl.setUuid("");
		}
		else {
			deDataListViewImpl.setUuid(uuid);
		}

		deDataListViewImpl.setDeDataListViewId(deDataListViewId);
		deDataListViewImpl.setGroupId(groupId);
		deDataListViewImpl.setCompanyId(companyId);
		deDataListViewImpl.setUserId(userId);

		if (userName == null) {
			deDataListViewImpl.setUserName("");
		}
		else {
			deDataListViewImpl.setUserName(userName);
		}

		if (createDate == Long.MIN_VALUE) {
			deDataListViewImpl.setCreateDate(null);
		}
		else {
			deDataListViewImpl.setCreateDate(new Date(createDate));
		}

		if (modifiedDate == Long.MIN_VALUE) {
			deDataListViewImpl.setModifiedDate(null);
		}
		else {
			deDataListViewImpl.setModifiedDate(new Date(modifiedDate));
		}

		deDataListViewImpl.setDeDataRecordQueryId(deDataRecordQueryId);
		deDataListViewImpl.setDdmStructureId(ddmStructureId);

		if (name == null) {
			deDataListViewImpl.setName("");
		}
		else {
			deDataListViewImpl.setName(name);
		}

		deDataListViewImpl.resetOriginalValues();

		return deDataListViewImpl;
	}

	@Override
	public void readExternal(ObjectInput objectInput) throws IOException {
		uuid = objectInput.readUTF();

		deDataListViewId = objectInput.readLong();

		groupId = objectInput.readLong();

		companyId = objectInput.readLong();

		userId = objectInput.readLong();
		userName = objectInput.readUTF();
		createDate = objectInput.readLong();
		modifiedDate = objectInput.readLong();

		deDataRecordQueryId = objectInput.readLong();

		ddmStructureId = objectInput.readLong();
		name = objectInput.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput objectOutput) throws IOException {
		if (uuid == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(uuid);
		}

		objectOutput.writeLong(deDataListViewId);

		objectOutput.writeLong(groupId);

		objectOutput.writeLong(companyId);

		objectOutput.writeLong(userId);

		if (userName == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(userName);
		}

		objectOutput.writeLong(createDate);
		objectOutput.writeLong(modifiedDate);

		objectOutput.writeLong(deDataRecordQueryId);

		objectOutput.writeLong(ddmStructureId);

		if (name == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(name);
		}
	}

	public String uuid;
	public long deDataListViewId;
	public long groupId;
	public long companyId;
	public long userId;
	public String userName;
	public long createDate;
	public long modifiedDate;
	public long deDataRecordQueryId;
	public long ddmStructureId;
	public String name;

}