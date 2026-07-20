package com.qagenie.testbe.application.entity;

public enum SpecVersionStatus {
    /** Exactly one CURRENT per application at any time - what scenarios/execution read. */
    CURRENT,
    /** Newly detected content differing from CURRENT, awaiting human review. */
    PENDING,
    /** Was CURRENT once, replaced by a later approved version. Kept for history/diffing. */
    SUPERSEDED,
    /** A PENDING version a reviewer explicitly declined. Terminal - won't be re-flagged for the same hash. */
    REJECTED
}
