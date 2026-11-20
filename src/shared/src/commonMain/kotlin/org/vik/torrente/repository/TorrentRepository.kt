package org.vik.torrente.repository

import androidx.compose.runtime.Updater
import app.cash.sqldelight.db.SqlDriver
import org.vik.torrente.repository.database.DatabaseDriverFactory
import org.vik.torrente.repository.models.*

import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

import org.vik.torrente.repository.orm.*
import queries.UpdateQueries

class TorrentRepository(val driverFactory : DatabaseDriverFactory) {
    private val driver : SqlDriver;
    val db : TorrentDB;

    init{
        driver = driverFactory.create();
        db = TorrentDB(driver);
    }

}