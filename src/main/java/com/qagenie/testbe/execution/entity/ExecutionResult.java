package com.qagenie.testbe.execution.entity;

import com.qagenie.testbe.scenario.entity.TestScenario;
import com.qagenie.testbe.testdata.entity.TestData;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "EXECUTION_RESULT")
public class ExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESULT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RUN_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExecutionRun executionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCENARIO_ID", nullable = false)
    private TestScenario scenario;

    /** Which TEST_DATA row (Examples row) this result corresponds to - null when the
     * scenario had no substitutable fields and ran once with no per-row data. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEST_DATA_ID")
    private TestData testData;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULT_STATUS", nullable = false, length = 20)
    private ResultStatus resultStatus;

    @Column(name = "RESPONSE_TIME_MS")
    private Long responseTimeMs;

    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "REQUEST_METHOD", length = 10)
    private String requestMethod;

    @Lob
    @Column(name = "REQUEST_URL")
    private String requestUrl;

    /** JSON object of header name -> value, exactly as sent. */
    @Lob
    @Column(name = "REQUEST_HEADERS")
    private String requestHeaders;

    @Lob
    @Column(name = "REQUEST_BODY")
    private String requestBody;

    @Column(name = "RESPONSE_STATUS_CODE")
    private Integer responseStatusCode;

    /** JSON object of header name -> value list, exactly as received. */
    @Lob
    @Column(name = "RESPONSE_HEADERS")
    private String responseHeaders;

    /** Full response body, untruncated (unlike errorMessage's 500-char snippet on failure). */
    @Lob
    @Column(name = "RESPONSE_BODY")
    private String responseBody;

    /** Expected values captured from the linked TestData row at execution time, for expected-vs-actual reporting. */
    @Column(name = "EXPECTED_HTTP_STATUS_CODE")
    private Integer expectedHttpStatusCode;

    @Column(name = "EXPECTED_ERROR_CODE", length = 50)
    private String expectedErrorCode;

    @Column(name = "EXPECTED_ERROR_MSG", length = 1000)
    private String expectedErrorMsg;

    @Lob
    @Column(name = "EXPECTED_RESPONSE_JSON")
    private String expectedResponseJson;
}
