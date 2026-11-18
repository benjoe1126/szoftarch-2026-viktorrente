package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*
import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

//torrent id is not used
fun TorrentRepository.insertTorrent(torrent:TorrentInfo) : Boolean{
    return runCatching { db.insertQueries.insertTorrent(torrent.name, torrent.tracker, torrent.length) }.isSuccess;
}

fun TorrentRepository.insertFile(file:FileInfo) : Boolean{
    return runCatching { db.insertQueries.insertFile(file.name, file.length, file.torrent.id, file.number) }.isSuccess;
}

fun TorrentRepository.insertPiece(piece:PieceInfo):Boolean{
    return runCatching{ db.insertQueries.insertPiece(piece.hash ,piece.length, piece.torrent.id, piece.number, piece.status.ordinal.toLong(), piece.downloadPath)}.isSuccess;
}

fun TorrentRepository.insertPeer(peer:PeerInfo):Boolean{
    return runCatching{db.insertQueries.insertPeer(peer.address, peer.torrent.id)}.isSuccess;
}

/*
ids are automatically created, a placeholder value is sufficient
 */
fun TorrentRepository.insertTorrentFullData(torrent:TorrentInfo, pieces:List<PieceInfo>, files:List<FileInfo>, peers:List<PeerInfo>) : Boolean
{
    return db.transactionWithResult {
        if(!insertTorrent(torrent))
            rollback(false);
        val torrent = getTorrentByName(torrent.name) ?: rollback(false);

        for((_, hash, length, number, status, downloadPath) in pieces)
        {
            val p=PieceInfo(PieceId(torrent.id, number), hash, length, number, status, downloadPath, torrent.id);
            if(!insertPiece(p))
                rollback(false);
        }

        for((_, name, length, number) in files)
        {
            val f=FileInfo(FileId(torrent.id, number), name, length, number, torrent.id);
            if(!insertFile(f))
                rollback(false);
        }

        for((_, address) in peers)
        {
            val p=PeerInfo(PeerId(torrent.id, address), address, torrent.id);
            if(!insertPeer(p))
                rollback(false);
        }

        true
    } as Boolean;
}

fun TorrentRepository.insertPeerOffering(peer:PeerId, piece:PieceId):Boolean {
    if(peer.torrent_id.id != piece.torrent_id.id)
        return false;

    return runCatching { db.transactionWithResult {
        db.insertQueries.insertPeerOffering(peer.torrent_id.id, peer.address, piece.number);
        if(db.insertQueries.affectedRows().executeAsOne().toInt()!=1)
            throw Exception();
    }}.isSuccess;
}