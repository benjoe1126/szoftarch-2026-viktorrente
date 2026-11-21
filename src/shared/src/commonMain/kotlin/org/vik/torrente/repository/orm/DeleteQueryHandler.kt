package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*

fun TorrentRepository.deleteTorrent(torrent: TorrentId):Boolean{
    return runCatching{
        if(getTorrentById(torrent)!=null)
        {
            db.transaction {
                val pieces=getPiecesByTorrent(torrent);
                val files=getFilesByTorrent(torrent);
                val peers = getPeersByTorrent(torrent);

                for(piece in pieces)
                    if(!deletePiece(piece.getId()))
                        throw Exception();

                for(file in files)
                    if(!deleteFile(file.getId()))
                        throw Exception();

                for(peer in peers)
                    if(!deletePeer(peer.getId()))
                        throw Exception();

                db.deleteQueries.deleteTorrentByTorrentId(torrent.hash);
            }
        }
    }.isSuccess;
}

fun TorrentRepository.deleteFile(file:FileId):Boolean{
    return runCatching {
        db.transaction {
            if(getFileById(file)!=null)
                db.deleteQueries.deleteFileByFileId(file.torrent.hash, file.number);
        }
    }.isSuccess;
}

fun TorrentRepository.deletePiece(piece:PieceId):Boolean{
    return runCatching{
        db.transaction {
            if(getPieceById(piece)!=null)
            {
                if(!deletePeerPieceOffers(piece))
                    throw Exception();
                db.deleteQueries.deletePieceByPieceId(piece.torrent.hash, piece.number);
            }
        }
    }.isSuccess;
}

fun TorrentRepository.deletePeer(peer:PeerId):Boolean{
    return runCatching{
        db.transaction {
            if(getPeerById(peer)!=null)
            {
                if(!deletePeerPieceOffers(peer))
                    throw Exception();
                db.deleteQueries.deletePeerByPeerId(peer.torrent.hash, peer.address);
            }
        }
    }.isSuccess;
}

//deletes the entries containing an offering of the peer regarding a piece
fun TorrentRepository.deletePeerPieceOffers(peer:PeerId):Boolean{
    return runCatching{
        db.deleteQueries.deletePhpByPeerId(peer.torrent.hash, peer.address);
    }.isSuccess;
}

fun TorrentRepository.deletePeerPieceOffers(piece:PieceId):Boolean{
    return runCatching{
        db.deleteQueries.deletePhpByPieceId(piece.torrent.hash, piece.number);
    }.isSuccess;
}