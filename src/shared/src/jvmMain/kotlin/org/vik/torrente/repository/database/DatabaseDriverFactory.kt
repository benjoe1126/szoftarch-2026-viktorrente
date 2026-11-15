package org.vik.torrente.repository.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver
    {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:torrent.db");
        return driver;
    }
}
