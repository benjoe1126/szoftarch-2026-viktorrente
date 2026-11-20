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

    @Test
    fun testDeleteTorrent()
    {
        val tr=prepareTest();
        assertTrue(tr.deleteTorrent(TorrentId(1)));
        assertEquals(0, tr.getTorrents().size);
        assertEquals(0, tr.getFilesByTorrent(TorrentId(1)).size);
        assertEquals(0, tr.getPiecesByTorrent(TorrentId(1)).size);
        assertEquals(0, tr.getPeersByTorrent(TorrentId(1)).size);
    }

    @Test
    fun testDeleteFile()
    {
        val tr=prepareTest();
        val file=FileId(TorrentId(1), 0);
        assertTrue(tr.deleteFile(file));
        assertNull(tr.getFileById(file));
    }

    @Test
    fun testDeletePiece()
    {
        val tr=prepareTest();
        val piece=PieceId(TorrentId(1), 0);
        assertTrue(tr.deletePiece(piece));
        assertNull(tr.getPieceById(piece));
    }

    @Test
    fun testDeletePeer()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId(1), "idf.hu");
        assertTrue(tr.deletePeer(peer));
        assertNull(tr.getPeerById(peer));
    }

    @Test
    fun testDeletePeerPieceOffersByPeer()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId(1), "idf.hu");
        assertTrue(tr.deletePeerPieceOffers(peer));
        assertEquals(0, tr.getPiecesByPeer(peer).size);
    }

    @Test
    fun testDeletePeerPieceOffersByPiece()
    {
        val tr=prepareTest();
        val piece=PieceId(TorrentId(1), 1);
        assertTrue(tr.deletePeerPieceOffers(piece));
        assertEquals(0, tr.getPeersByPiece(piece).size);
    }

    @Test
    fun testInsertTorrent()
    {
        val tr=prepareTest();
        val torrent= TorrentInfo(TorrentId.PLACEHOLDER, "nav.gov.hu", "tax-free robux", 67);
        assertTrue(tr.insertTorrent(torrent));
        assertEquals(torrent.tracker, tr.getTorrentByName(torrent.name)?.tracker);
    }

    @Test
    fun testInsertFile()
    {
        val tr=prepareTest();
        val fileIdValid=FileId(TorrentId(1), 67);
        val fileIdInvalid=FileId(TorrentId(67), 67);
        val fileValid= FileInfo(FileId.PLACEHOLDER,"sus.amogus",67,fileIdValid.number,fileIdValid.torrent_id);
        val fileInvalid= FileInfo(FileId.PLACEHOLDER,"2sus.2amogus",67,fileIdInvalid.number,fileIdInvalid.torrent_id);
        assertTrue(tr.insertFile(fileValid));
        assertEquals("sus.amogus", tr.getFileById(fileIdValid)?.name);
        assertFalse(tr.insertFile(fileInvalid));
        assertNull(tr.getFileById(fileIdInvalid));
    }

    @Test
    fun testInsertPiece()
    {
        val tr=prepareTest();
        val pieceIdValid=PieceId(TorrentId(1), 67);
        val pieceIdInvalid=PieceId(TorrentId(67), 67);
        val pieceValid= PieceInfo(PieceId.PLACEHOLDER,"batman",67,pieceIdValid.number, PieceStatus.entries[0], "batman.piece",pieceIdValid.torrent_id);
        val pieceInvalid= PieceInfo(PieceId.PLACEHOLDER,"batnigga",67,pieceIdInvalid.number, PieceStatus.entries[0], "batnigga.piece",pieceIdInvalid.torrent_id);
        assertTrue(tr.insertPiece(pieceValid));
        assertEquals("batman", tr.getPieceById(pieceIdValid)?.hash);
        assertFalse(tr.insertPiece(pieceInvalid));
        assertNull(tr.getPieceById(pieceIdInvalid));
    }

    @Test
    fun testInsertPeer()
    {
        val tr=prepareTest();
        val peerIdValid=PeerId(TorrentId(1), "parkside.il");
        val peerIdInvalid=PeerId(TorrentId(67), "kongstrong.il");
        val peerValid= PeerInfo(PeerId.PLACEHOLDER, peerIdValid.address, peerIdValid.torrent_id);
        val peerInvalid= PeerInfo(PeerId.PLACEHOLDER,peerIdInvalid.address,peerIdInvalid.torrent_id);
        assertTrue(tr.insertPeer(peerValid));
        assertEquals(peerIdValid.address, tr.getPeerById(peerIdValid)?.address);
        assertFalse(tr.insertPeer(peerInvalid));
        assertNull(tr.getPeerById(peerIdInvalid));
    }

    @Test
    fun testInsertTorrentFullData()
    {
        val tr=prepareTest();

        val torrent=TorrentInfo(TorrentId.PLACEHOLDER, "bigya.hu","john bigyahu", 67);
        val filesValid=listOf<FileInfo>(
            FileInfo(FileId.PLACEHOLDER,"john", 67, 0, TorrentId.PLACEHOLDER),
            FileInfo(FileId.PLACEHOLDER,"bigyahu", 67, 1, TorrentId.PLACEHOLDER),
        );
        val piecesValid=listOf<PieceInfo>(
            PieceInfo(PieceId.PLACEHOLDER, "john", 67, 0, PieceStatus.entries[0], "john.piece", TorrentId.PLACEHOLDER),
            PieceInfo(PieceId.PLACEHOLDER, "bigyahu", 67, 1, PieceStatus.entries[0], "bigyahu.piece", TorrentId.PLACEHOLDER)
        );
        val peersValid=listOf<PeerInfo>(
            PeerInfo(PeerId.PLACEHOLDER, "chatgps.hu", TorrentId.PLACEHOLDER),
            PeerInfo(PeerId.PLACEHOLDER, "kms.hu", TorrentId.PLACEHOLDER)
        );

        assertTrue(tr.insertTorrentFullData(torrent, piecesValid, filesValid, peersValid));
        assertNotNull(tr.getTorrentByName(torrent.name));
        val torrentId=tr.getTorrentByName(torrent.name)!!.id;
        assertEquals(2, tr.getPiecesByTorrent(torrentId).size);
        assertEquals(2,tr.getFilesByTorrent(torrentId).size);
        assertEquals(2,tr.getPeersByTorrent(torrentId).size);
    }

    @Test
    fun testInsertPeerOffering()
    {
        val tr=prepareTest();
        val peerValid=PeerId(TorrentId(1), "sussy.hu");
        val peerInvalid=PeerId(TorrentId(67), "idf.hu");
        val pieceValid=PieceId(TorrentId(1),0);
        val pieceInvalid=PieceId(TorrentId(67),0);
        assertTrue(tr.insertPeerOffering(peerValid,pieceValid));
        assertFalse(tr.insertPeerOffering(peerInvalid,pieceValid));
        assertFalse(tr.insertPeerOffering(peerValid,pieceInvalid));
    }

    @Test
    fun testUpdateTorrent()
    {
        val tr=prepareTest();
        val torrent=TorrentInfo(TorrentId(1),"rizzler.org","rizzler",67);
        assertTrue(tr.updateTorrent(torrent));
        val newTorrent=tr.getTorrentById(torrent.id);
        assertNotNull(newTorrent);
        assertEquals(torrent, newTorrent);
    }

    @Test
    fun testUpdatePiece()
    {
        val tr=prepareTest();
        val piece=PieceInfo(PieceId(TorrentId(1),0),"rizzler",6767,0,PieceStatus.DELETED,"rizzler.piece",TorrentId(1));
        assertTrue(tr.updatePiece(piece));
        val onePiece=tr.getPieceById(piece.id);
        assertNotNull(onePiece);
        assertEquals(piece, onePiece);
    }

    @Test
    fun testUpdateFile()
    {
        val tr=prepareTest();
        val file=FileInfo(FileId(TorrentId(1),0),"rizzler.exe",69,0, TorrentId(1));
        assertTrue(tr.updateFile(file));
        val newFile=tr.getFileById(file.id);
        assertNotNull(newFile);
        assertEquals(file, newFile);
    }

    @Test
    fun testUpdatePeerPieceOffering()
    {
        val tr=prepareTest();
        val peer=PeerId(TorrentId(1),"idf.hu");
        val piecesValid=listOf<PieceId>(
            PieceId(TorrentId(1),0)
        );
        val piecesInvalid=listOf<PieceId>(
            PieceId(TorrentId(1),0),
            PieceId(TorrentId(1),67)
        );

        assertFalse(tr.updatePeerOffering(peer,piecesInvalid));
        assertEquals(2,tr.getPiecesByPeer(peer).size);
        assertTrue(tr.updatePeerOffering(peer,piecesValid));
        assertEquals(1,tr.getPiecesByPeer(peer).size);
    }
}