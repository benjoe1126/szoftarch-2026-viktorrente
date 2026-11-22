package org.vik.torrente.scheduler

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentModeModel
import org.vik.torrente.repository.models.TorrentStatusModel
import org.vik.torrente.repository.orm.getTorrents
import org.vik.torrente.torrent.StopReason
import org.vik.torrente.torrent.TorrentCommand
import org.vik.torrente.torrent.TorrentContext
import org.vik.torrente.torrent.TorrentHandler
import org.vik.torrente.torrent.TorrentMode
import org.vik.torrente.torrent.TorrentStatus
import org.vik.torrente.torrent.engine.PeerListener
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean


class TorrentScheduler(val repository: TorrentRepository) {
    private data class HandlerData(
        val handler: TorrentHandler,
        val context: TorrentContext,
        val paused:Boolean
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
                sleep(500);

                //check the torrent states in the repository
                val torrents=repository.getTorrents();
                for(torrent in torrents)
                {
                    /*when(torrent.status)
                    {
                        TorrentStatus.STARTING->{
                            //check if the torrent is already downloading

                        }
                    }*/
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
        val currentHandler=getHandler(torrent.filePath);
        if(currentHandler!=null)
            return false;

        //construct the handler
        val context= TorrentContext(
            torrent.filePath,
            torrent.downloadDir,
            convertModeFromModel(torrent.mode)!!,
            this::onTorrentComplete,
            this::onTorrentStop,
            peerListener
        )

        return false;
    }

    private fun signalStopTorrent(torrent:TorrentInfo) : Boolean
    {
        //check if the torrent is active
        val handler=getHandler(torrent.filePath);
        if(handler==null)
            return true;

        //signal stop
        return handler.context.send(TorrentCommand.Stop)
    }

    private fun onTorrentComplete(status:TorrentStatus) : Unit
    {

    }

    private fun onTorrentStop(stopReason: StopReason)
    {

    }

    private fun getHandler(downloadPath : String): HandlerData?
    {
        var handlerData:HandlerData? = null;
        synchronized(handlers) {
            handlers.forEach {
                handler -> if(handler.context.torrentFilePath == downloadPath) handlerData = handler;
            }
        }
        return handlerData;
    }

    private fun removeHandler(downloadPath : String)
    {
        synchronized(handlers) {
            handlers.removeIf{
                it.context.torrentFilePath == downloadPath;
            }
        }
    }

}

private fun compareModeWithModel(mode:TorrentMode,modelMode: TorrentModeModel) : Boolean{
    if(mode== TorrentMode.DOWNLOAD&&modelMode== TorrentModeModel.DOWNLOADING)
        return true;
    if(mode== TorrentMode.DOWNLOAD_AND_SEED&&modelMode== TorrentModeModel.DOWNLOADING_AND_SEEDING)
        return true;
    if(mode== TorrentMode.SEED&&modelMode== TorrentModeModel.SEEDING)
        return true;
    return false;
}

private fun convertModeFromModel(modelMode: TorrentModeModel) : TorrentMode? {
    if(modelMode == TorrentModeModel.DOWNLOADING)
        return TorrentMode.DOWNLOAD;
    if(modelMode == TorrentModeModel.SEEDING)
        return TorrentMode.SEED;
    if(modelMode==TorrentModeModel.DOWNLOADING_AND_SEEDING)
        return TorrentMode.DOWNLOAD_AND_SEED;
    return null;
}