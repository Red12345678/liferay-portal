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

<%@ include file="/html/portal/layout/edit/init.jsp" %>

<%
String copyLayoutIdPrefix = ParamUtil.getString(request, "copyLayoutIdPrefix");
%>

<aui:select id='<%= HtmlUtil.escapeAttribute(copyLayoutIdPrefix) + "copyPlid" %>' label="copy-from-page" name="copyPlid">

	<%
	List<LayoutDescription> layoutDescriptions = (List<LayoutDescription>)request.getAttribute(WebKeys.LAYOUT_DESCRIPTIONS);

	for (LayoutDescription layoutDescription : layoutDescriptions) {
		if (layoutDescription.getPlid() > 0) {
	%>

			<aui:option disabled="<%= (selLayout != null) && (selLayout.getPlid() == layoutDescription.getPlid()) %>" label="<%= layoutDescription.getDisplayName() %>" value="<%= layoutDescription.getPlid() %>" />

	<%
		}
	}
	%>

</aui:select>