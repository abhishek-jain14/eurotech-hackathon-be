-- Seed data for the in-memory H2 database only (spring.sql.init.mode
-- defaults to "embedded", so this never runs against the real Oracle DB).
-- Recreated on every startup since H2 uses hibernate.hbm2ddl.auto=create-drop.

INSERT INTO PROJECT (NAME, DESCRIPTION, JIRA_URL, SPEC_PATH_SUFFIX, SPEC_AUTH_TYPE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
VALUES ('Test', 'Seed    project created automatically on H2 startup for local development.', NULL, '/v3/api-docs', 'NONE', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system');

INSERT INTO ENVIRONMENT_CONFIG (PROJECT_ID, ENV_NAME, CONFIG_TYPE, BASE_URL, IS_ACTIVE, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)
SELECT PROJECT_ID, 'Dev', 'SwaggerUrl', 'http://localhost:8081', 1, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system'
FROM PROJECT WHERE NAME = 'Test';