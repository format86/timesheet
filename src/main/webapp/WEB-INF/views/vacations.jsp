<%@page contentType="text/html" pageEncoding="UTF-8" %>

<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<html>
<head>
    <title><fmt:message key="title.vacations"/></title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/resources/css/vacations.css" />
    <script type="text/javascript" src="<%= request.getContextPath()%>/resources/js/vacations.js"></script>
    <script type="text/javascript">
        dojo.require("dojo.NodeList-traverse");

        dojo.ready(function() {
            window.focus();

            setCurrentYear(${year});
            divisionChange(dojo.byId("divisionId"));

            dojo.byId("employeeId").value = ${employeeId};
        });

        var employeeList = ${employeeListJson};

        function showVacations() {
            var empId = dojo.byId("employeeId").value;
            var divisionId = dojo.byId("divisionId").value;
            var year = dojo.byId("year").value;

            if (isNotNilOrNull(year)) {
                if (checkEmployeeData(divisionId, empId)) {
                    vacationsForm.action =
                            "<%=request.getContextPath()%>/vacations/" + divisionId + "/" + empId + "/" + year;
                    vacationsForm.submit();
                }
            } else {
                var error = "";

                if (isNilOrNull(year)) {
                    error += ("Необходимо выбрать год\n");
                }

                alert(error);
            }
        }

        function createVacation() {
            var divisionId = dojo.byId("divisionId").value;
            var empId = dojo.byId("employeeId").value;

            if (checkEmployeeData(divisionId, empId)) {
                vacationsForm.action =
                        "<%=request.getContextPath()%>/createVacation/" + empId;
                vacationsForm.submit();
            }
        }

        function deleteVacation(parentElement, vac_id) {
            if (!confirm("Удалить заявку?")) {
                return;
            }

            var prevHtml = parentElement.innerHTML;

            dojo.addClass(parentElement, "activity-indicator");
            parentElement.innerHTML =
                    "<img src=\"<c:url value="/resources/img/loading_small.gif"/>\"/>";

            function handleError(error) {
                resetParent();

                alert("Не удалось удалить заявку:\n\n" + error);
            }

            function resetParent() {
                dojo.removeClass(parentElement, "activity-indicator");
                parentElement.innerHTML = prevHtml;
            }

            dojo.xhrGet({
                url: "<%= request.getContextPath()%>/deleteVacation/" + vac_id,
                handleAs: "text",

                load: function(data) {
                    if (data.length == 0) {
                        dojo.destroy(dojo.NodeList(parentElement).parents("tr")[0]);
                    } else {
                        handleError(data);
                    }
                },

                error: function(error) {
                    handleError(error.message);
                }
            });
        }
    </script>
</head>
<body>

<h1><fmt:message key="title.vacations"/></h1>
<br/>

<form:form method="post" commandName="vacationsForm" name="mainForm">
    <c:if test="${fn:length(errors) > 0}">
        <div class="errors_box">
            <c:forEach items="${errors}" var="error">
                <fmt:message key="${error.code}">
                    <fmt:param value="${error.arguments[0]}"/>
                </fmt:message><br/>
            </c:forEach>
        </div>
    </c:if>

    <span class="label">Подразделение</span>
    <form:select path="divisionId" id="divisionId" onchange="divisionChange(this)" class="without_dojo"
                 onmouseover="tooltip.show(getTitle(this));" onmouseout="tooltip.hide();">
        <form:option label="" value="0"/>
        <form:options items="${divisionList}" itemLabel="name" itemValue="id"/>
    </form:select>

    <span class="label">Отчет сотрудника</span>
    <form:select path="employeeId" id="employeeId" class="without_dojo" onmouseover="tooltip.show(getTitle(this));"
                 onmouseout="tooltip.hide();" onchange="setDefaultEmployeeJob(-1);">
        <form:option items="${employeeList}" label="" value="0"/>
    </form:select>
    <br><br>

    <span class="label">Год:</span>
    <form:select path="year" id="year" class="without_dojo" onmouseover="tooltip.show(getTitle(this));" onmouseout="tooltip.hide();">
        <form:option label="" value="0"/>
        <form:options items="${yearsList}" itemLabel="year" itemValue="year"/>
    </form:select>

    <button id="show" style="width:150px" style="vertical-align: middle" type="button"
            onclick="showVacations()">Показать</button>
</form:form>
<br/>
<table id="vacations">
    <thead>
    <tr>
        <th width="15" class="create-button">
            <img src="<c:url value="/resources/img/add.gif"/>" title="Создать" onclick="createVacation();"/>
        </th>
        <th width="160">Статус</th>
        <th width="220">Тип отпуска</th>
        <th width="120">Дата с</th>
        <th width="120">Дата по</th>
        <th width="130">Кол-во календарных дней</th>
        <th width="130">Кол-во рабочих дней</th>
        <th width="270">Комментарий</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="vacation" items="${vacationsList}" varStatus="lp">
        <tr>
            <td>
                <sec:authorize access="
                    hasRole('ROLE_ADMIN') or
                    ${
                        (vacation.employee.id eq curEmployee.id) or
                        (vacation.author.id eq curEmployee.id)
                    }
                ">
                    <div class="delete-button">
                        <img src="<c:url value="/resources/img/delete.png"/>" title="Удалить"
                             onclick="deleteVacation(this.parentElement, ${vacation.id});" />
                    </div>
                </sec:authorize>
            </td>
            <td class="centered">${vacation.status.value}</td>
            <td class="centered">${vacation.type.value}</td>
            <td class="date"><fmt:formatDate value="${vacation.beginDate}" pattern="dd.MM.yyyy"/></td>
            <td class="date"><fmt:formatDate value="${vacation.endDate}" pattern="dd.MM.yyyy"/></td>
            <td class="centered">${calDays[lp.index]}</td>
            <td class="centered">${workDays[lp.index]}</td>
            <td>
                ${vacation.comment}
                <c:if test="${vacation.author.id ne vacation.employee.id}">
                    <c:if test="${fn:length(vacation.comment) != 0}"><br/></br></c:if>
                    Заявка создана сотрудником ${vacation.author.name}
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
    <tfoot>
        <tr class="summary">
            <td colspan="3">Кол-во утвержденных заявлений на отпуск</td>
            <td colspan="5">${summaryApproved}</td>
        </tr>
        <tr class="summary">
            <td colspan="3">Кол-во отклоненных заявлений на отпуск</td>
            <td colspan="5">${summaryRejected}</td>
        </tr>
        <tr class="summary">
            <td colspan="3">Кол-во календарных дней отпуска за год</td>
            <td colspan="5">${summaryCalDays}</td>
        </tr>
        <tr class="summary">
            <td colspan="3">Кол-во рабочих дней отпуска за год</td>
            <td colspan="5">${summaryWorkDays}</td>
        </tr>
    </tfoot>
</table>
</body>
</html>