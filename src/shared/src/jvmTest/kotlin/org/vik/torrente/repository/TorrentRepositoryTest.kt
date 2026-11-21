package org.vik.torrente.repository

import org.vik.torrente.repository.database.DummyDatabaseDriverFactory
import org.vik.torrente.repository.models.FileId
import org.vik.torrente.repository.models.FileInfo
import org.vik.torrente.repository.models.PeerId
import org.vik.torrente.repository.models.PeerInfo
import org.vik.torrente.repository.models.PieceId
import org.vik.torrente.repository.models.PieceInfo
import org.vik.torrente.repository.models.PieceStatus
import org.vik.torrente.repository.models.TorrentId
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentStatus
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
        val result=tr.getTorrentById(TorrentId("hashnigga"));
        assertEquals("nigga.org", result?.tracker);
        val result2=tr.getTorrentById(TorrentId("hashnigga67"));
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
        val result=tr.getPieceById(PieceId(TorrentId("hashnigga"),0));
        assertEquals("globus", result?.hash);
        val result2=tr.getPieceById(PieceId(TorrentId("hashnigga67"),0));
        assertEquals(null, result2);
    }

    @Test
    fun testGetPieceArray()
    {
        val tr=prepareTest();
        val result=tr.getPieceArray(listOf(
            PieceId(TorrentId("hashnigga"),0),
            PieceId(TorrentId("hashnigga"),1),
            PieceId(TorrentId("hashnigga67"),0)
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
        val result=tr.getPiecesByTorrent(TorrentId("hashnigga"));
        assertEquals(2, result.size);
        assert(result[0].hash.equals("globus") || result[1].hash.equals("globus"));
        assert(result[0].hash.equals("mustar") || result[1].hash.equals("mustar"));
        val result2=tr.getPiecesByTorrent(TorrentId("hashnigga67"));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPiecesByPeer()
    {
        val tr=prepareTest();
        val result=tr.getPiecesByPeer(PeerId(TorrentId("hashnigga"),"idf"));
        assertEquals(2, result.size);
        assert(result[0].hash.equals("globus") || result[1].hash.equals("globus"));
        assert(result[0].hash.equals("mustar") || result[1].hash.equals("mustar"));
        val result2=tr.getPiecesByPeer(PeerId(TorrentId("hashnigga"),"amogus"));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetFileById()
    {
        val tr=prepareTest();
        val result=tr.getFileById(FileId(TorrentId("hashnigga"), 1));
        assertEquals("chungus.txt", result?.name);
        val result2=tr.getFileById(FileId(TorrentId("hashnigga"), 67));
        assertEquals(null, result2);
    }

    @Test
    fun testGetFileArray()
    {
        val tr=prepareTest();
        val result=tr.getFileArray(listOf(
            FileId(TorrentId("hashnigga"),0),
            FileId(TorrentId("hashnigga"),1),
            FileId(TorrentId("hashnigga67"),0)
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
        val result=tr.getFilesByTorrent(TorrentId("hashnigga"));
        assertEquals(2, result.size);
        assert(result[0].name.equals("big.txt") || result[1].name.equals("big.txt"));
        assert(result[0].name.equals("chungus.txt") || result[1].name.equals("chungus.txt"));
        val result2=tr.getFilesByTorrent(TorrentId("hashnigga67"));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPeerById()
    {
        val tr=prepareTest();
        val result=tr.getPeerById(PeerId(TorrentId("hashnigga"), "idf"));
        assertEquals("idf.hu", result?.address);
        val result2=tr.getPeerById(PeerId(TorrentId("hashnigga"), "grindr"));
        assertEquals(null, result2);
    }

    @Test
    fun testGetPeerArray()
    {
        val tr=prepareTest();
        val result=tr.getPeerArray(listOf(
            PeerId(TorrentId("hashnigga"),"idf"),
            PeerId(TorrentId("hashnigga"),"amogus"),
            PeerId(TorrentId("hashnigga67"),"idf")
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
        val result=tr.getPeersByPiece(PieceId(TorrentId("hashnigga"),0));
        assertEquals(1, result.size);
        assert(result[0].address.equals("idf.hu"));
        val result2=tr.getPeersByPiece(PieceId(TorrentId("hashnigga"),67));
        assertEquals(0, result2.size);
    }

    @Test
    fun testGetPeersByTorrent()
    {
        val tr=prepareTest();
        val result=tr.getPeersByTorrent(TorrentId("hashnigga"));
        assertEquals(3, result.size);
        assert(result.any { it.address.equals("idf.hu") });
        assert(result.any { it.address.equals("sussy.hu") });
        assert(result.any { it.address.equals("amogus.hu") });
        val result2=tr.getPeersByTorrent(TorrentId("hashnigga67"));
        assertEquals(0, result2.size);
    }

    @Test
    fun testDeleteTorrent()
    {
        val tr=prepareTest();
        assertTrue(tr.deleteTorrent(TorrentId("hashnigga")));
        assertEquals(0, tr.getTorrents().size);
        assertEquals(0, tr.getFilesByTorrent(TorrentId("hashnigga")).size);
        assertEquals(0, tr.getPiecesByTorrent(TorrentId("hashnigga")).size);
        assertEquals(0, tr.getPeersByTorrent(TorrentId("hashnigga")).size);
    }

    @Test
    fun testDeleteFile()
    {
        val tr=prepareTest();
        val file=FileId(TorrentId("hashnigga"), 0);
        assertTrue(tr.deleteFile(file));
        assertNull(tr.getFileById(file));
    }

    @Test
    fun testDeletePiece()
    {
        val tr=prepareTest();
        val piece=PieceId(TorrentId("hashnigga"), 0);
        assertTrue(tr.deletePiece(piece));
        assertNull(tr.getPieceById(piece));
    }

    @Test
    fun testDeletePeer()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId("hashnigga"), "idf.hu");
        assertTrue(tr.deletePeer(peer));
        assertNull(tr.getPeerById(peer));
    }

    @Test
    fun testDeletePeerPieceOffersByPeer()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId("hashnigga"), "idf.hu");
        assertTrue(tr.deletePeerPieceOffers(peer));
        assertEquals(0, tr.getPiecesByPeer(peer).size);
    }

    @Test
    fun testDeletePeerPieceOffersByPiece()
    {
        val tr=prepareTest();
        val piece=PieceId(TorrentId("hashnigga"), 1);
        assertTrue(tr.deletePeerPieceOffers(piece));
        assertEquals(0, tr.getPeersByPiece(piece).size);
    }

    @Test
    fun testInsertTorrent()
    {
        val tr=prepareTest();
        val torrent= TorrentInfo("hashnigga67", "nav.gov.hu", "tax-free robux", 67, TorrentStatus.entries[0]);
        assertTrue(tr.insertTorrent(torrent));
        assertEquals(torrent.tracker, tr.getTorrentByName(torrent.name)?.tracker);
    }

    @Test
    fun testInsertFile()
    {
        val tr=prepareTest();
        val fileIdValid=FileId(TorrentId("hashnigga"), 67);
        val fileIdInvalid=FileId(TorrentId("hashnigga67"), 67);
        val fileValid= FileInfo(fileIdValid.torrent,fileIdValid.number,"sus.amogus",67);
        val fileInvalid= FileInfo(fileIdInvalid.torrent,fileIdInvalid.number,"2sus.2amogus",67);
        assertTrue(tr.insertFile(fileValid));
        assertEquals("sus.amogus", tr.getFileById(fileIdValid)?.name);
        assertFalse(tr.insertFile(fileInvalid));
        assertNull(tr.getFileById(fileIdInvalid));
    }

    @Test
    fun testInsertPiece()
    {
        val tr=prepareTest();
        val pieceIdValid=PieceId(TorrentId("hashnigga"), 67);
        val pieceIdInvalid=PieceId(TorrentId("hashnigga67"), 67);
        val pieceValid= PieceInfo(pieceIdValid.torrent,pieceIdValid.number,"batman",67, PieceStatus.entries[0], "batman.piece");
        val pieceInvalid= PieceInfo(pieceIdInvalid.torrent,pieceIdInvalid.number,"batnigga",67, PieceStatus.entries[0], "batnigga.piece");
        assertTrue(tr.insertPiece(pieceValid));
        assertEquals("batman", tr.getPieceById(pieceIdValid)?.hash);
        assertFalse(tr.insertPiece(pieceInvalid));
        assertNull(tr.getPieceById(pieceIdInvalid));
    }

    @Test
    fun testInsertPeer()
    {
        val tr=prepareTest();
        val peerIdValid=PeerId(TorrentId("hashnigga"), "parkside");
        val peerIdInvalid=PeerId(TorrentId("hashnigga67"), "kongstrong");
        val peerValid= PeerInfo(peerIdValid.torrent, peerIdValid.idHash, "parkside.il");
        val peerInvalid= PeerInfo(peerIdInvalid.torrent,peerIdInvalid.idHash, "kongstrong.il");
        assertTrue(tr.insertPeer(peerValid));
        assertEquals(peerIdValid.idHash, tr.getPeerById(peerIdValid)?.idHash);
        assertFalse(tr.insertPeer(peerInvalid));
        assertNull(tr.getPeerById(peerIdInvalid));
    }

    @Test
    fun testInsertTorrentFullData()
    {
        val tr=prepareTest();

        val torrent=TorrentInfo("hashnigga67", "bigya.hu","john bigyahu", 67, TorrentStatus.entries[0]);
        val filesValid=listOf<FileInfo>(
            FileInfo(torrent.getId(),0,"john", 67),
            FileInfo(torrent.getId(),1,"bigyahu", 67),
        );
        val piecesValid=listOf<PieceInfo>(
            PieceInfo(torrent.getId(), 0,"john", 67, PieceStatus.entries[0], "john.piece"),
            PieceInfo(torrent.getId(),1, "bigyahu", 67,  PieceStatus.entries[0], "bigyahu.piece")
        );
        val peersValid=listOf<PeerInfo>(
            PeerInfo(torrent.getId(), "chatgps","chatgps.hu"),
            PeerInfo(torrent.getId(), "kms","kms.hu")
        );

        assertTrue(tr.insertTorrentFullData(torrent, piecesValid, filesValid, peersValid));
        assertNotNull(tr.getTorrentByName(torrent.name));
        val torrentId=tr.getTorrentByName(torrent.name)!!.getId();
        assertEquals(2, tr.getPiecesByTorrent(torrentId).size);
        assertEquals(2,tr.getFilesByTorrent(torrentId).size);
        assertEquals(2,tr.getPeersByTorrent(torrentId).size);
    }

    @Test
    fun testInsertPeerOffering()
    {
        val tr=prepareTest();
        val peerValid=PeerId(TorrentId("hashnigga"), "sussy");
        val peerInvalid=PeerId(TorrentId("hashnigga67"), "idf");
        val pieceValid=PieceId(TorrentId("hashnigga"),0);
        val pieceInvalid=PieceId(TorrentId("hashnigga67"),0);
        assertTrue(tr.insertPeerOffering(peerValid,pieceValid));
        assertFalse(tr.insertPeerOffering(peerInvalid,pieceValid));
        assertFalse(tr.insertPeerOffering(peerValid,pieceInvalid));
    }

    @Test
    fun testUpdateTorrent()
    {
        val tr=prepareTest();
        val torrent=TorrentInfo("hashnigga","rizzler.org","rizzler",67, TorrentStatus.entries[0]);
        assertTrue(tr.updateTorrent(torrent));
        val newTorrent=tr.getTorrentById(torrent.getId());
        assertNotNull(newTorrent);
        assertEquals(torrent, newTorrent);
    }

    @Test
    fun testUpdatePiece()
    {
        val tr=prepareTest();
        val piece=PieceInfo(TorrentId("hashnigga"),0,"rizzler",6767,PieceStatus.entries[0],"rizzler.piece");
        assertTrue(tr.updatePiece(piece));
        val onePiece=tr.getPieceById(piece.getId());
        assertNotNull(onePiece);
        assertEquals(piece, onePiece);
    }

    @Test
    fun testUpdateFile()
    {
        val tr=prepareTest();
        val file=FileInfo(TorrentId("hashnigga"),0,"rizzler.exe",69);
        assertTrue(tr.updateFile(file));
        val newFile=tr.getFileById(file.getId());
        assertNotNull(newFile);
        assertEquals(file, newFile);
    }

    @Test
    fun testUpdatePeer()
    {
        val tr=prepareTest();
        val peerValid=PeerInfo(TorrentId("hashnigga"),"idf","rizzler.exe");
        assertTrue(tr.updatePeer(peerValid));
        val newPeer=tr.getPeerById(peerValid.getId());
        assertNotNull(newPeer);
        assertEquals("rizzler.exe", newPeer.address);
        val peerInvalid=PeerInfo(TorrentId("hashnigga67"),"idf","rizzler.exe");
        assertFalse(tr.updatePeer(peerInvalid));
    }

    @Test
    fun testUpdatePeerPieceOffering()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId("hashnigga"),"idf");
        val piecesValid=listOf<PieceId>(
            PieceId(TorrentId("hashnigga"),0)
        );
        val piecesInvalid=listOf<PieceId>(
            PieceId(TorrentId("hashnigga"),0),
            PieceId(TorrentId("hashnigga"),67)
        );

        assertFalse(tr.updatePeerOffering(peer,piecesInvalid));
        assertEquals(2,tr.getPiecesByPeer(peer).size);
        assertTrue(tr.updatePeerOffering(peer,piecesValid));
        assertEquals(1,tr.getPiecesByPeer(peer).size);
    }
}