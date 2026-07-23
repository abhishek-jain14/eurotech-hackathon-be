package com.qagenie.testbe.testdata.entity;

import com.qagenie.testbe.common.audit.AuditableEntity;
import com.qagenie.testbe.application.entity.Application;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "TEST_DATA")
public class TestData extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TEST_DATA_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Application application;

    @Column(name = "RECORD_NAME", nullable = false, length = 150)
    private String recordName;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODE", nullable = false, length = 20)
    private TestDataMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private TestDataStatus status = TestDataStatus.VALID;

    @Lob
    @Column(name = "FIELDS_JSON")
    private String fieldsJson;
}
