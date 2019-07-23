<%--
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
--%>

<%@ include file="/init.jsp" %>

<portlet:actionURL name="copyApplications" var="copyApplicationsURL">
	<portlet:param name="mvcPath" value="/copy_layout_redirect.jsp" />
	<portlet:param name="referringPortletResource" value="<%= portletDisplay.getId() %>" />
</portlet:actionURL>

<aui:form action="<%= copyApplicationsURL %>" name="copyApplicationsFm">
	<aui:input name="selPlid" type="hidden" value="<%= layoutsAdminDisplayContext.getSelPlid() %>" />
</aui:form>

<%
Layout selLayout = layoutsAdminDisplayContext.getSelLayout();
%>

<div class="container-fluid-1280" id="<portlet:namespace />copyPortletsFromPage">
	<p>
		<c:if test="<%= selLayout != null %>">
			<liferay-ui:message arguments="<%= HtmlUtil.escape(selLayout.getName(locale)) %>" key="the-applications-in-page-x-will-be-replaced-with-the-ones-in-the-page-you-select-below" translateArguments="<%= false %>" />
		</c:if>
	</p>

	<aui:select label="copy-from-page" name="copyPlid">

		<%
		for (LayoutDescription layoutDescription : layoutsAdminDisplayContext.getLayoutDescriptions()) {
			if (layoutDescription.getPlid() > 0) {
		%>

				<aui:option disabled="<%= (selLayout != null) && (selLayout.getPlid() == layoutDescription.getPlid()) %>" label="<%= layoutDescription.getDisplayName() %>" value="<%= layoutDescription.getPlid() %>" />

		<%
			}
		}
		%>

	</aui:select>

	<aui:button-row>
		<aui:button name="copySubmitButton" value="copy" />
	</aui:button-row>
</div>

<aui:script use="liferay-util-window">
	var content = A.one('#<portlet:namespace />copyPortletsFromPage');

	var submitButton = A.one('#<portlet:namespace />copySubmitButton');

	if (submitButton) {
		submitButton.on(
			'click',
			function(event) {
				var form = A.one('#<portlet:namespace />copyApplicationsFm');

				if (form) {
					form.append(content);

					submitForm(form);
				}
			}
		);
	}
</aui:script>