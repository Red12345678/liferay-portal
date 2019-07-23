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

package com.liferay.layout.content.page.editor.web.internal.asset.model;

import com.liferay.asset.kernel.model.AssetRenderer;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.kernel.model.BaseAssetRenderer;
import com.liferay.asset.kernel.model.BaseAssetRendererFactory;
import com.liferay.fragment.contributor.FragmentCollectionContributorTracker;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererTracker;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.layout.content.page.editor.constants.ContentPageEditorPortletKeys;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.Validator;

import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(
	immediate = true,
	property = "javax.portlet.name=" + ContentPageEditorPortletKeys.CONTENT_PAGE_EDITOR_PORTLET,
	service = AssetRendererFactory.class
)
public class FragmentEntryLinkAssetRendererFactory
	extends BaseAssetRendererFactory<FragmentEntryLink> {

	public static final String TYPE = "fragmentEntryLink";

	public FragmentEntryLinkAssetRendererFactory() {
		setCategorizable(false);
		setClassName(FragmentEntryLink.class.getName());
		setLinkable(false);
		setPortletId(ContentPageEditorPortletKeys.CONTENT_PAGE_EDITOR_PORTLET);
		setSearchable(false);
		setSelectable(false);
	}

	@Override
	public AssetRenderer<FragmentEntryLink> getAssetRenderer(
			long classPK, int type)
		throws PortalException {

		return new FragmentEntryLinkAssetRenderer(
			_fragmentEntryLinkLocalService.getFragmentEntryLink(classPK));
	}

	@Override
	public String getType() {
		return TYPE;
	}

	public class FragmentEntryLinkAssetRenderer
		extends BaseAssetRenderer<FragmentEntryLink> {

		public FragmentEntryLinkAssetRenderer(
			FragmentEntryLink fragmentEntryLink) {

			_fragmentEntryLink = fragmentEntryLink;
		}

		@Override
		public FragmentEntryLink getAssetObject() {
			return _fragmentEntryLink;
		}

		@Override
		public String getClassName() {
			return FragmentEntryLink.class.getName();
		}

		@Override
		public long getClassPK() {
			return _fragmentEntryLink.getFragmentEntryLinkId();
		}

		@Override
		public long getGroupId() {
			return _fragmentEntryLink.getGroupId();
		}

		@Override
		public String getSummary(
			PortletRequest portletRequest, PortletResponse portletResponse) {

			return null;
		}

		@Override
		public String getTitle(Locale locale) {
			FragmentEntry fragmentEntry =
				_fragmentEntryLocalService.fetchFragmentEntry(
					_fragmentEntryLink.getFragmentEntryId());

			if (fragmentEntry != null) {
				return fragmentEntry.getName();
			}

			String rendererKey = _fragmentEntryLink.getRendererKey();

			if (Validator.isNull(rendererKey)) {
				return StringPool.BLANK;
			}

			Map<String, FragmentEntry> fragmentEntries =
				_fragmentCollectionContributorTracker.getFragmentEntries();

			FragmentEntry contributedFragmentEntry = fragmentEntries.get(
				rendererKey);

			if (contributedFragmentEntry != null) {
				return contributedFragmentEntry.getName();
			}

			FragmentRenderer fragmentRenderer =
				_fragmentRendererTracker.getFragmentRenderer(
					_fragmentEntryLink.getRendererKey());

			if (fragmentRenderer != null) {
				return fragmentRenderer.getLabel(locale);
			}

			return StringPool.BLANK;
		}

		@Override
		public long getUserId() {
			return _fragmentEntryLink.getUserId();
		}

		@Override
		public String getUserName() {
			return _fragmentEntryLink.getUserName();
		}

		@Override
		public String getUuid() {
			return _fragmentEntryLink.getUuid();
		}

		@Override
		public boolean include(
				HttpServletRequest httpServletRequest,
				HttpServletResponse httpServletResponse, String template)
			throws Exception {

			return false;
		}

		private final FragmentEntryLink _fragmentEntryLink;

	}

	@Reference
	private FragmentCollectionContributorTracker
		_fragmentCollectionContributorTracker;

	@Reference
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Reference
	private FragmentEntryLocalService _fragmentEntryLocalService;

	@Reference
	private FragmentRendererTracker _fragmentRendererTracker;

}