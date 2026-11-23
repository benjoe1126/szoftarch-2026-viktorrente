package org.vik.torrente.repository.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import queries.TorrentDB
import java.util.Properties

class JvmDatabaseDriverFactory : DatabaseDriverFactory {
    override fun create(): SqlDriver
    {
        /*val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:torrent.db",
            properties = Properties().apply {
                put("foreign_keys", "true")
            }
        );*/
        val driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            properties = Properties().apply {
                put("foreign_keys", "true")
            }
        );
        runCatching {
            TorrentDB.Schema.create(driver);
        }
        return driver;
    }
}
