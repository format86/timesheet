package com.aplana.timesheet.dao;

import com.aplana.timesheet.dao.entity.Division;
import com.aplana.timesheet.dao.entity.Employee;
import com.google.common.collect.Iterables;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class EmployeeDAO {
	private static final Logger logger = LoggerFactory.getLogger(EmployeeDAO.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Возвращает сотрудника по идентификатору.
	 * @param id идентификатор сотрудника
	 * @return объект класса Employee либо null, если сотрудник
	 *         с указанным id не найден.
	 */
	@Transactional(readOnly = true)
	public Employee find(Integer id) {
		if (id == null) {
			logger.warn("For unknown reasons, the Employee ID is null.");
			return null;
		}
		return entityManager.find(Employee.class, id);
	}
	
	/**
	 * Возвращает сотрудника по имени.
	 * @param name имя сотрудника
	 * @return объект класса Employee либо null, если сотрудник
	 *         с указанным именем не найден.
	 */
	@Transactional(readOnly = true)
	public Employee find(String name) {
		if ("".equals(name)) { return null; }

		Query query = entityManager.createQuery(
                "from Employee as e where e.name=:name"
        ).setParameter( "name", name );
		try {
            return (Employee) query.getSingleResult();
		} catch (NoResultException e) {
			logger.warn("Employee with name '{}' not found.", name);
		} catch (NonUniqueResultException e) {
			logger.warn("More than one employee with name '{}' was found.", name);
		}
        return null;
	}

    /**
     * Возвращает сотрудника по email
     * @param email сотрудника
     * @return объект класса Employee либо null, если сотрудник
     *         с указанным именем не найден.
     */

    @Transactional(readOnly = true)
    public Employee findByEmail(String email) {
        if ("".equals(email)) { return null; }

        Query query = entityManager.createQuery(
                "select e from Employee as e where e.email=:email"
        ).setParameter( "email", email );
        try {
            Employee result = (Employee) query.getSingleResult();
            // Загружаем детализации, чтобы не было проблем из-за lazy загрузки.
            Hibernate.initialize(result);
            Hibernate.initialize(result.getDivision());
            Hibernate.initialize(result.getManager());
            return result;
        } catch (NoResultException e) {
            logger.warn("Employee with email '{}' not found.", email);
        } catch (NonUniqueResultException e) {
            logger.warn("More than one employee with email '{}' was found.", email);
        }
        return null;
    }


	public Employee findByLdapName(String ldap) {
		Query query = this.entityManager.createQuery(
                "select e from Employee e where e.ldap like :ldap"
        ).setMaxResults( 1 ).setParameter("ldap", "%" + ldap + "%");

		return (Employee) Iterables.getFirst(query.getResultList(), null);
	}

    public List<Employee> getEmployeesForSync() {
        return getEmployeesForSync(null);
    }

    /**
	 * Возвращает список доступных для синхронизации с ldap сотрудников.
	 * @param division Если null, то поиск осуществляется без учета подразделения,
	 * 				   иначе с учётом подразделения
	 * @return список сотрудников для синхронизации
	 */
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<Employee> getEmployeesForSync(Division division) {
		Query query;
		if (division == null) {
			query = entityManager.createQuery(
                    "from Employee as e where e.notToSync=:notToSync"
            );
		} else {
			query = entityManager.createQuery(
                    "from Employee as e where e.notToSync=:notToSync and e.division=:division"
            );
			query.setParameter("division", division);
		}
		query.setParameter("notToSync", false);

        return query.getResultList();
	}

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Employee> getAllEmployeesDivision(Division division) {
        Query query;
        if (division == null) {
            query = entityManager.createQuery("FROM Employee");
        } else {
            query = entityManager.createQuery("FROM Employee AS e WHERE e.division=:division");
            query.setParameter("division", division);
        }

        return query.getResultList();
    }
    
    /**
     * Возвращает список менеджеров для конкретного работника
     * @param employeeId
     * @return List<Employee>
     */
    @Transactional(readOnly = true)
    public List<Employee> getRegionManager (Integer employeeId) {
        Query query = this.entityManager.createQuery(
                "select m.employee from  Employee e, Manager m " +
                "where e.id = :emp_id " +
                    "AND m.division.id = e.division.id " +
                    "AND m.region.id = e.region.id"
        ).setParameter("emp_id", employeeId);

        return query.getResultList();
    }

    /**
     * Возвращает список менеджеров для конкретного региона и подразделения
     * @param regionId
     * @param divisionId
     * @return
     */
    public List<Employee> getRegionManager(Integer regionId, Integer divisionId) {
        Query query = this.entityManager.createQuery(
                "select m.employee from  Manager m " +
                        "WHERE m.division.id = :div_id " +
                        "AND m.region.id = :region_id"
        ).setParameter("region_id", regionId).setParameter("div_id", divisionId);

        return query.getResultList();
    }

	/**
	 * Возвращает список действующих сотрудников указанного подразделения
	 * @param division подразделениe
	 * @return список сотрудников подразделения с идентификатором division
	 */
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<Employee> getEmployees(Division division) {
		Query query;

        Date maxModDate = new Date();

		if (division == null) {
			query = entityManager.createQuery(
                    //действующий сотрудник-который на текущий момент либо не имеет endDate, либо endDate<=cuDate
                    "FROM Employee AS emp " +
                            "WHERE (emp.endDate IS NOT NULL " +
                                "AND emp.endDate >= :curDate) " +
                                "OR (emp.endDate IS NULL) " +
                            "ORDER BY emp.name"
            ).setParameter("curDate", maxModDate);
		} else {
			query = entityManager.createQuery(
                    "FROM Employee AS emp " +
                            "WHERE (emp.division=:division " +
                                "AND emp.endDate IS NOT NULL " +
                                "AND emp.endDate >= :curDate) " +
                                "OR (emp.division=:division " +
                                "AND emp.endDate IS NULL) " +
                            "ORDER BY emp.name"
            ).setParameter("curDate", maxModDate).setParameter("division", division);
		}

        return query.getResultList();
	}

	/**
	 * Сохраняет в базе нового сотрудника, либо обновляет данные уже
	 * существующего сотрудника.
	 * @param employee
	 */
	public Employee save(Employee employee) {
		Employee empMerged = entityManager.merge(employee);
		entityManager.flush();
		logger.info("Persistence context synchronized to the underlying database.");
		logger.debug("Flushed Employee object id = {}", empMerged.getId());
        return empMerged;
	}

    public boolean isNotToSync(Employee employee) {
        Query query;
        query = entityManager.createQuery(
                "FROM Employee AS e WHERE e.email=:email"
        ).setParameter("email", employee.getEmail().trim());

        List<Employee> result = query.getResultList();

        return result != null &&  ! result.isEmpty() && result.get( 0 ).isNotToSync();
    }


	/**
	 * Сохраняет в базе новых сотрудников, либо обновляет данные уже
	 * существующих сотрудников.
	 * @param employee
	 */
	public StringBuffer setEmployees(List<Employee> employees) {
        StringBuffer trace = new StringBuffer();
		for (Employee emp : employees) {
            if ( ! isNotToSync( emp ) ) {
                trace.append( String.format(
                        "%s user: %s %s\n", emp.getId() != null ? "Updated" : "Added", emp.getEmail(), emp.getName()
                ) );

                save(emp);
            } else {
                trace.append(String.format(
                        "\nUser: %s %s marked not_to_sync.(Need update)\n%s\n\n",
                        emp.getEmail(), emp.getName(), emp.toString()));
            }
        }
        trace.append("\n\n");
        return trace;
	}

    public Double getWorkDaysOnIllnessWorked(Employee employee, Date beginDate, Date endDate) {
        Query query = entityManager.createQuery(
                "select sum(tsd.duration) from TimeSheetDetail tsd " +
                        "inner join tsd.timeSheet ts " +
                        "where (tsd.actType.id = 14 or tsd.actType.id = 12 or tsd.actType.id = 13 or tsd.actType.id = 42) " +
                        "and (ts.calDate.calDate between :beginDate and :endDate) " +
                        "and (ts.employee = :employee)");
        query.setParameter("beginDate", beginDate);
        query.setParameter("endDate", endDate);
        query.setParameter("employee", employee);

        if (query.getResultList().get(0) != null){
            return ((Double) query.getResultList().get(0));
        } else {
            return 0d;
        }

    }

    /**
     * @param employeeId
     * @return Date - дата начала работы сотрудника
     */
    public Date getEmployeeFirstWorkDay(Integer employeeId){
        Query query = entityManager.createQuery(
                "SELECT startDate FROM Employee empl WHERE empl.id = :emplId").setParameter("emplId", employeeId);
        Timestamp result = (Timestamp) query.getResultList().get(0);
        return new Date(result.getTime());
    }

    @Transactional(readOnly = true)
    public Employee findByObjectSid(String objectSid) {
        Employee employee = (Employee) Iterables.getFirst(entityManager.createQuery(
                "FROM Employee emp WHERE objectSid = :objectSid"
        ).setParameter("objectSid", objectSid).getResultList(), null);

        if(employee == null) return null;

        Hibernate.initialize(employee.getDivision());
        Hibernate.initialize(employee.getDivision().getLeaderId());
        Hibernate.initialize(employee.getDivision().getLeader());
        Hibernate.initialize(employee.getJob());
        Hibernate.initialize(employee.getManager());
        Hibernate.initialize(employee.getRegion());

        return employee;
    }

    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployeesNotInList(List<Integer> syncedEmployees) {
        return entityManager.createQuery(
                "FROM Employee AS emp " +
                        "WHERE (emp.endDate IS NOT NULL " +
                        "AND emp.endDate >= :curDate) " +
                        "OR (emp.endDate IS NULL) AND emp.id NOT IN :ids"
        ).setParameter("curDate", new Date()).setParameter("ids", syncedEmployees).getResultList();
    }
}