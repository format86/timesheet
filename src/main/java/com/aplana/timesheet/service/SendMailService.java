package com.aplana.timesheet.service;

import com.aplana.timesheet.dao.VacationDAO;
import com.aplana.timesheet.dao.entity.*;
import com.aplana.timesheet.enums.ProjectRolesEnum;
import com.aplana.timesheet.enums.TypesOfActivityEnum;
import com.aplana.timesheet.form.AdminMessageForm;
import com.aplana.timesheet.form.FeedbackForm;
import com.aplana.timesheet.form.TimeSheetForm;
import com.aplana.timesheet.form.TimeSheetTableRowForm;
import com.aplana.timesheet.properties.TSPropertyProvider;
import com.aplana.timesheet.service.MailSenders.*;
import com.aplana.timesheet.util.DateTimeUtil;
import com.aplana.timesheet.util.TimeSheetUser;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.velocity.VelocityEngineUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aplana.timesheet.enums.ProjectRolesEnum.*;

@Service
public class SendMailService{
    private static final Logger logger = LoggerFactory.getLogger(SendMailService.class);

    private final Predicate<RenameMe> LEAVE_PRESALE_AND_PROJECTS_ONLY =
            Predicates.and(Predicates.notNull(), new Predicate<RenameMe>() {
                @Override
                public boolean apply(@Nullable RenameMe renameMe) {
                    TypesOfActivityEnum actType = renameMe.getTypeOfActivity();
                    return actType == TypesOfActivityEnum.PROJECT || actType == TypesOfActivityEnum.PRESALE;
                }
            });
    private final Function<ProjectParticipant, String> GET_EMAIL_FROM_PARTICIPANT
            = new Function<ProjectParticipant, String>() {
        @Nullable @Override
        public String apply(@Nullable ProjectParticipant projectParticipant) {
            return projectParticipant.getEmployee().getEmail();
        }
    };

    private Function<RenameMe,String> GET_EMAILS_OF_INTERESTED_PARTICIPANTS_FROM_PROJECT_FOR_CURRENT_ROLE
            = new Function<RenameMe, String>() {
        @Nullable @Override
        public String apply(@Nullable RenameMe input) {
            final ProjectRolesEnum roleInCurrentProject = input.getProjectRole();

            return Joiner.on(",").join(
                Iterables.transform(
                    Iterables.filter(
                        projectService.getParticipants(projectService.find(input.getProjectId())),
                            new Predicate<ProjectParticipant>() {
                                @Override
                                public boolean apply(@Nullable ProjectParticipant participant) {
                                    if(participant == null || participant.getProjectRole()==null) return false;
                                    ProjectRolesEnum projectPaticipantRole = getById(participant.getProjectRole().getId());
                                    return rolesOfEmploeeysForRole.get(projectPaticipantRole).contains(roleInCurrentProject);
                                } }),
                    GET_EMAIL_FROM_PARTICIPANT
                )
        ); } };

    public static Multimap<ProjectRolesEnum, ProjectRolesEnum> rolesOfEmploeeysForRole = HashMultimap.create();

    static {
        rolesOfEmploeeysForRole.putAll(PROJECT_MANAGER, Arrays.asList(values()));
        rolesOfEmploeeysForRole.putAll(HEAD_OF_DEVELOPMENT, Arrays.asList(DESIGN_ENGINEER, DEVELOPER_OLD, SYSTEM_ENGINEER_OLD, TESTER_OLD));
        rolesOfEmploeeysForRole.putAll(ANALYST_OLD, Arrays.asList(ANALYST_OLD, TECHNICAL_WRITER));
    }

    @Autowired
    public VelocityEngine velocityEngine;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private DictionaryItemService dictionaryItemService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private TSPropertyProvider propertyProvider;
    @Autowired
    private OvertimeCauseService overtimeCauseService;
    @Autowired
    private VacationDAO vacationDAO;


    /**
     * Возвращает строку с адресами линейных руководителей сотрудника
     * (непосредственного и всех вышестоящих) разделёнными запятой.
     * @param empId
     */
    public String getEmployeesManagersEmails(Integer empId) {
        StringBuilder chiefEmails = new StringBuilder();
        chiefEmails.append(employeeService.find(empId).getEmail()).append(",");
        Employee manager = employeeService.find(empId).getManager();
        if (manager != null) {
            chiefEmails.append(getEmployeesManagersEmails(manager.getId()));
        }
        return chiefEmails.toString();
    }

    /**
     * Возвращает email сотрудника
     *
     * @param Id сотрудника
     * @return email сотрудника
     */
    public String getEmployeeEmail(Integer empId) {
        return empId == null ? null : employeeService.find(empId).getEmail();
    }

    /**
     * Возвращает ФИО сотрудника
     * @param Id сотрудника
     * @return ФИО сотрудника
     */
    public String getEmployeeFIO(Integer empId){
        return empId == null ? null : employeeService.find(empId).getName();
    }

    /**
     * Возвращает строку с адресами менеджеров проектов/пресейлов
     * @param tsForm
     */
    public String getProjectsManagersEmails(TimeSheetForm tsForm) {
        List<TimeSheetTableRowForm> tsRows = tsForm.getTimeSheetTablePart();

        if (tsRows == null) {
            return "";
        } //Нет проектов\пресейлов, нет и менеджеров.

        return Joiner.on(",").join(Iterables.transform(
                Iterables.filter(transformTimeSheetTableRowForm(tsRows), LEAVE_PRESALE_AND_PROJECTS_ONLY),
                new Function<RenameMe, String>() {
                    @Nullable @Override
                    public String apply(@Nullable RenameMe input) {
                        return getEmployeeEmail(projectService.find(input.getProjectId()).getManager().getId());
                    }
                }));
    }

