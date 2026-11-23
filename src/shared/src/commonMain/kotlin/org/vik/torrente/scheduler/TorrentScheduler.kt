package org.vik.torrente.scheduler

import androidx.collection.emptyLongSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.PeerInfo
import org.vik.torrente.repository.models.PeerStatus
import org.vik.torrente.repository.models.PieceInfo
import org.vik.torrente.repository.models.PieceStatus
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentModeModel
import org.vik.torrente.repository.models.TorrentStatusModel
import org.vik.torrente.repository.orm.deletePeer
import org.vik.torrente.repository.orm.deletePiece
import org.vik.torrente.repository.orm.getPeersByTorrent
import org.vik.torrente.repository.orm.getPiecesByTorrent
import org.vik.torrente.repository.orm.getTorrents
import org.vik.torrente.repository.orm.getTorrentsByFilenameAndDownloaddir
import org.vik.torrente.repository.orm.insertPeer
import org.vik.torrente.repository.orm.updatePiece
import org.vik.torrente.repository.orm.updateTorrent
import org.vik.torrente.torrent.StopReason
import org.vik.torrente.torrent.TorrentCommand
import org.vik.torrente.torrent.TorrentContext
import org.vik.torrente.torrent.TorrentHandler
import org.vik.torrente.torrent.TorrentMode
import org.vik.torrente.torrent.TorrentPhase
import org.vik.torrente.torrent.TorrentStatus
import org.vik.torrente.torrent.engine.PeerListener
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Handler


class TorrentScheduler(val repository: TorrentRepository) {
    private data class HandlerData(
        var handler: TorrentHandler,
        var context: TorrentContext,
        var paused:Boolean
    )


    private val peerListener= PeerListener(0);

    //handlers that are active or paused
    private val handlers = Collections.synchronizedList<HandlerData>(mutableListOf<HandlerData>());

    //listener thread
    private var dbListener:DBListener? = null;

    fun start()
    {
        if(dbListener!=null)
            throw Exception("The scheduler is already started");

        dbListener = DBListener(this, repository);
        dbListener!!.start();
    }

    fun stop()
    {
        if(dbListener==null)
            throw Exception("The scheduler is already stopped");
        dbListener!!.signalStop();
        dbListener!!.join();
        dbListener=null;
    }

    private class DBListener(val scheduler:TorrentScheduler, val repository:TorrentRepository) : Thread(){
        val shouldStop = AtomicBoolean(false);

        override fun run() {
            while(!shouldStop.get())
            {
                //sleepy time
                sleep(100);

                //check the torrent states in the repository
                val torrents=repository.getTorrents();
                for(torrent in torrents)
                {
                    //check if action should be taken
                    val handler=scheduler.getHandler(torrent.filePath,torrent.downloadDir)
                    if(handler != null)
                    {
                        when(torrent.mode)
                        {
                            TorrentModeModel.DOWNLOADING -> scheduler.signalResumeTorrent(torrent);
                            TorrentModeModel.SEEDING-> scheduler.signalResumeTorrent(torrent);
                            TorrentModeModel.DOWNLOADING_AND_SEEDING -> scheduler.signalResumeTorrent(torrent);
                            TorrentModeModel.PAUSED -> scheduler.signalPauseTorrent(torrent);
                            TorrentModeModel.INACTIVE -> scheduler.signalStopTorrent(torrent);
                        }
                    }
                    else
                    {
                        when(torrent.mode)
                        {
                            TorrentModeModel.DOWNLOADING -> scheduler.startTorrent(torrent);
                            TorrentModeModel.SEEDING-> scheduler.startTorrent(torrent);
                            TorrentModeModel.DOWNLOADING_AND_SEEDING -> scheduler.startTorrent(torrent);
                            else->{}
                        }
                    }
                }

                //query the handler states
                val handlersCopy=mutableListOf<HandlerData>();
                synchronized(scheduler.handlers)
                {
                    scheduler.handlers.forEach{handler->handlersCopy.add(handler)}
                }
                handlersCopy.forEach {
                    handler->
                    val snapshot=handler.handler.torrentSnapshot()
                    val torrents=scheduler.repository.getTorrentsByFilenameAndDownloaddir(handler.context.torrentFilePath,handler.context.downloadDir)
                    if(torrents.isEmpty()||snapshot==null)
                        return;
                    scheduler.repository.getPeersByTorrent(torrents[0].getId()).forEach { p->scheduler.repository.deletePeer(p.getId()) }
                    snapshot.peers.forEach { p-> scheduler.repository.insertPeer(PeerInfo(torrents[0].getId(),p.host+":"+p.portNumber.toString(),p.host+":"+p.portNumber.toString(),PeerStatus.SEEDING))}
                    scheduler.repository.getPiecesByTorrent(torrents[0].getId()).forEach{
                        p->
                        if(snapshot.piecesDownloaded[p.number.toInt()].toInt()==1)
                            scheduler.repository.updatePiece(PieceInfo(p.torrent,p.number,p.hash,p.length,PieceStatus.AVAILABLE,p.downloadPath));
                        else if(snapshot.piecesDownloading[p.number.toInt()].toInt()==1)
                            scheduler.repository.updatePiece(PieceInfo(p.torrent,p.number,p.hash,p.length,PieceStatus.DOWNLOADING,p.downloadPath));
                        else
                            scheduler.repository.updatePiece(PieceInfo(p.torrent,p.number,p.hash,p.length,PieceStatus.NOT_AVAILABLE,p.downloadPath));
                    }
                }
            }
        }

