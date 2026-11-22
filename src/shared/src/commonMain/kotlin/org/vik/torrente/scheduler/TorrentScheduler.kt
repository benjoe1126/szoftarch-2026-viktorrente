package org.vik.torrente.scheduler

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentStatus
import org.vik.torrente.repository.orm.getTorrents
import org.vik.torrente.torrent.TorrentCommand
import org.vik.torrente.torrent.TorrentContext
import org.vik.torrente.torrent.TorrentHandler
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class TorrentScheduler(val repository: TorrentRepository) {
    private data class HandlerData(
        val handler: TorrentHandler,
        val context: TorrentContext,
        val paused:Boolean
    )

    //previously active handlers, whose demise was not met via an error
    private val stoppedHandlers =Collections.synchronizedList<HandlerData>(mutableListOf<HandlerData>());
    //handlers that are active
    private val activeHandlers = Collections.synchronizedList<HandlerData>(mutableListOf<HandlerData>());

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
        val currentHandler=getActiveHandler(torrent.filePath);
        //if(currentHandler!=null&&currentHandler.context.mode)
            //return true;

        //check if the handler has history
        val prevHandler = getStoppedHandler(torrent.filePath);
        if(prevHandler!=null)
            removeStoppedHandler(torrent.filePath);

        //construct the new handler
        return false;
    }

    private fun signalStopTorrent(torrent:TorrentInfo) : Boolean
    {
        //check if the torrent is active
        val handler=getActiveHandler(torrent.filePath);
        if(handler==null)
            return false;

        //signal stop
        return handler.context.send(TorrentCommand.Stop)
    }

    private fun getActiveHandler(downloadPath : String): HandlerData?
    {
        var handlerData:HandlerData? = null;
        synchronized(activeHandlers) {
            activeHandlers.forEach {
                handler -> if(handler.context.torrentFilePath == downloadPath) handlerData = handler;
            }
        }
        return handlerData;
    }

    private fun getStoppedHandler(downloadPath : String): HandlerData?
    {
        var handlerData:HandlerData? = null;
        synchronized(stoppedHandlers) {
            stoppedHandlers.forEach {
                    handler -> if(handler.context.torrentFilePath == downloadPath) handlerData = handler;
            }
        }
        return handlerData;
    }

    private fun removeActiveHandler(downloadPath : String)
    {
        synchronized(activeHandlers) {
            activeHandlers.removeIf{
                it.context.torrentFilePath == downloadPath;
            }
        }
    }

    private fun removeStoppedHandler(downloadPath : String)
    {
        synchronized(stoppedHandlers) {
            stoppedHandlers.removeIf{
                it.context.torrentFilePath == downloadPath;
            }
        }
    }
}