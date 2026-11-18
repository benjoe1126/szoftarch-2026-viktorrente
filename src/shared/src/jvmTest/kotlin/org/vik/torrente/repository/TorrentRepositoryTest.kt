package org.vik.torrente.repository

import org.vik.torrente.repository.database.DummyDatabaseDriverFactory
import org.vik.torrente.repository.models.FileId
import org.vik.torrente.repository.models.PeerId
import org.vik.torrente.repository.models.PieceId
import org.vik.torrente.repository.models.PieceInfo
import org.vik.torrente.repository.models.TorrentId
import org.vik.torrente.repository.orm.*
import kotlin.test.Test
import kotlin.test.*

class TorrentRepositoryTest {
    class DummyDriverFactory

    fun prepareTest() : TorrentRepository{
        var tr =TorrentRepository(DummyDatabaseDriverFactory());

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
    fun testGetTorrents()
    {
        val tr=prepareTest();
        val result=tr.getTorrents()
        assertEquals(1, result.size);
    }

    @Test
    fun testGetTorrentById()
    {
        val tr=prepareTest();
        val result=tr.getTorrentById(TorrentId(1));
        assertEquals("nigga.org", result?.tracker);
        val result2=tr.getTorrentById(TorrentId(2));
        assertEquals(null, result2);
    }

    @Test
    fun testGetTorrentByName()
    {
        val tr=prepareTest();
        val result=tr.getTorrentByName("nigga");
        assertEquals("nigga.org", result?.tracker);
        val result2=tr.getTorrentByName("supernigga");
        assertEquals(null, result2);
    }

    @Test
    fun testGetPieceById()
    {
        val tr=prepareTest();
        val result=tr.getPieceById(PieceId(TorrentId(1),0));
        assertEquals("globus", result?.hash);
        val result2=tr.getPieceById(PieceId(TorrentId(67),0));
        assertEquals(null, result2);
    }

    @Test
    fun testGetPieceArray()
    {
        val tr=prepareTest();
        val result=tr.getPieceArray(listOf(
            PieceId(TorrentId(1),0),
            PieceId(TorrentId(1),1),
            PieceId(TorrentId(67),0)
        ));
        assertEquals(3, result.size);
        assertEquals("globus", result[0]?.hash);
        assertEquals("mustar", result[1]?.hash);
        assertEquals(null, result[2]);
    }

    @Test
    fun testGetPiecesByTorrent()
    {
        val tr=prepareTest();
        val result=tr.getPiecesByTorrent(TorrentId(1));
        assertEquals(2, result.size);
        assert(result[0].hash.equals("globus") || result[1].hash.equals("globus"));
        assert(result[0].hash.equals("mustar") || result[1].hash.equals("mustar"));
        val result2=tr.getPiecesByTorrent(TorrentId(67));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPiecesByPeer()
    {
        val tr=prepareTest();
        val result=tr.getPiecesByPeer(PeerId(TorrentId(1),"idf.hu"));
        assertEquals(2, result.size);
        assert(result[0].hash.equals("globus") || result[1].hash.equals("globus"));
        assert(result[0].hash.equals("mustar") || result[1].hash.equals("mustar"));
        val result2=tr.getPiecesByPeer(PeerId(TorrentId(1),"amogus.hu"));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetFileById()
    {
        val tr=prepareTest();
        val result=tr.getFileById(FileId(TorrentId(1), 1));
        assertEquals("chungus.txt", result?.name);
        val result2=tr.getFileById(FileId(TorrentId(1), 67));
        assertEquals(null, result2);
    }

    @Test
    fun testGetFileArray()
    {
        val tr=prepareTest();
        val result=tr.getFileArray(listOf(
            FileId(TorrentId(1),0),
            FileId(TorrentId(1),1),
            FileId(TorrentId(67),0)
        ));
        assertEquals(3, result.size);
        assertEquals("big.txt", result[0]?.name);
        assertEquals("chungus.txt", result[1]?.name);
        assertEquals(null, result[2]);
    }

    @Test
    fun testGetFilesByTorrent()
    {
        val tr=prepareTest();
        val result=tr.getFilesByTorrent(TorrentId(1));
        assertEquals(2, result.size);
        assert(result[0].name.equals("big.txt") || result[1].name.equals("big.txt"));
        assert(result[0].name.equals("chungus.txt") || result[1].name.equals("chungus.txt"));
        val result2=tr.getFilesByTorrent(TorrentId(67));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPeerById()
    {
        val tr=prepareTest();
        val result=tr.getPeerById(PeerId(TorrentId(1), "idf.hu"));
        assertEquals("idf.hu", result?.address);
        val result2=tr.getPeerById(PeerId(TorrentId(1), "grindr.com"));
        assertEquals(null, result2);
    }

    @Test
    fun testGetPeerArray()
    {
        val tr=prepareTest();
        val result=tr.getPeerArray(listOf(
            PeerId(TorrentId(1),"idf.hu"),
            PeerId(TorrentId(1),"amogus.hu"),
            PeerId(TorrentId(67),"idf.hu")
        ));
        assertEquals(3, result.size);
        assertEquals("idf.hu", result[0]?.address);
        assertEquals("amogus.hu", result[1]?.address);
        assertEquals(null, result[2]);
    }

    @Test
    fun testGetPeersByPiece()
    {
        val tr=prepareTest();
        val result=tr.getPeersByPiece(PieceId(TorrentId(1),0));
        assertEquals(1, result.size);
        assert(result[0].address.equals("idf.hu"));
        val result2=tr.getPeersByPiece(PieceId(TorrentId(1),67));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPeersByTorrent()
    {
        val tr=prepareTest();
        val result=tr.getPeersByTorrent(TorrentId(1));
        assertEquals(3, result.size);
        assert(result.any { it.address.equals("idf.hu") });
        assert(result.any { it.address.equals("sussy.hu") });
        assert(result.any { it.address.equals("amogus.hu") });
        val result2=tr.getPeersByTorrent(TorrentId(67));
        assertEquals(0, result2.size);
    }
}