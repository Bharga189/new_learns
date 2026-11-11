package io.buffy.cdc.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC helpers for the modules that write to the demo database. */
public final class Jdbc {

    private Jdbc() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(CdcConfig.jdbcUrl(),
                CdcConfig.mysqlUser(), CdcConfig.mysqlPassword());
    }
}
