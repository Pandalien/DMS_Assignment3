/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author andyc
 */
@Entity
@Table(name = "transaction")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Transaction.findAll", query = "SELECT t FROM Transaction t"),
    @NamedQuery(name = "Transaction.findByTransactionId", query = "SELECT t FROM Transaction t WHERE t.transactionId = :transactionId"),
    @NamedQuery(name = "Transaction.findByCollectedDt", query = "SELECT t FROM Transaction t WHERE t.collectedDt = :collectedDt"),
    @NamedQuery(name = "Transaction.findByCollectedLat", query = "SELECT t FROM Transaction t WHERE t.collectedLat = :collectedLat"),
    @NamedQuery(name = "Transaction.findByCollectedLng", query = "SELECT t FROM Transaction t WHERE t.collectedLng = :collectedLng"),
    @NamedQuery(name = "Transaction.findByStatus", query = "SELECT t FROM Transaction t WHERE t.status = :status"),
    @NamedQuery(name = "Transaction.findByCompletedDt", query = "SELECT t FROM Transaction t WHERE t.completedDt = :completedDt"),
    @NamedQuery(name = "Transaction.findByCompletedLat", query = "SELECT t FROM Transaction t WHERE t.completedLat = :completedLat"),
    @NamedQuery(name = "Transaction.findByCompletedLng", query = "SELECT t FROM Transaction t WHERE t.completedLng = :completedLng"),
    @NamedQuery(name = "Transaction.findByPendingDt", query = "SELECT t FROM Transaction t WHERE t.pendingDt = :pendingDt"),
    @NamedQuery(name = "Transaction.findByPassengerId", query = "SELECT t FROM Transaction t WHERE t.passengerId = :passengerId"),
    @NamedQuery(name = "Transaction.findByDriverId", query = "SELECT t FROM Transaction t WHERE t.driverId = :driverId")
})
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "transaction_id")
    private Integer transactionId;
    @Column(name = "collected_dt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date collectedDt;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "collected_lat")
    private Double collectedLat;
    @Column(name = "collected_lng")
    private Double collectedLng;
    @Column(name = "status")
    private Integer status;
    @Column(name = "completed_dt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedDt;
    @Column(name = "completed_lat")
    private Double completedLat;
    @Column(name = "completed_lng")
    private Double completedLng;
    @Column(name = "pending_dt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date pendingDt;
    @JoinColumn(name = "driver_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private User driverId;
    @JoinColumn(name = "passenger_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private User passengerId;

    public Transaction() {
    }

    public Transaction(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Date getCollectedDt() {
        return collectedDt;
    }

    public void setCollectedDt(Date collectedDt) {
        this.collectedDt = collectedDt;
    }

    public Double getCollectedLat() {
        return collectedLat;
    }

    public void setCollectedLat(Double collectedLat) {
        this.collectedLat = collectedLat;
    }

    public Double getCollectedLng() {
        return collectedLng;
    }

    public void setCollectedLng(Double collectedLng) {
        this.collectedLng = collectedLng;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCompletedDt() {
        return completedDt;
    }

    public void setCompletedDt(Date completedDt) {
        this.completedDt = completedDt;
    }

    public Double getCompletedLat() {
        return completedLat;
    }

    public void setCompletedLat(Double completedLat) {
        this.completedLat = completedLat;
    }

    public Double getCompletedLng() {
        return completedLng;
    }

    public void setCompletedLng(Double completedLng) {
        this.completedLng = completedLng;
    }

    public Date getPendingDt() {
        return pendingDt;
    }

    public void setPendingDt(Date pendingDt) {
        this.pendingDt = pendingDt;
    }

    public User getDriverId() {
        return driverId;
    }

    public void setDriverId(User driverId) {
        this.driverId = driverId;
    }

    public User getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(User passengerId) {
        this.passengerId = passengerId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionId != null ? transactionId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction) object;
        if ((this.transactionId == null && other.transactionId != null) || (this.transactionId != null && !this.transactionId.equals(other.transactionId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "models.Transaction[ transactionId=" + transactionId + " ]";
    }
    
}
