# Rendszerterv

## Platform, választott szoftver készlet

A választott programozási nyelv a Kotlin lett, ami egy JVM-en (is) futtatható, magas szintű programozási nyelv.  
A végleges program egy Kotlin multiplatform alkalmazás (KMP), amely jelenleg kizárólag asztali számítógépen működik.  
A keretrendszernek köszönhetően használható vált a Jetpack Compose eszköztár (widgetek, Composable függvények), melyet a felhasználói felület elkészítéséhez használtunk.  
Az alkalmazás ezen felül végez különböző adatbázis műveleteket, mely alapértelmezetten egy SQLite in-memory adatbázis.  
Az adatbázis műveletekhez SQLDelight-ot használtunk, amely már meglévő SQL statementekből generál típusbiztos kotlin interfacet.  
Az alkalmazás, lévén, hogy torrent kliens végez különböző hálózati hívásokat (udp socket nyitása, udp datagram elkülése, stb.). A hálózati műveletek kezelésére a java.net modult használtuk, mivel relatív low-level network API-ra volt szükségünk, ez pedig pontosan ezt biztosította.  
Végezetül build rendszernek gradle-t használtunk, ~~mert a maven kapja be~~, mivel relatív egyszerű függőség kezelést biztosít és jól együtt működik a különböző java IDEE-kel.

## Rendszer architektúra

Az alábbi ábrán látható architektúrát használja a végleges program.

![](arch.png)

A konkrét komponensek és felelősségeik:

| Komponens | Felelősség |
|:---|---|
| View (UI)|A megjelenítésért felelős komponens, Compose keretrendszert használ. A felhasználó ezzel interaktál, ezen keresztül jutnak el a kérések az alacsonyabb szintekhez (i.e ViewModel)  |
| Torrent ViewModel | Adatot biztosít a View-nak, amit az megtud jeleníteni (az adat lentebbi rétegből származik, a ViewModel csak a megfelelő formára hozza), ill. a felhasználói szándékot kommunikálja le az repository felé|
| TorrentRepository | A mögöttes adatbázis interakciót rejti el, rajta kereszütl kérhetőek le és módosíthatóak az adatbázisban tárolta adatok
| TorrentScheduler | Torrent műveletek (seed, download, pause) indítását kezeli, torrentenként létrehoz egy TorrentHandlert, mely megvalósítja a tényleges elvégzendő műveletet. Egy TorrentContext objecten keresztül adja át a handlernek a megfelelő infókat, illetve callback-eket, ezt a contextet maga generálja. |
| TorrentHandler | Magas szintű API-t biztosít, rejtve ezzel a konkrét torrent implementációt (TorrentEngine), ezen felül lekérdezhető rajta keresztül a TorrentEngine bizonyos adatai (letöltés állapota, seed állapota, stb.) |
| TorrentEngine | Alacsony szintű bittorrent protokol implementáció, peerek lekérdezése, letöltés kezelése, disk IO kezelése a feladata. |

## Adatbázis séma

A szoftver 5 adatbázis táblát használ, melyek az alábbi ábrán látható.

![](dbschema.png)