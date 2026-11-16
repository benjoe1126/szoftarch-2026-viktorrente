package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*
import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

class InsertQueryHandler (tr_ : TorrentRepository) : QueryHandlerBase(tr_){

    //torrent id is not used
    fun insertTorrent(torrent:TorrentInfo) : Boolean{
        return runCatching { tr.db.insertQueries.insertTorrent(torrent.name, torrent.tracker, torrent.length) }.isSuccess;
    }

    fun insertFile(file:FileInfo) : Boolean{
        return runCatching { tr.db.insertQueries.insertFile(file.name, file.length, file.torrent.id, file.number) }.isSuccess;
    }

    fun insertPiece(piece:PieceInfo):Boolean{
        return runCatching{ tr.db.insertQueries.insertPiece(piece.hash ,piece.length, piece.torrent.id, piece.number, piece.status.ordinal.toLong(), piece.downloadPath)}.isSuccess;
    }

    fun insertPeer(peer:PeerInfo):Boolean{
        return runCatching{tr.db.insertQueries.insertPeer(peer.address, peer.torrent.id)}.isSuccess;
    }

    /*
    torrent id is not used, a placeholder value is sufficient
     */
    fun insertTorrentFullData(torrent:TorrentInfo, pieces:List<PieceInfo>, files:List<FileInfo>, peers:List<PeerInfo>) : Boolean
    {
        return tr.db.transactionWithResult {
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
}