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

package com.liferay.portal.workflow.kaleo.search;

import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetRenderer;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupGroupRole;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.IndexWriterHelper;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.DateRangeTermFilter;
import com.liferay.portal.kernel.search.filter.TermFilter;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserGroupGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.workflow.kaleo.internal.search.KaleoTaskInstanceTokenField;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskAssignmentInstance;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskInstanceToken;
import com.liferay.portal.workflow.kaleo.service.KaleoTaskInstanceTokenLocalService;
import com.liferay.portal.workflow.kaleo.service.persistence.KaleoTaskInstanceTokenQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alberto Montero
 */
@Component(immediate = true, service = Indexer.class)
public class KaleoTaskInstanceTokenIndexer
	extends BaseIndexer<KaleoTaskInstanceToken> {

	public static final String CLASS_NAME =
		KaleoTaskInstanceToken.class.getName();

	public KaleoTaskInstanceTokenIndexer() {
		setDefaultSelectedFieldNames(
			Field.COMPANY_ID, Field.ENTRY_CLASS_NAME, Field.ENTRY_CLASS_PK,
			Field.UID);
		setDefaultSelectedLocalizedFieldNames(
			KaleoTaskInstanceTokenField.ASSET_DESCRIPTION,
			KaleoTaskInstanceTokenField.ASSET_TITLE);

		setPermissionAware(false);
		setFilterSearch(false);
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	@Override
	public void postProcessContextBooleanFilter(
			BooleanFilter contextBooleanFilter, SearchContext searchContext)
		throws Exception {

		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery =
			(KaleoTaskInstanceTokenQuery)searchContext.getAttribute(
				"kaleoTaskInstanceTokenQuery");

		if (kaleoTaskInstanceTokenQuery == null) {
			return;
		}

		appendAssigneeClassIdsNameTerm(
			contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		appendAssigneeClassPKsTerm(
			contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		appendCompletedTerm(contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		appendKaleoInstanceIdTerm(
			contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		appendRoleIdsTerm(contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		appendSearchByUserRolesTerm(
			contextBooleanFilter, kaleoTaskInstanceTokenQuery);

		if (appendSearchCriteria(kaleoTaskInstanceTokenQuery)) {
			appendAssetPrimaryKeyTerm(
				contextBooleanFilter, kaleoTaskInstanceTokenQuery);
			appendDueDateRangeTerm(
				contextBooleanFilter, kaleoTaskInstanceTokenQuery);
		}
	}

	@Override
	public void postProcessSearchQuery(
			BooleanQuery searchQuery, BooleanFilter fullQueryBooleanFilter,
			SearchContext searchContext)
		throws Exception {

		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery =
			(KaleoTaskInstanceTokenQuery)searchContext.getAttribute(
				"kaleoTaskInstanceTokenQuery");

		if (kaleoTaskInstanceTokenQuery == null) {
			return;
		}

		appendAssetTitleTerm(
			searchQuery, kaleoTaskInstanceTokenQuery.getAssetTitle(),
			searchContext);
		appendAssetTypeTerm(
			searchQuery, kaleoTaskInstanceTokenQuery.getAssetTypes(),
			searchContext);
		appendTaskNameTerm(
			searchQuery, kaleoTaskInstanceTokenQuery.getTaskName(),
			searchContext);
	}

	protected void appendAssetPrimaryKeyTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Long[] assetPrimaryKeys =
			kaleoTaskInstanceTokenQuery.getAssetPrimaryKeys();

		if (ArrayUtil.isEmpty(assetPrimaryKeys)) {
			return;
		}

		for (Long assetPrimaryKey : assetPrimaryKeys) {
			booleanFilter.addTerm(Field.CLASS_PK, assetPrimaryKey);
		}
	}

	protected void appendAssetTitleTerm(
			BooleanQuery booleanQuery, String assetTitle,
			SearchContext searchContext)
		throws Exception {

		if (Validator.isNull(assetTitle)) {
			return;
		}

		String assetTitleLocalizedName = LocalizationUtil.getLocalizedName(
			KaleoTaskInstanceTokenField.ASSET_TITLE,
			searchContext.getLanguageId());

		searchContext.setAttribute(assetTitleLocalizedName, assetTitle);

		addSearchLocalizedTerm(
			booleanQuery, searchContext,
			KaleoTaskInstanceTokenField.ASSET_TITLE, false);
	}

	protected void appendAssetTypeTerm(
			BooleanQuery booleanQuery, String[] assetTypes,
			SearchContext searchContext)
		throws Exception {

		if (ArrayUtil.isEmpty(assetTypes)) {
			return;
		}

		searchContext.setAttribute(
			KaleoTaskInstanceTokenField.CLASS_NAME, assetTypes);

		addSearchTerm(
			booleanQuery, searchContext, KaleoTaskInstanceTokenField.CLASS_NAME,
			false);
	}

	protected void appendAssigneeClassIdsNameTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		String assigneeClassName =
			kaleoTaskInstanceTokenQuery.getAssigneeClassName();

		if (Validator.isNull(assigneeClassName)) {
			return;
		}

		TermFilter assigneeClassNameIdsTermFilter = new TermFilter(
			KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_NAME_IDS,
			String.valueOf(portal.getClassNameId(assigneeClassName)));

		booleanFilter.add(
			assigneeClassNameIdsTermFilter, BooleanClauseOccur.MUST);
	}

	protected void appendAssigneeClassPKsTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Long assigneeClassPK = kaleoTaskInstanceTokenQuery.getAssigneeClassPK();

		if (assigneeClassPK == null) {
			return;
		}

		TermFilter assigneeClassNameIdsTermFilter = new TermFilter(
			KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
			String.valueOf(assigneeClassPK));

		booleanFilter.add(
			assigneeClassNameIdsTermFilter, BooleanClauseOccur.MUST);
	}

	protected void appendCompletedTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Boolean completed = kaleoTaskInstanceTokenQuery.isCompleted();

		if (completed == null) {
			return;
		}

		booleanFilter.addRequiredTerm(
			KaleoTaskInstanceTokenField.COMPLETED, completed);
	}

	protected void appendDueDateRangeTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Date dueDateGT = kaleoTaskInstanceTokenQuery.getDueDateGT();
		Date dueDateLT = kaleoTaskInstanceTokenQuery.getDueDateLT();

		if ((dueDateGT == null) && (dueDateLT == null)) {
			return;
		}

		DateRangeTermFilter dateRangeTermFilter = new DateRangeTermFilter(
			KaleoTaskInstanceTokenField.DUE_DATE, false, false,
			dueDateGT.toString(), dueDateLT.toString());

		booleanFilter.add(dateRangeTermFilter, BooleanClauseOccur.MUST);
	}

	protected void appendKaleoInstanceIdTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Long kaleoInstanceId = kaleoTaskInstanceTokenQuery.getKaleoInstanceId();

		if (kaleoInstanceId == null) {
			return;
		}

		booleanFilter.addRequiredTerm(
			KaleoTaskInstanceTokenField.KALEO_INSTANCE_ID, kaleoInstanceId);
	}

	protected void appendRoleIdsTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Boolean searchByUserRoles =
			kaleoTaskInstanceTokenQuery.isSearchByUserRoles();

		if (searchByUserRoles != null) {
			return;
		}

		List<Long> roleIds = kaleoTaskInstanceTokenQuery.getRoleIds();

		if (ListUtil.isEmpty(roleIds)) {
			return;
		}

		BooleanFilter roleIdsBooleanFilter = new BooleanFilter();

		for (Long roleId : roleIds) {
			roleIdsBooleanFilter.add(
				new TermFilter(
					KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
					roleId.toString()),
				BooleanClauseOccur.SHOULD);
		}

		booleanFilter.add(roleIdsBooleanFilter, BooleanClauseOccur.MUST);
	}

	protected void appendSearchByUserRolesTerm(
		BooleanFilter booleanFilter,
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Boolean searchByUserRoles =
			kaleoTaskInstanceTokenQuery.isSearchByUserRoles();

		if (searchByUserRoles == null) {
			return;
		}

		if (searchByUserRoles) {
			List<Long> roleIds = getSearchByUserRoleIds(
				kaleoTaskInstanceTokenQuery.getUserId());

			Map<Long, Set<Long>> roleIdGroupIdsMap = getRoleIdGroupIdsMap(
				kaleoTaskInstanceTokenQuery);

			if (roleIds.isEmpty() && roleIdGroupIdsMap.isEmpty()) {
				return;
			}

			BooleanFilter searchByRolesBooleanFilter = new BooleanFilter();

			TermFilter rolesClassNameIdTermFilter = new TermFilter(
				KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_NAME_IDS,
				String.valueOf(portal.getClassNameId(Role.class)));

			searchByRolesBooleanFilter.add(
				rolesClassNameIdTermFilter, BooleanClauseOccur.MUST);

			BooleanFilter innerSearchByRolesBooleanFilter = new BooleanFilter();

			searchByRolesBooleanFilter.add(
				innerSearchByRolesBooleanFilter, BooleanClauseOccur.MUST);

			innerSearchByRolesBooleanFilter.add(
				createRoleAssigneeClassPKBooleanFilter(roleIds));

			if (!roleIdGroupIdsMap.isEmpty()) {
				BooleanFilter roleIdGroupIdsMapBooleanFilter =
					createRoleIdGroupIdsMapBooleanFilter(roleIdGroupIdsMap);

				BooleanClauseOccur roleIdGroupIdsMapBooleanClauseOccur =
					BooleanClauseOccur.SHOULD;

				if (roleIds.isEmpty()) {
					roleIdGroupIdsMapBooleanClauseOccur =
						BooleanClauseOccur.MUST;
				}

				innerSearchByRolesBooleanFilter.add(
					roleIdGroupIdsMapBooleanFilter,
					roleIdGroupIdsMapBooleanClauseOccur);
			}

			booleanFilter.add(
				searchByRolesBooleanFilter, BooleanClauseOccur.MUST);
		}
		else {
			TermFilter assigneeClassNameIdsTermFilter = new TermFilter(
				KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_NAME_IDS,
				String.valueOf(portal.getClassNameId(User.class)));

			booleanFilter.add(
				assigneeClassNameIdsTermFilter, BooleanClauseOccur.MUST);

			TermFilter assigneeClassPKsTermFilter = new TermFilter(
				KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
				String.valueOf(kaleoTaskInstanceTokenQuery.getUserId()));

			booleanFilter.add(
				assigneeClassPKsTermFilter, BooleanClauseOccur.MUST);
		}
	}

	protected boolean appendSearchCriteria(
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		if (ArrayUtil.isNotEmpty(
				kaleoTaskInstanceTokenQuery.getAssetPrimaryKeys())) {

			return true;
		}

		if (ArrayUtil.isNotEmpty(kaleoTaskInstanceTokenQuery.getAssetTypes())) {
			return true;
		}

		if (kaleoTaskInstanceTokenQuery.getDueDateGT() != null) {
			return true;
		}

		if (kaleoTaskInstanceTokenQuery.getDueDateLT() != null) {
			return true;
		}

		return false;
	}

	protected void appendTaskNameTerm(
			BooleanQuery booleanQuery, String taskName,
			SearchContext searchContext)
		throws Exception {

		if (Validator.isNull(taskName)) {
			return;
		}

		searchContext.setAttribute(
			KaleoTaskInstanceTokenField.TASK_NAME, taskName);

		addSearchTerm(
			booleanQuery, searchContext, KaleoTaskInstanceTokenField.TASK_NAME,
			false);
	}

	protected BooleanFilter createRoleAssigneeClassPKBooleanFilter(
		List<Long> roleIds) {

		BooleanFilter roleClassPKBooleanFilter = new BooleanFilter();

		for (Long roleId : roleIds) {
			roleClassPKBooleanFilter.add(
				new TermFilter(
					KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
					String.valueOf(roleId)));
		}

		return roleClassPKBooleanFilter;
	}

	protected BooleanFilter createRoleIdGroupIdsMapBooleanFilter(
		Map<Long, Set<Long>> roleIdGroupIdsMap) {

		BooleanFilter roleIdGroupIdsMapBooleanFilter = new BooleanFilter();

		for (Map.Entry<Long, Set<Long>> entry : roleIdGroupIdsMap.entrySet()) {
			BooleanFilter roleIdGroupIdsBooleanFilter = new BooleanFilter();

			roleIdGroupIdsBooleanFilter.add(
				new TermFilter(
					KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
					String.valueOf(entry.getKey())),
				BooleanClauseOccur.MUST);

			BooleanFilter assigneeGroupIdsBooleanFilter = new BooleanFilter();

			for (Long assigneeGroupId : entry.getValue()) {
				assigneeGroupIdsBooleanFilter.add(
					new TermFilter(
						KaleoTaskInstanceTokenField.ASSIGNEE_GROUP_IDS,
						String.valueOf(assigneeGroupId)));
			}

			roleIdGroupIdsBooleanFilter.add(
				assigneeGroupIdsBooleanFilter, BooleanClauseOccur.MUST);

			roleIdGroupIdsMapBooleanFilter.add(
				roleIdGroupIdsBooleanFilter, BooleanClauseOccur.SHOULD);
		}

		return roleIdGroupIdsMapBooleanFilter;
	}

	@Override
	protected void doDelete(KaleoTaskInstanceToken kaleoTaskInstanceToken)
		throws Exception {

		deleteDocument(
			kaleoTaskInstanceToken.getCompanyId(),
			kaleoTaskInstanceToken.getKaleoTaskInstanceTokenId());
	}

	@Override
	protected Document doGetDocument(
			KaleoTaskInstanceToken kaleoTaskInstanceToken)
		throws Exception {

		Document document = getBaseModelDocument(
			CLASS_NAME, kaleoTaskInstanceToken);

		List<KaleoTaskAssignmentInstance> kaleoTaskAssignmentInstances =
			kaleoTaskInstanceToken.getKaleoTaskAssignmentInstances();

		Set<Long> assigneeClassNameIds = new HashSet<>();
		Set<Long> assigneeClassPKs = new HashSet<>();
		Set<Long> assigneeGroupIds = new HashSet<>();

		for (KaleoTaskAssignmentInstance kaleoTaskAssignmentInstance :
				kaleoTaskAssignmentInstances) {

			assigneeClassNameIds.add(
				portal.getClassNameId(
					kaleoTaskAssignmentInstance.getAssigneeClassName()));
			assigneeClassPKs.add(
				kaleoTaskAssignmentInstance.getAssigneeClassPK());
			assigneeGroupIds.add(kaleoTaskAssignmentInstance.getGroupId());
		}

		document.addKeyword(
			KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_NAME_IDS,
			assigneeClassNameIds.toArray(new Long[0]));
		document.addKeyword(
			KaleoTaskInstanceTokenField.ASSIGNEE_CLASS_PKS,
			assigneeClassPKs.toArray(new Long[0]));
		document.addKeyword(
			KaleoTaskInstanceTokenField.ASSIGNEE_GROUP_IDS,
			assigneeGroupIds.toArray(new Long[0]));
		document.addKeyword(
			KaleoTaskInstanceTokenField.COMPLETED,
			kaleoTaskInstanceToken.isCompleted());
		document.addKeyword(
			KaleoTaskInstanceTokenField.CLASS_NAME,
			kaleoTaskInstanceToken.getClassName());
		document.addKeyword(
			Field.CLASS_PK, kaleoTaskInstanceToken.getClassPK());
		document.addDate(
			KaleoTaskInstanceTokenField.DUE_DATE,
			kaleoTaskInstanceToken.getDueDate());
		document.addKeyword(
			KaleoTaskInstanceTokenField.KALEO_INSTANCE_ID,
			kaleoTaskInstanceToken.getKaleoInstanceId());
		document.addKeyword(
			KaleoTaskInstanceTokenField.TASK_NAME,
			kaleoTaskInstanceToken.getKaleoTaskName());

		AssetEntry assetEntry = getAssetEntry(kaleoTaskInstanceToken);

		if (assetEntry == null) {
			return document;
		}

		document.addKeyword(
			KaleoTaskInstanceTokenField.ASSET_CLASS_NAME_ID,
			assetEntry.getClassNameId());
		document.addKeyword(
			KaleoTaskInstanceTokenField.ASSET_CLASS_PK,
			assetEntry.getClassPK());

		Locale defaultLocale = LocaleUtil.getSiteDefault();

		String siteDefaultLanguageId = LocaleUtil.toLanguageId(defaultLocale);

		String[] titleLanguageIds = getLanguageIds(
			siteDefaultLanguageId, assetEntry.getTitle());

		for (String titleLanguageId : titleLanguageIds) {
			document.addText(
				LocalizationUtil.getLocalizedName(
					KaleoTaskInstanceTokenField.ASSET_TITLE, titleLanguageId),
				assetEntry.getTitle(titleLanguageId));
		}

		String[] descriptionLanguageIds = getLanguageIds(
			siteDefaultLanguageId, assetEntry.getDescription());

		for (String descriptionLanguageId : descriptionLanguageIds) {
			document.addText(
				LocalizationUtil.getLocalizedName(
					KaleoTaskInstanceTokenField.ASSET_DESCRIPTION,
					descriptionLanguageId),
				assetEntry.getDescription(descriptionLanguageId));
		}

		return document;
	}

	@Override
	protected Summary doGetSummary(
			Document document, Locale locale, String snippet,
			PortletRequest portletRequest, PortletResponse portletResponse)
		throws Exception {

		Summary summary = createSummary(document);

		summary.setMaxContentLength(200);

		return summary;
	}

	@Override
	protected void doReindex(KaleoTaskInstanceToken kaleoTaskInstanceToken)
		throws Exception {

		Document document = getDocument(kaleoTaskInstanceToken);

		indexWriterHelper.updateDocument(
			getSearchEngineId(), kaleoTaskInstanceToken.getCompanyId(),
			document, isCommitImmediately());
	}

	@Override
	protected void doReindex(String className, long classPK) throws Exception {
		KaleoTaskInstanceToken entry =
			kaleoTaskInstanceTokenLocalService.getKaleoTaskInstanceToken(
				classPK);

		doReindex(entry);
	}

	@Override
	protected void doReindex(String[] ids) throws Exception {
		long companyId = GetterUtil.getLong(ids[0]);

		reindexKaleoTaskInstanceTokens(companyId);
	}

	protected AssetEntry getAssetEntry(
		KaleoTaskInstanceToken kaleoTaskInstanceToken) {

		try {
			AssetRendererFactory<?> assetRendererFactory =
				getAssetRendererFactory(kaleoTaskInstanceToken.getClassName());

			AssetRenderer<?> assetRenderer =
				assetRendererFactory.getAssetRenderer(
					kaleoTaskInstanceToken.getClassPK());

			return assetEntryLocalService.getEntry(
				assetRenderer.getClassName(), assetRenderer.getClassPK());
		}
		catch (PortalException pe) {
			if (_log.isDebugEnabled()) {
				_log.debug(pe, pe);
			}
		}

		return null;
	}

	protected AssetRendererFactory<?> getAssetRendererFactory(
		String className) {

		return AssetRendererFactoryRegistryUtil.
			getAssetRendererFactoryByClassName(className);
	}

	protected String[] getLanguageIds(
		String defaultLanguageId, String content) {

		String[] languageIds = LocalizationUtil.getAvailableLanguageIds(
			content);

		if (languageIds.length == 0) {
			languageIds = new String[] {defaultLanguageId};
		}

		return languageIds;
	}

	protected Map<Long, Set<Long>> getRoleIdGroupIdsMap(
		KaleoTaskInstanceTokenQuery kaleoTaskInstanceTokenQuery) {

		Map<Long, Set<Long>> roleIdGroupIdsMap = new HashMap<>();

		List<UserGroupRole> userGroupRoles =
			userGroupRoleLocalService.getUserGroupRoles(
				kaleoTaskInstanceTokenQuery.getUserId());

		for (UserGroupRole userGroupRole : userGroupRoles) {
			mapRoleIdGroupId(
				userGroupRole.getRoleId(), userGroupRole.getGroupId(),
				roleIdGroupIdsMap);
		}

		List<UserGroupGroupRole> userGroupGroupRoles = getUserGroupGroupRoles(
			kaleoTaskInstanceTokenQuery.getUserId());

		for (UserGroupGroupRole userGroupGroupRole : userGroupGroupRoles) {
			mapRoleIdGroupId(
				userGroupGroupRole.getRoleId(), userGroupGroupRole.getGroupId(),
				roleIdGroupIdsMap);
		}

		return roleIdGroupIdsMap;
	}

	protected List<Long> getSearchByUserRoleIds(long userId) {
		try {
			List<Role> roles = roleLocalService.getUserRoles(userId);

			List<Group> groups = new ArrayList<>();

			User user = userLocalService.getUserById(userId);

			groups.addAll(user.getGroups());
			groups.addAll(
				groupLocalService.getOrganizationsGroups(
					user.getOrganizations()));
			groups.addAll(
				groupLocalService.getOrganizationsRelatedGroups(
					user.getOrganizations()));
			groups.addAll(
				groupLocalService.getUserGroupsGroups(user.getUserGroups()));
			groups.addAll(
				groupLocalService.getUserGroupsRelatedGroups(
					user.getUserGroups()));

			for (Group group : groups) {
				roles.addAll(
					roleLocalService.getGroupRoles(group.getGroupId()));
			}

			Stream<Role> stream = roles.parallelStream();

			return stream.map(
				Role::getRoleId
			).collect(
				Collectors.toList()
			);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return Collections.emptyList();
	}

	protected List<UserGroupGroupRole> getUserGroupGroupRoles(long userId) {
		List<UserGroupGroupRole> userGroupGroupRoles = new ArrayList<>();

		List<UserGroup> userGroups = userGroupLocalService.getUserUserGroups(
			userId);

		for (UserGroup userGroup : userGroups) {
			userGroupGroupRoles.addAll(
				userGroupGroupRoleLocalService.getUserGroupGroupRoles(
					userGroup.getUserGroupId()));
		}

		return userGroupGroupRoles;
	}

	protected void mapRoleIdGroupId(
		long roleId, long groupId, Map<Long, Set<Long>> roleIdGroupIdsMap) {

		Set<Long> groupIds = roleIdGroupIdsMap.get(roleId);

		if (groupIds == null) {
			groupIds = new TreeSet<>();

			roleIdGroupIdsMap.put(roleId, groupIds);
		}

		groupIds.add(groupId);
	}

	protected void reindexKaleoTaskInstanceTokens(long companyId)
		throws PortalException {

		final IndexableActionableDynamicQuery indexableActionableDynamicQuery =
			kaleoTaskInstanceTokenLocalService.
				getIndexableActionableDynamicQuery();

		indexableActionableDynamicQuery.setCompanyId(companyId);

		indexableActionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod
				<KaleoTaskInstanceToken>() {

				@Override
				public void performAction(
					KaleoTaskInstanceToken kaleoTaskInstanceToken) {

					try {
						Document document = getDocument(kaleoTaskInstanceToken);

						indexableActionableDynamicQuery.addDocuments(document);
					}
					catch (PortalException pe) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Unable to index kaleoTaskInstanceToken " +
									kaleoTaskInstanceToken.
										getKaleoTaskInstanceTokenId(),
								pe);
						}
					}
				}

			});
		indexableActionableDynamicQuery.setSearchEngineId(getSearchEngineId());

		indexableActionableDynamicQuery.performActions();
	}

	@Reference
	protected AssetEntryLocalService assetEntryLocalService;

	@Reference
	protected ClassNameLocalService classNameLocalService;

	@Reference
	protected GroupLocalService groupLocalService;

	@Reference
	protected IndexWriterHelper indexWriterHelper;

	@Reference
	protected KaleoTaskInstanceTokenLocalService
		kaleoTaskInstanceTokenLocalService;

	@Reference
	protected Portal portal;

	@Reference
	protected RoleLocalService roleLocalService;

	@Reference
	protected UserGroupGroupRoleLocalService userGroupGroupRoleLocalService;

	@Reference
	protected UserGroupLocalService userGroupLocalService;

	@Reference
	protected UserGroupRoleLocalService userGroupRoleLocalService;

	@Reference
	protected UserLocalService userLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
		KaleoTaskInstanceTokenIndexer.class);

}