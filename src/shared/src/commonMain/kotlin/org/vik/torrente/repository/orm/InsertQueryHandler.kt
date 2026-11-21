package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*
import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

//torrent id is not used
fun TorrentRepository.insertTorrent(torrent:TorrentInfo) : Boolean{
    return runCatching { db.insertQueries.insertTorrent(torrent.hash, torrent.name, torrent.tracker, torrent.length, torrent.status.ordinal.toLong()) }.isSuccess;
}

fun TorrentRepository.insertFile(file:FileInfo) : Boolean{
    return runCatching { db.insertQueries.insertFile(file.name, file.length, file.torrent.hash, file.number) }.isSuccess;
}

fun TorrentRepository.insertPiece(piece:PieceInfo):Boolean{
    return runCatching{ db.insertQueries.insertPiece(piece.hash ,piece.length, piece.torrent.hash, piece.number, piece.status.ordinal.toLong(), piece.downloadPath)}.isSuccess;
}

fun TorrentRepository.insertPeer(peer:PeerInfo):Boolean{
    return runCatching{db.insertQueries.insertPeer(peer.address,peer.idHash, peer.torrent.hash,peer.status.ordinal.toLong())}.isSuccess;
}

/*
ids are automatically created, a placeholder value is sufficient
 */
fun TorrentRepository.insertTorrentFullData(torrent:TorrentInfo, pieces:List<PieceInfo>, files:List<FileInfo>, peers:List<PeerInfo>) : Boolean
{
    val tid=torrent.getId();

    return db.transactionWithResult {
        if(!insertTorrent(torrent))
            rollback(false);
        val torrent = getTorrentByName(torrent.name) ?: rollback(false);

        for(piece in pieces)
        {
            if(piece.torrent!=tid||!insertPiece(piece))
                rollback(false);
        }

        for(file in files)
        {
            if(file.torrent!=tid||!insertFile(file))
                rollback(false);
        }

        for(peer in peers)
        {
            if(peer.torrent!=tid||!insertPeer(peer))
                rollback(false);
        }

        true
    } as Boolean;
}

fun TorrentRepository.insertPeerOffering(peer:PeerId, piece:PieceId):Boolean {
    if(peer.torrent != piece.torrent)
        return false;

    return runCatching { db.transactionWithResult {
        db.insertQueries.insertPeerOffering(peer.torrent.hash, peer.idHash, piece.number);
        if(db.insertQueries.affectedRows().executeAsOne().toInt()!=1)
            throw Exception();
    }}.isSuccess;
}