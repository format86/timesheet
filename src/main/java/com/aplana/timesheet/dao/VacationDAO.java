package com.aplana.timesheet.dao;

import com.aplana.timesheet.dao.entity.Vacation;
import com.aplana.timesheet.enums.VacationStatusEnum;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
@Repository
public class VacationDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Список заявлений на отпуск для конкретного сотрудника
     * @param employeeId
     * @return
     */
    public List<Vacation> findVacations(Integer employeeId, Integer year) {
        final Query query =
                entityManager.createQuery("from Vacation v where v.employee.id = :emp_id and (YEAR(v.beginDate) = :year or YEAR(v.endDate) = :year) order by v.beginDate")
                        .setParameter("emp_id", employeeId).setParameter("year", year);

        return query.getResultList();
    }

    @Transactional
    public void store(Vacation vacation) {
        final Vacation mergedVacation = entityManager.merge(vacation);

        entityManager.flush();

        vacation.setId(mergedVacation.getId());
    }

    @Transactional
    public void delete(Vacation vacation) {
        Hibernate.initialize(vacation);

        entityManager.remove(vacation);
    }

    public Long getIntersectVacationsCount(Date fromDate, Date toDate) {
        final Query query = entityManager.createQuery(
                "select count(*) as c " +
                "from Vacation v, DictionaryItem di " +
                "where di.id = :status_id and ((:from_date between v.beginDate and v.endDate) or (:to_date between v.beginDate and v.endDate)) and not v.status = di"
        ).setParameter("from_date", fromDate).setParameter("to_date", toDate).
                setParameter("status_id", VacationStatusEnum.REJECTED.getId());

        return (Long) query.getSingleResult();
    }

    public Vacation findVacation(Integer vacationId) {
        final Query query = entityManager.createQuery("from Vacation v where v.id = :id").setParameter("id", vacationId);

        return (Vacation) query.getSingleResult();
    }

    public List<String> getVacationApprovalEmailList(Integer vacationId) {
        final Query query = entityManager.createQuery("select va.manager.email from VacationApproval va where va.vacation.id = :vac_id")
                .setParameter("vac_id", vacationId);

        return query.getResultList();
    }
}
