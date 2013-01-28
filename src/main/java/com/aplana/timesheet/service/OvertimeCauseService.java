package com.aplana.timesheet.service;

import com.aplana.timesheet.dao.DictionaryItemDAO;
import com.aplana.timesheet.dao.OvertimeCauseDAO;
import com.aplana.timesheet.dao.entity.OvertimeCause;
import com.aplana.timesheet.dao.entity.TimeSheet;
import com.aplana.timesheet.enums.OvertimeCauses;
import com.aplana.timesheet.enums.TSEnum;
import com.aplana.timesheet.enums.UnfinishedDayCauses;
import com.aplana.timesheet.form.TimeSheetForm;
import com.aplana.timesheet.properties.TSPropertyProvider;
import com.aplana.timesheet.util.EnumsUtils;
import com.aplana.timesheet.util.TimeSheetConstans;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author eshangareev
 * @version 1.0
 */
@Service("overtimeCauseService")
public class OvertimeCauseService {

    @Autowired
    private OvertimeCauseDAO dao;
    @Autowired
    private DictionaryItemDAO dictionaryItemDAO;
    @Autowired
    private TSPropertyProvider propertyProvider;


    public void store(TimeSheet timeSheet, TimeSheetForm tsForm) {
        if (!isOvertimeCauseNeeeded(tsForm.getTotalDuration())) return;

        OvertimeCause overtimeCause = new OvertimeCause();

        overtimeCause.setOvertimeCause( dictionaryItemDAO.find(tsForm.getOvertimeCause()) );
        overtimeCause.setTimeSheet(timeSheet);
        overtimeCause.setComment(tsForm.getOvertimeCauseComment());

        dao.store(overtimeCause);
    }

    public String getCauseName(TimeSheetForm tsForm) {
        Integer overtimeCauseId = tsForm.getOvertimeCause();
        if (!isOvertimeCauseNeeeded(tsForm.getTotalDuration()) || overtimeCauseId == null) return null;

        OvertimeCauses overtimeCause = EnumsUtils.tryFindById(overtimeCauseId, OvertimeCauses.class);
        UnfinishedDayCauses unfinishedDayCauses = EnumsUtils.tryFindById(overtimeCauseId, UnfinishedDayCauses.class);
        TSEnum cause = overtimeCause == null? unfinishedDayCauses : overtimeCause;
        if (Preconditions.checkNotNull(cause).getName().equals("Другой")) {
            return Preconditions.checkNotNull(tsForm.getOvertimeCauseComment());
        } else {
            return dictionaryItemDAO.find(overtimeCauseId).getValue();
        }
    }

    public boolean isOvertimeCauseNeeeded(double totalDuration) {
        return Math.abs(totalDuration - TimeSheetConstans.WORK_DAY_DURATION) > propertyProvider.getOvertimeThreshold();
    }
}