        fun signalStop()
        {
            shouldStop.set(true);
        }
    }

    private fun startTorrent(torrent : TorrentInfo) : Boolean {
        //check if the handler is already active
        val currentHandler=getHandler(torrent.filePath,torrent.downloadDir);
        if(currentHandler!=null)
            return false;

        //construct and start the handler
        val context= TorrentContext(
            torrent.filePath,
            torrent.downloadDir,
            torrent.mode.toMode()!!,
            this::onTorrentComplete,
            this::onTorrentStop,
            peerListener
        )
        val handler= TorrentHandler(context);

        handlers.addLast(HandlerData(handler,context, false));

        handler.start(CoroutineScope(Dispatchers.IO));

        return true;
    }

    private fun signalStopTorrent(torrent:TorrentInfo) : Boolean
    {
        //check if the torrent is active
        val handlerData=getHandler(torrent.filePath,torrent.downloadDir);
        if(handlerData==null)
            return true;

        //signal stop
        return handlerData.context.send(TorrentCommand.Stop)
    }

    private fun signalPauseTorrent(torrent:TorrentInfo) : Boolean
    {
        //check if there is a matching torrent and if it is not paused
        val handlerData=getHandler(torrent.filePath,torrent.downloadDir);
        if(handlerData==null||handlerData.paused)
            return true;

        if(handlerData.context.send(TorrentCommand.Pause))
        {
            handlerData.paused=true;

            val torrents=repository.getTorrentsByFilenameAndDownloaddir(handlerData.context.torrentFilePath,handlerData.context.downloadDir);
            if(torrents.isNotEmpty())
            {
                val t = torrents[0];
                repository.updateTorrent(TorrentInfo(t.hash,t.tracker,t.name,t.length, t.mode.toStatus(),
                    t.mode,t.filePath,t.downloadDir));
            }

            return true;
        }
        return false;
    }

    private fun signalResumeTorrent(torrent:TorrentInfo) : Boolean
    {
        //check if there is a matching torrent and if it is paused
        val handlerData=getHandler(torrent.filePath,torrent.downloadDir);
        if(handlerData==null||!handlerData.paused)
            return true;

        if(handlerData.context.send(TorrentCommand.Resume))
        {
            handlerData.paused=false;

            val torrents=repository.getTorrentsByFilenameAndDownloaddir(handlerData.context.torrentFilePath,handlerData.context.downloadDir);
            if(torrents.isNotEmpty())
            {
                val t = torrents[0];
                repository.updateTorrent(TorrentInfo(t.hash,t.tracker,t.name,t.length, t.mode.toStatus(),
                    t.mode,t.filePath,t.downloadDir));
            }

            return true;
        }
        return false;
    }

    private fun onTorrentComplete(context:TorrentContext, status:TorrentStatus) : Unit
    {
        val handler=getHandlerByContext(context);
        if(handler==null)
            return;

        when(context.mode)
        {
            //check if the torrent is completed
            TorrentMode.DOWNLOAD->{
                val torrents=repository.getTorrentsByFilenameAndDownloaddir(context.torrentFilePath,context.downloadDir);
                if(torrents.isNotEmpty())
                {
                    val t = torrents[0];
                    repository.updateTorrent(TorrentInfo(t.hash,t.tracker,t.name,t.length,status.phase.toModel(),
                        TorrentModeModel.INACTIVE,t.filePath,t.downloadDir));
                }
            }
            //if DOWNLOAD_AND_SEED, update db entry to SEED
            TorrentMode.DOWNLOAD_AND_SEED->{
                val torrents=repository.getTorrentsByFilenameAndDownloaddir(context.torrentFilePath,context.downloadDir);
                if(torrents.isNotEmpty())
                {
                    val t = torrents[0];
                    repository.updateTorrent(TorrentInfo(t.hash,t.tracker,t.name,t.length,status.phase.toModel(),
                        TorrentModeModel.SEEDING,t.filePath,t.downloadDir));
                }
            }
            else->{}
        }
    }

