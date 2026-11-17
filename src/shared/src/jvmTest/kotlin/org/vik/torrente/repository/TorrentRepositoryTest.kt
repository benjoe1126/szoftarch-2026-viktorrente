package org.vik.torrente.repository

import org.vik.torrente.repository.database.DatabaseDriverFactory
import kotlin.test.Test

class TorrentRepositoryTest {
    fun prepareTest() : TorrentRepository{
        var tr =TorrentRepository(DatabaseDriverFactory());

        tr.db.testQueries.clearPeerHasPiece();
        tr.db.testQueries.clearPeers();
        tr.db.testQueries.clearPieces();
        tr.db.testQueries.clearFiles();
        tr.db.testQueries.clearTorrents();

        tr.db.testQueries.fillTorrents();
        tr.db.testQueries.fillFiles();
        tr.db.testQueries.fillPieces();
        tr.db.testQueries.fillPeers();
        tr.db.testQueries.fillPeerHasPiece();

        return tr;
    }

    @Test
    fun testGetTorrent()
    {
        
    }
}