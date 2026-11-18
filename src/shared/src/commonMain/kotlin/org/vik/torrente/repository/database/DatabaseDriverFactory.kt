package org.vik.torrente.repository.database

import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactory {
    fun create(): SqlDriver
}