    private fun onTorrentStop(context:TorrentContext, stopReason: StopReason)
    {
        //check if the handler exists
        val handler=getHandlerByContext(context);
        if(handler==null)
            return;

        //remove its entry
        handlers.remove(handler);

        //if failed, reset its data from the database
        if(stopReason is StopReason.Failed)
        {
            val torrents=repository.getTorrentsByFilenameAndDownloaddir(context.torrentFilePath, context.downloadDir);
            if(torrents.isNotEmpty())
            {
                val t=torrents[0];
                repository.updateTorrent(TorrentInfo(t.hash,t.tracker,t.name,t.length, TorrentStatusModel.ERROR,
                    TorrentModeModel.INACTIVE,t.filePath,t.downloadDir));
                repository.getPeersByTorrent(t.getId()).forEach{p->repository.deletePeer(p.getId())}
                repository.getPiecesByTorrent(t.getId()).forEach{p->repository.updatePiece(PieceInfo(p.torrent,p.number,p.hash,p.length,PieceStatus.NOT_AVAILABLE,p.downloadPath))}
            }
        }
    }

    private fun getHandler(filePath:String, downloadPath : String): HandlerData?
    {
        var handlerData:HandlerData? = null;
        synchronized(handlers) {
            handlers.forEach {
                handler -> if(handler.context.torrentFilePath==filePath&&handler.context.downloadDir == downloadPath) handlerData = handler;
            }
        }
        return handlerData;
    }

    private fun getHandlerByContext(context:TorrentContext): HandlerData?
    {
        var handlerData:HandlerData? = null;
        synchronized(handlers) {
            handlers.forEach {
                    handler -> if(handler.context == context) handlerData = handler;
            }
        }
        return handlerData;
    }

    private fun TorrentMode.compareWithModel(modelMode: TorrentModeModel) : Boolean{
        if(this== TorrentMode.DOWNLOAD&&modelMode== TorrentModeModel.DOWNLOADING)
            return true;
        if(this== TorrentMode.DOWNLOAD_AND_SEED&&modelMode== TorrentModeModel.DOWNLOADING_AND_SEEDING)
            return true;
        if(this== TorrentMode.SEED&&modelMode== TorrentModeModel.SEEDING)
            return true;
        return false;
    }

    private fun TorrentModeModel.toMode() : TorrentMode? {
        if(this == TorrentModeModel.DOWNLOADING)
            return TorrentMode.DOWNLOAD;
        if(this == TorrentModeModel.SEEDING)
            return TorrentMode.SEED;
        if(this==TorrentModeModel.DOWNLOADING_AND_SEEDING)
            return TorrentMode.DOWNLOAD_AND_SEED;
        return null;
    }

    private fun TorrentPhase.toModel(): TorrentStatusModel {
        when(this)
        {
            TorrentPhase.DOWNLOADING->return TorrentStatusModel.DOWNLOADING;
            TorrentPhase.CHECKING -> return TorrentStatusModel.CHECKING;
            TorrentPhase.PAUSED -> return TorrentStatusModel.PAUSED;
            TorrentPhase.SEEDING -> return TorrentStatusModel.SEEDING;
            TorrentPhase.STARTING -> return TorrentStatusModel.STARTING;
            TorrentPhase.STOPPED -> return TorrentStatusModel.STOPPED;
        }
    }

    private fun TorrentModeModel.toStatus():TorrentStatusModel{
        when(this)
        {
            TorrentModeModel.DOWNLOADING->return TorrentStatusModel.DOWNLOADING;
            TorrentModeModel.PAUSED->return TorrentStatusModel.PAUSED;
            TorrentModeModel.SEEDING->return TorrentStatusModel.SEEDING;
            TorrentModeModel.DOWNLOADING_AND_SEEDING -> return TorrentStatusModel.DOWNLOADING;
            else->return TorrentStatusModel.STOPPED;
        }
    }
}