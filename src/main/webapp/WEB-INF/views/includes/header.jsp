<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<%@page contentType="text/html" pageEncoding="UTF-8" %>
<a href="<c:url value='/'/>"><img id="logo" src="<%=request.getContextPath()%>/resources/img/logo.png"
                                  alt="Aplana Software"/></a>

<div id="header_text">
    Корпоративная система списания занятости
</div>
<sec:authorize access="isAuthenticated()">
    <div class="employee_name">Сотрудник: <sec:authentication property="principal.employee.name"/></div>

    <div class="menu_holder">

        <ul style="list-style-type: none;">
            <li><a href="<c:url value='/'/>"><fmt:message key="menu.main"/></a></li>
            <li><a href="<c:url value='/viewreports'/>"><fmt:message key="title.viewreports"/></a></li>
            <li><a href="<c:url value='/businesstripsandillness'/>"><fmt:message key="title.businesstripsandillness"/></a></li>
            <li><a href="<c:url value='/vacations'/>"><fmt:message key="title.vacations"/></a></li>
            <sec:authorize access="hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')">
                <li><a href="<c:url value='/managertools'/>"><fmt:message key="title.manager"/></a></li>
            </sec:authorize>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
                <li><a href="<c:url value='/admin'/>"><fmt:message key="title.admin"/></a></li>
            </sec:authorize>
            <li>
                <a href="<c:url value='/feedback'/>"><fmt:message key="menu.feedback"/></a></li>
            <li style="float: right"><a href="<c:url value='/logout'/>">Выйти из системы</a></li>
        </ul>
    </div>
</sec:authorize>

<div class="clear"></div>