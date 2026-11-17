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
torrent id is not used, a placeholder value is sufficient
 */
fun TorrentRepository.insertTorrentFullData(torrent:TorrentInfo, pieces:List<PieceInfo>, files:List<FileInfo>, peers:List<PeerInfo>) : Boolean
{
    return db.transactionWithResult {
        if(!insertTorrent(torrent))
            rollback(false);

        for(piece in pieces)
            if(!insertPiece(piece))
                rollback(false);

        for(file in files)
            if(!insertFile(file))
                rollback(false);

        for(peer in peers)
            if(!insertPeer(peer))
                rollback(false);

        true
    } as Boolean;
}

fun TorrentRepository.insertPeerOffering(peer:PeerId, piece:PieceId):Boolean {
    if(peer.torrent_id.id != piece.torrent_id.id)
        return false;

    return db.transactionWithResult {
        db.insertQueries.insertPeerOffering(peer.torrent_id.id, peer.address, piece.number);
        db.insertQueries.affectedRows().executeAsOne().toInt()==1;
    }
}