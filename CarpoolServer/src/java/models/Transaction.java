/*
 * Carpool Server, DMS Assignment 3
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
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Administrator
 */
@Entity
@Table(name = "transaction")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Transaction.findAll", query = "SELECT t FROM Transaction t"),
    @NamedQuery(name = "Transaction.findByTransactionId", query = "SELECT t FROM Transaction t WHERE t.transactionId = :transactionId"),
    @NamedQuery(name = "Transaction.findByStatus", query = "SELECT t FROM Transaction t WHERE t.status = :status"),
    @NamedQuery(name = "Transaction.findByCollectedDt", query = "SELECT t FROM Transaction t WHERE t.collectedDt = :collectedDt"),
    @NamedQuery(name = "Transaction.findByCollectedLat", query = "SELECT t FROM Transaction t WHERE t.collectedLat = :collectedLat"),
    @NamedQuery(name = "Transaction.findByCollectedLng", query = "SELECT t FROM Transaction t WHERE t.collectedLng = :collectedLng"),
    @NamedQuery(name = "Transaction.findByDroppedDt", query = "SELECT t FROM Transaction t WHERE t.droppedDt = :droppedDt"),
    @NamedQuery(name = "Transaction.findByDroppedLat", query = "SELECT t FROM Transaction t WHERE t.droppedLat = :droppedLat"),
    @NamedQuery(name = "Transaction.findByDroppedLng", query = "SELECT t FROM Transaction t WHERE t.droppedLng = :droppedLng"),
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
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private int status;
    @Column(name = "collected_dt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date collectedDt;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "collected_lat")
    private Double collectedLat;
    @Column(name = "collected_lng")
    private Double collectedLng;
    @Column(name = "dropped_dt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date droppedDt;
    @Column(name = "dropped_lat")
    private Double droppedLat;
    @Column(name = "dropped_lng")
    private Double droppedLng;
    @JoinColumn(name = "passenger_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private User passengerId;
    @JoinColumn(name = "driver_id", referencedColumnName = "user_id")
    @ManyToOne(optional = false)
    private User driverId;

    public Transaction() {
    }

    public Transaction(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Transaction(Integer transactionId, int status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public Date getDroppedDt() {
        return droppedDt;
    }

    public void setDroppedDt(Date droppedDt) {
        this.droppedDt = droppedDt;
    }

    public Double getDroppedLat() {
        return droppedLat;
    }

    public void setDroppedLat(Double droppedLat) {
        this.droppedLat = droppedLat;
    }

    public Double getDroppedLng() {
        return droppedLng;
    }

    public void setDroppedLng(Double droppedLng) {
        this.droppedLng = droppedLng;
    }

    public User getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(User passengerId) {
        this.passengerId = passengerId;
    }

    public User getDriverId() {
        return driverId;
    }

    public void setDriverId(User driverId) {
        this.driverId = driverId;
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
