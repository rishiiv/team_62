# Third-party JARs

Place the **PostgreSQL JDBC driver** JAR here so the app can connect to the database at runtime.

- Download from: https://jdbc.postgresql.org/download/
- Example filename: `postgresql-42.7.10.jar`
- The run script looks for `postgresql-*.jar` in this directory first; you can also set `PG_JDBC_JAR` to a full path if the driver is elsewhere.
