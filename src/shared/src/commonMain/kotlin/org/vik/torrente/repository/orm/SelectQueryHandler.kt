package org.vik.torrente.repository.orm

import androidx.collection.emptyLongSet
import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*
import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

class SelectQueryHandler (tr_ : TorrentRepository) :QueryHandlerBase(tr_){

    fun getTorrents() : List<TorrentInfo>{
        return tr.db.selectQueries.queryTorrents().executeAsList().map{
            torrent ->
            TorrentInfo(
                TorrentId(torrent.id),
                torrent.tracker,
                torrent.name,
                torrent.length,
            )
        };
    }

    fun getTorrentById(torrent : TorrentId) : TorrentInfo? {
        val result=tr.db.selectQueries.queryTorrentById(torrent.id).executeAsOneOrNull();
        return if(result!=null) TorrentInfo(TorrentId(result.id), result.tracker, result.name, result.length) else null;
    }

    fun getTorrentByName(torrentName : String):TorrentInfo?{
        val result=tr.db.selectQueries.queryTorrentByName(torrentName).executeAsOneOrNull();
        return if(result!=null) TorrentInfo(TorrentId(result.id), result.tracker, result.name, result.length) else null;
    }


    fun getPieceById(piece : PieceId) : PieceInfo? {
        val result=tr.db.selectQueries.queryPieceById(piece.number, piece.torrent_id.id).executeAsOneOrNull();

        return if(result!=null)
                    PieceInfo(piece, result.hash, result.length, result.number, PieceStatus.entries[result.status.toInt()], result.download_path, piece.torrent_id)
            else null;
    }

    fun getPieceArray(pieces:List<PieceId>):List<PieceInfo?> {
        var result=mutableListOf<PieceInfo?>();

        tr.db.transaction {
            for (piece:PieceId in pieces) {
                result.addLast(getPieceById(piece));
            }
        }

        return result;
    }

    fun getPiecesByTorrent(torrent:TorrentId):List<PieceInfo>{
        return tr.db.selectQueries.queryPiecesByTorrent(torrent.id).executeAsList().map{
            piece ->
            PieceInfo(PieceId(torrent, piece.number), piece.hash, piece.length, piece.number, PieceStatus.entries[piece.status.toInt()], piece.download_path, torrent)
        }
    }

    fun getPiecesByPeer(peer:PeerId):List<PieceInfo>{
        return tr.db.selectQueries.queryPiecesByPeer(peer.torrent_id.id, peer.address).executeAsList().map{
            piece ->
            PieceInfo(PieceId(TorrentId(piece.torrent_id), piece.number), piece.hash, piece.length, piece.number, PieceStatus.entries[piece.status.toInt()], piece.download_path, TorrentId(piece.torrent_id))
        }
    }

    fun getFileById(file : FileId) : FileInfo? {
        val file2=tr.db.selectQueries.queryFileById(file.number, file.torrent_id.id).executeAsOneOrNull();

        return if(file2!=null)
            FileInfo(file, file2.name, file2.length,file2.number, file.torrent_id);
        else
            null;
    }

    fun getFileArray(files:List<FileId>):List<FileInfo?> {
        var result=mutableListOf<FileInfo?>();
        tr.db.transaction {
            for (file:FileId in files)
            {
                result.addLast(getFileById(file));
            }
        }

        return result;
    }

    fun getFilesByTorrent(torrent:TorrentId):List<FileInfo>{
        return tr.db.selectQueries.queryFilesByTorrent(torrent.id).executeAsList().map{
            file ->
            FileInfo(FileId(torrent,file.number),file.name,file.length, file.number, torrent);
        }
    }

    fun getPeerById(peer : PeerId) : PeerInfo? {
        val result=tr.db.selectQueries.queryPeerById(peer.address, peer.torrent_id.id).executeAsOneOrNull();
        return if(result!=null)
            PeerInfo(PeerId(TorrentId(result.torrent_id), result.address), result.address, TorrentId(result.torrent_id));
            else null;
    }

    fun getPeerArray(peers:List<PeerId>):List<PeerInfo?> {
        var result=mutableListOf<PeerInfo?>();

        tr.db.transaction {
            peers.forEach{peer -> result.addLast(getPeerById(peer)) }
        }

        return result;
    }

    fun getPeersByPiece(piece:PieceId) : List<PeerInfo> {
        return tr.db.selectQueries.queryPeersByPiece(piece.number, piece.torrent_id.id).executeAsList().map{
                peer ->
            PeerInfo(PeerId(TorrentId(peer.torrent_id), peer.address),peer.address, TorrentId(peer.torrent_id));
        };
    }

    fun getPeersByTorrent(torrent:TorrentId) : List<PeerInfo> {
        return tr.db.selectQueries.queryPeersByTorrent(torrent.id).executeAsList().map{
                peer ->
            PeerInfo(PeerId(TorrentId(peer.torrent_id), peer.address),peer.address, TorrentId(peer.torrent_id));
        };
    }
}