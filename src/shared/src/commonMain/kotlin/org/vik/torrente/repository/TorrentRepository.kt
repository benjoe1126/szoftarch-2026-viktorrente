package org.vik.torrente.repository

import org.vik.torrente.repository.models.*

class TorrentRepository(val databaseIP : String, val databasePort: UShort) {

    init{
        //connect to database or sum
    }

    fun queryTorrents() : List<TorrentInfo>{
        return listOf<TorrentInfo>();
    }

    fun queryTorrentInfo(torrent : TorrentId) : TorrentInfo? {
        return null;
    }

    fun queryPieceInfo(piece : PieceId) : PieceInfo? {
        return null;
    }

    fun queryPieceInfoArray(pieces:List<PieceId>):List<PieceInfo?> {
        return listOf<PieceInfo?>();
    }

    fun queryFileInfo(file : FileId) : FileInfo? {
        return null;
    }

    fun queryFileInfoArray(files:List<FileId>):List<FileInfo?> {
        return listOf<FileInfo?>();
    }

    fun queryPeersByPiece(piece:PieceId) : List<PeerInfo> {
        return listOf<PeerInfo>();
    }

    fun queryPeerInfo(peer : PeerId) : PeerInfo? {
        return null;
    }

    fun queryPeerInfoArray(peers:List<PeerId>):List<PeerInfo?> {
        return listOf<PeerInfo?>();
    }
}