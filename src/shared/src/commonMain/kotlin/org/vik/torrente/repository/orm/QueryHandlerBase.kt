package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import queries.TorrentDB

abstract class QueryHandlerBase(val tr : TorrentRepository)