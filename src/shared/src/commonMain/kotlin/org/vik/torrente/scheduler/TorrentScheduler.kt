package org.vik.torrente.scheduler

import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.TorrentStatus
import org.vik.torrente.repository.orm.getTorrents
import org.vik.torrente.torrent.TorrentContext
import org.vik.torrente.torrent.TorrentHandler
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class TorrentScheduler(val repository: TorrentRepository) {
    private data class HandlerData(val handler: TorrentHandler, val context: TorrentContext);

    private val activeHandlers = Collections.synchronizedList<HandlerData>(mutableListOf<HandlerData>());
    private var monitor:DBMonitor? = null;

    fun start()
    {
        if(monitor!=null)
            throw Exception("The scheduler is already started");

        monitor = DBMonitor(this, repository);
        monitor!!.start();
    }

    fun stop()
    {
        if(monitor==null)
            throw Exception("The scheduler is already stopped");
        monitor!!.signalStop();
        monitor!!.join();
        monitor=null;
    }

    private class DBMonitor(val scheduler:TorrentScheduler, val repository:TorrentRepository) : Thread(){
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

    private fun torrentRegistered(): Boolean
    {
        return false;
    }
}