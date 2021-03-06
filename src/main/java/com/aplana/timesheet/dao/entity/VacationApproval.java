package com.aplana.timesheet.dao.entity;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Date;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
@Entity
@Table(name = "vacation_approval")
public class VacationApproval {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "INTEGER NOT NULL")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacation_id", nullable = false)
    @ForeignKey(name = "fk_vacation")
    private Vacation vacation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    @ForeignKey(name = "fk_manager")
    private Employee manager;

    @Column(name = "request_datetime", columnDefinition = "date NOT NULL")
    private Date requestDate;

    @Column(name = "response_datetime", columnDefinition = "date")
    private Date responseDate;

    @Column(name = "uid", columnDefinition = "CHAR(36) NOT NULL")
    private String uid;

    @Column(name = "result", columnDefinition = "BOOLEAN")
    private Boolean result;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Vacation getVacation() {
        return vacation;
    }

    public void setVacation(Vacation vacation) {
        this.vacation = vacation;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("VacationApproval");
        sb.append("{id=").append(id);
        sb.append(", vacation=").append(vacation);
        sb.append(", manager=").append(manager);
        sb.append(", requestDate=").append(requestDate);
        sb.append(", responseDate=").append(responseDate);
        sb.append(", uid='").append(uid).append('\'');
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
