package com.qagenie.testbe.application.entity;

public enum SpecSource {
    /** Manual file upload via the onboarding/spec upload screen. */
    UPLOAD,
    /** Fetched from the application's resolved swagger/DOM URL (derived or custom). */
    FETCH_URL,
    /** Found by the Change Tracker's background comparison against the live endpoint. */
    DRIFT_DETECTED
}
