package com.qagenie.testbe.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
