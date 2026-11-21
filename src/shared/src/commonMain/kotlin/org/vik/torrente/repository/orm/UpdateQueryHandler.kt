package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.FileInfo
import org.vik.torrente.repository.models.PeerId
import org.vik.torrente.repository.models.PeerInfo
import org.vik.torrente.repository.models.PieceId
import org.vik.torrente.repository.models.PieceInfo
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentStatus

fun TorrentRepository.updateTorrent(torrent : TorrentInfo) : Boolean {
    return db.transactionWithResult {
        db.updateQueries.updateTorrentById(torrent.name, torrent.tracker, torrent.length, torrent.status.ordinal.toLong(), torrent.hash);
        db.updateQueries.affectedRows().executeAsOne().toInt()==1;
    };
}

fun TorrentRepository.updatePiece(piece : PieceInfo) : Boolean {
    return db.transactionWithResult {
        db.updateQueries.updatePieceById(piece.hash, piece.length, piece.status.ordinal.toLong(), piece.downloadPath, piece.torrent.hash, piece.number);
        db.updateQueries.affectedRows().executeAsOne().toInt()==1;
    };
}

fun TorrentRepository.updateFile(file: FileInfo):Boolean  {
    return db.transactionWithResult {
        db.updateQueries.updateFileById(file.name, file.length, file.torrent.hash, file.number);
        db.updateQueries.affectedRows().executeAsOne().toInt()==1;
    };
}

fun TorrentRepository.updatePeer(peer: PeerInfo) : Boolean {
    return db.transactionWithResult {
        db.updateQueries.updatePeerById(peer.address,peer.torrent.hash, peer.idHash);
        db.updateQueries.affectedRows().executeAsOne().toInt()==1;
    }
}

fun TorrentRepository.updatePeerOffering(peer: PeerId, piecesOffered : List<PieceId>) : Boolean {
    return db.transactionWithResult {
        if(!deletePeerPieceOffers(peer))
            rollback(false);

        for(piece in piecesOffered)
            if(!insertPeerOffering(peer, piece))
                rollback(false);

        true;
    }
}