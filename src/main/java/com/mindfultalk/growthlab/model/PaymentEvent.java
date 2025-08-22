package com.mindfultalk.growthlab.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "payment_id", nullable = false)
    private String paymentId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "amount")
    private Double amount;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "error_description", length = 1000)
    private String errorDescription;
    
    @Column(name = "created_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
    
    @Column(name = "subscription_id")
    private Long subscriptionId;
    
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;

    // Default constructor
    public PaymentEvent() {
    }
    
    // Constructor with fields
    public PaymentEvent(Long id, String eventType, String paymentId, String orderId, Double amount, String status,
			String errorCode, String errorDescription, OffsetDateTime createdAt, String rawPayload, Long subscriptionId,
			UUID uuid) {
		super();
		this.id = id;
		this.eventType = eventType;
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.amount = amount;
		this.status = status;
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
		this.createdAt = createdAt;
		this.rawPayload = rawPayload;
		this.subscriptionId = subscriptionId;
		this.uuid = uuid;
	}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

	public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
 @Override
	public String toString() {
		return "PaymentEvent [id=" + id + ", eventType=" + eventType + ", paymentId=" + paymentId + ", orderId="
				+ orderId + ", amount=" + amount + ", status=" + status + ", errorCode=" + errorCode
				+ ", errorDescription=" + errorDescription + ", createdAt=" + createdAt + ", rawPayload=" + rawPayload
				+ ", subscriptionId=" + subscriptionId + ", uuid=" + uuid + "]";
	}

	// Generate UUID before persistence
    @PrePersist
    private void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
}