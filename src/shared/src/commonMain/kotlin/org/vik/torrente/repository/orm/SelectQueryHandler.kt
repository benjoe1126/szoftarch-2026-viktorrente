package org.vik.torrente.repository.orm

import androidx.collection.emptyLongSet
import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.*
import org.vik.torrente.repository.orm.getFileById
import org.vik.torrente.repository.orm.getPeerById
import org.vik.torrente.repository.orm.getPieceById
import queries.TorrentDB
import queries.TorrentDB.Companion.invoke

fun TorrentRepository.getTorrents() : List<TorrentInfo>{
    return db.selectQueries.queryTorrents().executeAsList().map{
            torrent ->
        TorrentInfo(
            torrent.hash,
            torrent.tracker,
            torrent.name,
            torrent.length,
            TorrentStatus.entries[torrent.status.toInt()],
            torrent.file_path
        )
    };
}

fun TorrentRepository.getTorrentById(torrent : TorrentId) : TorrentInfo? {
    val result=db.selectQueries.queryTorrentById(torrent.hash).executeAsOneOrNull();
    return if(result!=null) TorrentInfo(result.hash, result.tracker, result.name, result.length, TorrentStatus.entries[result.status.toInt()], result.file_path) else null;
}

fun TorrentRepository.getTorrentByName(torrentName : String):TorrentInfo?{
    val result=db.selectQueries.queryTorrentByName(torrentName).executeAsOneOrNull();
    return if(result!=null) TorrentInfo(result.hash, result.tracker, result.name, result.length, TorrentStatus.entries[result.status.toInt()], result.file_path) else null;
}

fun TorrentRepository.getPieceById(piece : PieceId) : PieceInfo? {
    val result=db.selectQueries.queryPieceById(piece.number, piece.torrent.hash).executeAsOneOrNull();

    return if(result!=null)
        PieceInfo(TorrentId(result.torrent_hash), result.number, result.hash, result.length, PieceStatus.entries[result.status.toInt()], result.download_path)
    else null;
}

fun TorrentRepository.getPieceArray(pieces:List<PieceId>):List<PieceInfo?> {
    var result=mutableListOf<PieceInfo?>();

    db.transaction {
        for (piece:PieceId in pieces) {
            result.addLast(getPieceById(piece));
        }
    }

    return result;
}

fun TorrentRepository.getPiecesByTorrent(torrent:TorrentId):List<PieceInfo>{
    return db.selectQueries.queryPiecesByTorrent(torrent.hash).executeAsList().map{
            piece ->
        PieceInfo(TorrentId(piece.torrent_hash), piece.number,piece.hash, piece.length, PieceStatus.entries[piece.status.toInt()], piece.download_path)
    }
}

fun TorrentRepository.getPiecesByPeer(peer:PeerId):List<PieceInfo>{
    return db.selectQueries.queryPiecesByPeer(peer.torrent.hash, peer.idHash).executeAsList().map{
            piece ->
        PieceInfo(TorrentId(piece.torrent_hash), piece.number, piece.hash, piece.length,  PieceStatus.entries[piece.status.toInt()], piece.download_path)
    }
}

fun TorrentRepository.getFileById(file : FileId) : FileInfo? {
    val file2=db.selectQueries.queryFileById(file.number, file.torrent.hash).executeAsOneOrNull();

    return if(file2!=null)
        FileInfo(TorrentId(file2.torrent_hash), file2.number,file2.name, file2.length);
    else
        null;
}

fun TorrentRepository.getFileArray(files:List<FileId>):List<FileInfo?> {
    var result=mutableListOf<FileInfo?>();
    db.transaction {
        for (file:FileId in files)
        {
            result.addLast(getFileById(file));
        }
    }

    return result;
}

fun TorrentRepository.getFilesByTorrent(torrent:TorrentId):List<FileInfo>{
    return db.selectQueries.queryFilesByTorrent(torrent.hash).executeAsList().map{
            file ->
        FileInfo(TorrentId(file.torrent_hash),file.number,file.name,file.length);
    }
}

fun TorrentRepository.getPeerById(peer : PeerId) : PeerInfo? {
    val result=db.selectQueries.queryPeerById(peer.idHash, peer.torrent.hash).executeAsOneOrNull();
    return if(result!=null)
        PeerInfo(TorrentId(result.torrent_hash),result.id_hash, result.address, PeerStatus.entries[result.status.toInt()]);
    else null;
}

fun TorrentRepository.getPeerArray(peers:List<PeerId>):List<PeerInfo?> {
    var result=mutableListOf<PeerInfo?>();

    db.transaction {
        peers.forEach{peer -> result.addLast(getPeerById(peer)) }
    }

    return result;
}

fun TorrentRepository.getPeersByPiece(piece:PieceId) : List<PeerInfo> {
    return db.selectQueries.queryPeersByPiece(piece.torrent.hash, piece.number).executeAsList().map{
            peer ->
        PeerInfo(TorrentId(peer.torrent_hash),peer.id_hash,peer.address,PeerStatus.entries[peer.status.toInt()]);
    };
}

fun TorrentRepository.getPeersByTorrent(torrent:TorrentId) : List<PeerInfo> {
    return db.selectQueries.queryPeersByTorrent(torrent.hash).executeAsList().map{
            peer ->
        PeerInfo(TorrentId(peer.torrent_hash),peer.id_hash,peer.address,PeerStatus.entries[peer.status.toInt()]);
    };
}