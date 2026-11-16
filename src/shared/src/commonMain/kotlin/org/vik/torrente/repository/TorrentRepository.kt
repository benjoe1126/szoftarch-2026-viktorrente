package org.vik.torrente.repository

import app.cash.sqldelight.db.SqlDriver
import org.vik.torrente.repository.database.DatabaseDriverFactory
import org.vik.torrente.repository.models.*

import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

import org.vik.torrente.repository.orm.*

class TorrentRepository(val driverFactory : DatabaseDriverFactory) {
    val driver : SqlDriver;
    val db : TorrentDB;

    val selectHandler : SelectQueryHandler;
    val insertHandler : InsertQueryHandler;

    init{
        driver = driverFactory.create();
        db = TorrentDB(driver);

        selectHandler=SelectQueryHandler(this);
        insertHandler=InsertQueryHandler(this);
    }


}