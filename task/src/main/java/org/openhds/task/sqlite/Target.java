package org.openhds.task.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The operations required of the target side of an export.
 */
public interface Target {

    Connection createConnection(File target) throws IOException, SQLException;

    Statement createStatement(Connection c) throws SQLException;

}
