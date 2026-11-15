package org.vik.torrente.repository

import app.cash.sqldelight.db.SqlDriver
import org.vik.torrente.repository.database.DatabaseDriverFactory
import org.vik.torrente.repository.models.*

import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

class TorrentRepository(val driverFactory : DatabaseDriverFactory) {
    val driver : SqlDriver;
    val db : TorrentDB;

    init{
        driver = driverFactory.create();
        db = TorrentDB(driver);
    }

    fun queryTorrents() : List<TorrentInfo>{
        var infos=mutableListOf<TorrentInfo>();

        db.transaction {
            val torrents=db.selectQueries.queryTorrents().executeAsList();

            for (torrent in torrents)
            {
                val torrentId=TorrentId(torrent.id);

                val pieces=db.selectQueries.queryPiecesByTorrent(torrent.id).executeAsList().map{
                    piece ->
                    PieceId(torrentId, piece.number)
                }

                val files=db.selectQueries.queryFilesByTorrent(torrent.id).executeAsList().map{
                    file ->
                    FileId(torrentId, file.number);
                }

                infos.addLast(
                    TorrentInfo(
                        torrentId,
                        torrent.tracker,
                        torrent.name,
                        torrent.length,
                        files,
                        pieces
                    )
                );
            }
        }

        return infos;
    }

    fun queryTorrentInfo(torrent : TorrentId) : TorrentInfo? {
        var result:TorrentInfo?=null;

        db.transaction {
            val torrent=db.selectQueries.queryTorrentInfo(torrent.id).executeAsOneOrNull();

            if(torrent!=null)
            {
                val torrentId=TorrentId(torrent.id);

                val pieces=db.selectQueries.queryPiecesByTorrent(torrent.id).executeAsList().map{
                        piece ->
                    PieceId(torrentId, piece.number)
                }

                val files=db.selectQueries.queryFilesByTorrent(torrent.id).executeAsList().map{
                        file ->
                    FileId(torrentId, file.number);
                }

                result=TorrentInfo(torrentId, torrent.tracker,torrent.name,torrent.length,files,pieces);
            }
        }

        return result;
    }

    fun queryPieceInfo(piece : PieceId) : PieceInfo? {
        var result:PieceInfo?=null;

        db.transaction {
            val sus=db.selectQueries.queryPieceInfo(piece.number, piece.torrent_id.id).executeAsOneOrNull();

            if(sus!=null)
            {
                val peers = db.selectQueries.queryPeersByPiece(piece.number, piece.torrent_id.id).executeAsList().map{
                    peer ->
                    PeerId(TorrentId(peer.torrent_id), peer.address)
                };

                result=PieceInfo(piece, sus.hash, sus.length, sus.number, PieceStatus.entries[sus.status.toInt()], sus.download_path, piece.torrent_id, peers);
            }
        }

        return result;
    }

    fun queryPieceInfoArray(pieces:List<PieceId>):List<PieceInfo?> {
        var result=mutableListOf<PieceInfo?>();

        db.transaction {
            for (piece:PieceId in pieces) {
                val sus=db.selectQueries.queryPieceInfo(piece.number, piece.torrent_id.id).executeAsOneOrNull();

                if(sus!=null)
                {
                    val peers = db.selectQueries.queryPeersByPiece(piece.number, piece.torrent_id.id).executeAsList().map{
                            peer ->
                        PeerId(TorrentId(peer.torrent_id), peer.address)
                    };

                    result.addLast(PieceInfo(piece, sus.hash, sus.length, sus.number, PieceStatus.entries[sus.status.toInt()], sus.download_path, piece.torrent_id, peers));
                }
                else
                    result.addLast(null);
            }
        }

        return result;
    }

    fun queryFileInfo(file : FileId) : FileInfo? {
        val file2=db.selectQueries.queryFileInfo(file.number, file.torrent_id.id).executeAsOneOrNull();

        return if(file2!=null)
            FileInfo(file, file2.name, file2.length,file2.number, file.torrent_id);
        else
            null;
    }

    fun queryFileInfoArray(files:List<FileId>):List<FileInfo?> {
        var result=mutableListOf<FileInfo?>();
        db.transaction {
            for (file:FileId in files)
            {
                val f=db.selectQueries.queryFileInfo(file.number, file.torrent_id.id).executeAsOneOrNull();
                if(f!=null)
                    result.addLast(FileInfo(file, f.name, f.length, f.number, file.torrent_id));
                else
                    result.addLast(null);
            }
        }

        return result;
    }

    fun queryPeersByPiece(piece:PieceId) : List<PeerInfo> {
        var result=mutableListOf<PeerInfo>();

        db.transaction {
            val peers=db.selectQueries.queryPeersByPiece(piece.number, piece.torrent_id.id).executeAsList().map{
                peer ->
                PeerId(TorrentId(peer.torrent_id), peer.address)
            };

            for(peer in peers)
            {
                val peerQuery=db.selectQueries.queryPeerInfo(peer.address, peer.torrent_id.id).executeAsOneOrNull();

                if(peerQuery!=null)
                {
                    val pieceQuery=db.selectQueries.queryPiecesByPeer(peer.torrent_id.id, peer.address).executeAsList().map{
                            piece ->
                        PieceId(TorrentId(piece.torrent_id), piece.number)
                    }

                    result.addLast(PeerInfo(peer,peer.address,peer.torrent_id,pieceQuery));
                }
            }
        }

        return result;
    }

    fun queryPeerInfo(peer : PeerId) : PeerInfo? {
        var result:PeerInfo?=null;

        db.transaction {
            val peerQuery=db.selectQueries.queryPeerInfo(peer.address, peer.torrent_id.id).executeAsOneOrNull();

            if(peerQuery!=null)
            {
                val pieceQuery=db.selectQueries.queryPiecesByPeer(peer.torrent_id.id, peer.address).executeAsList().map{
                    piece ->
                    PieceId(TorrentId(piece.torrent_id), piece.number)
                }

                result=PeerInfo(peer,peer.address,peer.torrent_id,pieceQuery);
            }
        }

        return result;
    }

    fun queryPeerInfoArray(peers:List<PeerId>):List<PeerInfo?> {
        var result=mutableListOf<PeerInfo?>();

        db.transaction {
            for(peer in peers)
            {
                val peerQuery=db.selectQueries.queryPeerInfo(peer.address, peer.torrent_id.id).executeAsOneOrNull();

                if(peerQuery!=null)
                {
                    val pieceQuery=db.selectQueries.queryPiecesByPeer(peer.torrent_id.id, peer.address).executeAsList().map{
                            piece ->
                        PieceId(TorrentId(piece.torrent_id), piece.number)
                    }

                    result.addLast(PeerInfo(peer,peer.address,peer.torrent_id,pieceQuery));
                }
                else
                    result.addLast(null);
            }
        }

        return result;
    }
}