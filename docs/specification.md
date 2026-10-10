# Viktorrente – Követelményspecifikáció

Szoftverarchitektúrák házi feladat

## 1. Feladatkiírás
A projekt célja egy modern, krosszplatformos BitTorrent kliens fejlesztése **Compose Multiplatform (CMP)** technológia használatával. A **Viktorrente** nevű alkalmazás célja, hogy felületet biztosítson a peer-to-peer (P2P) alapú fájlmegosztáshoz.

---

## 2. A fejlesztőcsapat
A projekt megvalósítását egy speciális szaktudású mérnökökből, nemzetközileg ismert és elismert szakértőkből álló agilis fejlesztő csapat végzi, amely a szoftveréletciklus minden fázisáért felelős.

| Név | Neptun | Email cím |
| :--- | :--- | :--- |
| **Abonyi Bence Péter** | D10QTW | bence.abonyi@edu.bme.hu  |
| **Kovács Nimród Ányos** | CZ4WH5 | kovacsnimrodanyos@edu.bme.hu |
| **Tóth Máté** | XBVV06 | tothmate04@edu.bme.hu |
| **Szobek Ádám Márk** | RV30LP | szobek69420@edu.bme.hu |

---

## 3. Részletes feladatleírás

### 3.1. Felhasználói profil
A szoftver indításakor a felhasználó egy lokális profillal rendelkezik. 
* **Munkamenet kezelés:** Az alkalmazás a konfigurációt perzisztens módon (SQLite) tárolja felhasználónként (pl. letöltési mappa, sávszélesség-korlátok).

### 3.2. Torrentkezelés
A torrentek az alkalmazás magját képzik, amelyek az alábbi fázisokon mennek keresztül:
1. **Importálás:** `.torrent` fájlok megnyitása. A rendszer validálja a metaadatokat (info hash, fájllisták).
2. **Metaadat-lekérdezés:** Kapcsolódás a tracker szerverekhez és a peer-ek felkutatása.
3. **Leechelés:** A fájlok darabolása (piece/chunk alapú letöltés), ellenőrzőösszeg (SHA-1 hasítófüggvény) validáció valós időben, sávszélesség-elosztás.
4. **Seedelés:** A letöltés befejezése után a fájlok rendelkezésre bocsátása a többi peer számára a megadott arányig (ratio) vagy manuális leállításig.
5. **Törlés:** A torrent feladat eltávolítása a listából (opcionálisan a letöltött fájlok törlésével).

### 3.3. BitTorrent protokoll és hálózati réteg
* **Többszálas végrehajtás:** peerek párhuzamos kezelése
* **Bencode:** feladatra optimalizált indahouse dekóder
* **Tracker:** *lesz...*

### 3.4. Extra funkció: peerek részletes adatai
A felhasználó számára egy dedikált nézet érhető el az aktív kapcsolatokról:
* **Peer lista:** Az egyes kapcsolódó IP-címek, kliens-azonosítók (peer ID), letöltési/feltöltési sebességek, valamint a letöltött százalékos arány vizuális megjelenítése.

---

## 4. Technikai paraméterek
* **Alaptechnológia:** Kotlin (Java 17+) Compose Multiplatform
* **Adatbázis / Lokális tárolás:** Room a beállítások, felhasználói adatok és torrent metaadatok helyi mentésére.
* **Aszinkron programozás:** Kotlin Coroutines és Flow a hálózati műveletek, fájl-I/O és a UI közötti reaktív adatáramláshoz.
* **Hálózati réteg:** Ktor vagy egyedi TCP/UDP socket alapú BitTorrent engine implementáció.
* **Célplatformok:** 
  * Asztali környezet: Windows 10/11, Linux.

---

## 5. Szótár
* **Compose Multiplatform (CMP):** A JetBrains által fejlesztett keretrendszer, amely lehetővé teszi a deklaratív UI megosztását több platform között.
* **Peer:** Egy másik kliens a P2P hálózatban, akitől adatot töltünk le, vagy akinek adatot küldünk.
* **Tracker:** Olyan szerver, amely segít a torrent klienseknek egymásra találni (IP címek cseréje útján).
* **Chunk / Piece:** A torrent fájlok kisebb, egyenlő méretű darabokra bontott egységei, amelyek külön-külön kerülnek letöltésre és ellenőrzésre.
* **Leech (Leechelés):** Olyan állapot, amikor a felhasználónak a fájl 100%-a nincs a birtokában és azt aktívan letölti másoktól.
* **Seed (Seedelés):** Olyan állapot, amikor a felhasználónak a fájl 100%-a birtokában van és azt aktívan megosztja másokkal.


