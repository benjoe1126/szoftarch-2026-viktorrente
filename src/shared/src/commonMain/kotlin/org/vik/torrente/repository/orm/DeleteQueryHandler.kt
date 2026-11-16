package org.vik.torrente.repository.orm

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*

class DeleteQueryHandler(tr_: TorrentRepository): QueryHandlerBase(tr_) {

    fun deleteTorrent(torrent: TorrentId):Boolean{
        var result = true;


        return runCatching{
            if(tr.selectHandler.getTorrentById(torrent)!=null)
            {
                tr.db.transaction {
                    val pieces=tr.selectHandler.getPiecesByTorrent(torrent);
                    val files=tr.selectHandler.getFilesByTorrent(torrent);
                    val peers = tr.selectHandler.getPeersByTorrent(torrent);

                    for(piece in pieces)
                        if(!deletePiece(piece.id))
                            throw Exception();

                    for(file in files)
                        if(!deleteFile(file.id))
                            throw Exception();

                    for(peer in peers)
                        if(!deletePeer(peer.id))
                            throw Exception();

                    tr.db.deleteQueries.deleteTorrentByTorrentId(torrent.id);
                }
            }
        }.isSuccess;
    }

    fun deleteFile(file:FileId):Boolean{
        return runCatching {
            tr.db.transaction {
                if(tr.selectHandler.getFileById(file)!=null)
                    tr.db.deleteQueries.deleteFileByFileId(file.torrent_id.id, file.number);
            }
        }.isSuccess;
    }

    fun deletePiece(piece:PieceId):Boolean{
        return runCatching{
             tr.db.transaction {
                 if(tr.selectHandler.getPieceById(piece)!=null)
                 {
                    if(!deletePeerPieceOffers(piece))
                        throw Exception();
                    tr.db.deleteQueries.deletePieceByPieceId(piece.torrent_id.id, piece.number);
                 }
             }
        }.isSuccess;
    }

    fun deletePeer(peer:PeerId):Boolean{
        return runCatching{
            tr.db.transaction {
                if(tr.selectHandler.getPeerById(peer)!=null)
                {
                    if(!deletePeerPieceOffers(peer))
                        throw Exception();
                    tr.db.deleteQueries.deletePeerByPeerId(peer.torrent_id.id, peer.address);
                }
            }
        }.isSuccess;
    }

    //deletes the entries containing an offering of the peer regarding a piece
    fun deletePeerPieceOffers(peer:PeerId):Boolean{
        return runCatching{
            tr.db.deleteQueries.deletePhpByPeerId(peer.torrent_id.id, peer.address);
        }.isSuccess;
    }

    fun deletePeerPieceOffers(piece:PieceId):Boolean{
        return runCatching{
            tr.db.deleteQueries.deletePhpByPieceId(piece.torrent_id.id, piece.number);
        }.isSuccess;
    }
}