    /**
     * Возвращает строку с email адресами в соответствии с логикой
     * РП - все роли
     * Руководителю группы разработки - конструктор, разработчик, системный инженер, тестировщик.
     * Ведущему аналитику - аналитик и технический писатель.
     *
     *
     * @param empId - идентификатор сотрудника, пославшего отчет
     * @param tsForm
     * @return emails - строка с emailАМИ
     */
    public String getProjectParticipantsEmails(TimeSheetForm tsForm) {
        return getProjectParticipantsEmails(transformTimeSheetTableRowForm(tsForm.getTimeSheetTablePart()));
    }


    /**
     * Получает email адреса из всех проектов
     * @param ts
     * @return string строка содержащая email's которым относится данный timesheet
     */
    public String getProjectParticipantsEmails(TimeSheet ts) {
        return getProjectParticipantsEmails(transformTimeSheetDetail(ts.getTimeSheetDetails()));
    }

    private String getProjectParticipantsEmails(Iterable<RenameMe> details) {
        return Joiner.on(",").join(
                Iterables.transform(
                        Iterables.filter(details, LEAVE_PRESALE_AND_PROJECTS_ONLY),
                        GET_EMAILS_OF_INTERESTED_PARTICIPANTS_FROM_PROJECT_FOR_CURRENT_ROLE)
        );
    }

    public List<Employee> getEmployeesList(Division division){
        return employeeService.getEmployees(division, false);
    }

    public void performMailing(TimeSheetForm form) {
        new TimeSheetSender(this, propertyProvider).sendMessage(form);
    }

    public void performFeedbackMailing(FeedbackForm form) {
        new FeedbackSender(this, propertyProvider).sendMessage(form);
    }

    public void performLoginProblemMailing(AdminMessageForm form) {
        new LoginProblemSender(this, propertyProvider).sendMessage(form);
    }

    public void performPersonalAlertMailing(List<ReportCheck> rCheckList) {
        new PersonalAlertSender(this, propertyProvider).sendMessage(rCheckList);
    }

    public void performManagerMailing(List<ReportCheck> rCheckList) {
        new ManagerAlertSender(this, propertyProvider).sendMessage(rCheckList);
    }

    public void performEndMonthMailing(List<ReportCheck> rCheckList) {
        new EndMonthAlertSender(this, propertyProvider).sendMessage(rCheckList);
    }

    public void performTimeSheetDeletedMailing(TimeSheet timeSheet) {
        new TimeSheetDeletedSender(this, propertyProvider).sendMessage(timeSheet);
    }

    public void performVacationMailing(Vacation vacation) {
        new VacationSender(this, propertyProvider).sendMessage(vacation);
    }

    public void performExceptionSender(String problem){
        new ExceptionSender(this, propertyProvider).sendMessage(problem);
    }

    public String initMessageBodyForReport(TimeSheet timeSheet) {
        Map<String, Object> model1 = new HashMap<String, Object>();

        model1.put("dictionaryItemService", dictionaryItemService);
        model1.put("projectService", projectService);
        model1.put("DateTimeUtil", DateTimeUtil.class);
        model1.put("senderName",
                timeSheet == null
                        ? securityService.getSecurityPrincipal().getEmployee().getName()
                        : timeSheet.getEmployee().getName());
        Map<String, Object> model = model1;

        model.put("timeSheet", timeSheet);
        logger.info("follows initialization output from velocity");
        return VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "report.vm", model);
    }

    public List<Employee> getRegionManagerList(Integer id) {
        return employeeService.getRegionManager(id);
    }

    public TimeSheetUser getSecurityPrincipal() {
        return securityService.getSecurityPrincipal();
    }

    public String getProjectName(int projectId) {
        return projectService.find(projectId).getName();
    }

    public String getOvertimeCause(TimeSheetForm tsForm) {
        return overtimeCauseService.getCauseName(tsForm);
    }

    public List<String> getVacationApprovalEmailList(Integer vacationId) {
        return vacationDAO.getVacationApprovalEmailList(vacationId);
    }

    interface RenameMe {
        TypesOfActivityEnum getTypeOfActivity();
        ProjectRolesEnum getProjectRole();
        Integer getProjectId();
    }

    public Iterable<RenameMe> transformTimeSheetTableRowForm(Iterable<TimeSheetTableRowForm> tsRows) {
        return Iterables.transform(tsRows, new Function<TimeSheetTableRowForm, RenameMe>() {
            @Nullable
            @Override
            public RenameMe apply(@Nullable final TimeSheetTableRowForm input) {
                return new RenameMe() {
                    @Override
                    public TypesOfActivityEnum getTypeOfActivity() {
                        return TypesOfActivityEnum.getById(input.getActivityTypeId());
                    }

                    @Override
                    public ProjectRolesEnum getProjectRole() {
                        return ProjectRolesEnum.getById(input.getProjectRoleId());
                    }

                    @Override
                    public Integer getProjectId() {
                        return input.getProjectId();
                    }
                };
            }
        });
    }

    public Iterable<RenameMe> transformTimeSheetDetail(Iterable<TimeSheetDetail> details) {
        return Iterables.transform(details, new Function<TimeSheetDetail, RenameMe>() {
            @Nullable
            @Override
            public RenameMe apply(@Nullable final TimeSheetDetail input) {
                return new RenameMe() {
                    @Override
                    public TypesOfActivityEnum getTypeOfActivity() {
                        return TypesOfActivityEnum.getById(input.getActType().getId());
                    }

                    @Override
                    public ProjectRolesEnum getProjectRole() {
                        return ProjectRolesEnum.getById(input.getProjectRole().getId());
                    }

                    @Override
                    public Integer getProjectId() {
                        return input.getProject().getId();
                    }
                };
            }
        });
    }
}