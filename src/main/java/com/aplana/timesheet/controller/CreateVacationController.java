package com.aplana.timesheet.controller;

import com.aplana.timesheet.dao.DictionaryItemDAO;
import com.aplana.timesheet.dao.VacationDAO;
import com.aplana.timesheet.dao.entity.Calendar;
import com.aplana.timesheet.dao.entity.Employee;
import com.aplana.timesheet.dao.entity.Vacation;
import com.aplana.timesheet.enums.VacationStatusEnum;
import com.aplana.timesheet.enums.VacationTypesEnum;
import com.aplana.timesheet.form.CreateVacationForm;
import com.aplana.timesheet.form.validator.CreateVacationFormValidator;
import com.aplana.timesheet.service.CalendarService;
import com.aplana.timesheet.service.EmployeeService;
import com.aplana.timesheet.service.SecurityService;
import com.aplana.timesheet.util.DateTimeUtil;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
@Controller
public class CreateVacationController {

    private static final String CREATE_VACATION_FORM = "createVacationForm";

    @Autowired
    private CreateVacationFormValidator createVacationFormValidator;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private DictionaryItemDAO dictionaryItemDAO;

    @Autowired
    private VacationDAO vacationDAO;

    @RequestMapping(value = "/createVacation", method = RequestMethod.GET)
    public String prepareToCreateVacation() {
        final Employee employee = securityService.getSecurityPrincipal().getEmployee();

        return String.format(
                "redirect:/createVacation/%s",
                employee.getId()
        );
    }

    @RequestMapping("/createVacation/{employeeId}")
    public ModelAndView createVacation(
            @PathVariable("employeeId") Integer employeeId,
            @ModelAttribute(CREATE_VACATION_FORM) CreateVacationForm createVacationForm,
            BindingResult result
    ) {
        final Employee employee = employeeService.find(employeeId);
        final Calendar calendar = getCalendar(new Timestamp(java.util.Calendar.getInstance().getTimeInMillis()));
        final Timestamp nextWorkDay = calendarService.getNextWorkDay(calendar, employee.getRegion()).getCalDate();

        createVacationForm.setCalFromDate(DateTimeUtil.formatDate(nextWorkDay));
        createVacationForm.setCalToDate(DateTimeUtil.formatDate(nextWorkDay));

        return getModelAndView(employee);
    }

    private ModelAndView getModelAndView(Employee employee) {
        final ModelAndView modelAndView = new ModelAndView("createVacation");

        modelAndView.addObject("vacationTypes", dictionaryItemDAO.getItemsByDictionaryId(VacationTypesEnum.DICT_ID));
        modelAndView.addObject("employee", employee);
        modelAndView.addObject("typeWithRequiredComment", CreateVacationFormValidator.TYPE_WITH_REQUIRED_COMMENT);

        return modelAndView;
    }

    private Calendar getCalendar(Timestamp date) {
        final Calendar calendar = new Calendar();

        calendar.setCalDate(date);

        return calendar;
    }

    @RequestMapping(value = "/getExitToWork/{employeeId}/{date}", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getExitToWork(
            @PathVariable("employeeId") Integer employeeId,
            @PathVariable("date") String dateString
    ) {
        final Timestamp date = DateTimeUtil.stringToTimestamp(dateString, CreateVacationForm.DATE_FORMAT);
        final Employee employee = employeeService.find(employeeId);

        return String.format(
                "Выход на работу: %s",
                DateFormatUtils.format(
                    calendarService.getNextWorkDay(getCalendar(date), employee.getRegion()).getCalDate(),
                    CreateVacationForm.DATE_FORMAT
                )
        );
    }

    @RequestMapping(value = "/validateAndCreateVacation/{employeeId}/{approved}", method = RequestMethod.POST)
    public ModelAndView validateAndCreateVacation(
            @PathVariable("employeeId") Integer employeeId,
            @PathVariable("approved") Integer approved,
            @ModelAttribute(CREATE_VACATION_FORM) CreateVacationForm createVacationForm,
            BindingResult bindingResult
    ) {
        createVacationFormValidator.validate(createVacationForm, bindingResult);

        final Employee employee = employeeService.find(employeeId);

        if (bindingResult.hasErrors()) {
            return getModelAndView(employee);
        }

        final Employee curEmployee = securityService.getSecurityPrincipal().getEmployee();

        final Vacation vacation = new Vacation();

        vacation.setCreationDate(new Date());
        vacation.setBeginDate(DateTimeUtil.stringToTimestamp(createVacationForm.getCalFromDate()));
        vacation.setEndDate(DateTimeUtil.stringToTimestamp(createVacationForm.getCalToDate()));
        vacation.setComment(createVacationForm.getComment().trim());
        vacation.setType(dictionaryItemDAO.find(createVacationForm.getVacationType()));
        vacation.setAuthor(curEmployee);
        vacation.setEmployee(employee);

        final boolean isApprovedVacation =
                (employeeService.isEmployeeAdmin(curEmployee.getId()) && BooleanUtils.toBoolean(approved));

        vacation.setStatus(dictionaryItemDAO.find(
                isApprovedVacation ? VacationStatusEnum.APPROVED.getId() : VacationStatusEnum.APPROVEMENT_WITH_PM.getId()
        ));

        vacationDAO.store(vacation);

        return new ModelAndView("redirect:../");
    }

    @RequestMapping(value = "/validateAndCreateVacation", method = RequestMethod.GET)
    public String validateAndCreateVacation(
    ) {
        return "redirect:/vacations";
    }
}
