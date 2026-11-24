# Telepítési dokumentáció

A programot forráskódból kell buildelni, ehhez a következőkre van szükség:
- **JDK, legalább 17-es verzió** (tesztelve: OpenJDK 21) – https://jdk.java.net/
- Gradle-t nem szükséges külön telepíteni: a projektben található Gradle wrapper (`gradlew`) az első futtatáskor automatikusan letölti a megfelelő verziót.
- A JDK az IntelliJ IDEA fejlesztőkörnyezettel együtt is letölthető – https://www.jetbrains.com/idea/download/

Ha a függőségeket telepítettük, a következő paranccsal futtathatjuk az alkalmazást (az `src` mappában kell kiadni):
```bash
./gradlew :desktopApp:run
```
Windows alatt:
```bat
gradlew.bat :desktopApp:run
```

Ha csak a futtatható binárist szeretnénk előállítani, azt a következő paranccsal tehetjük meg:
```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
```
Ezt követően az `src/desktopApp/build/compose/jars/` mappában találjuk a generált jar fájlt `org.vik.torrente-<os>-<arch>-1.0.0.jar` néven (pl. `org.vik.torrente-windows-x64-1.0.0.jar` vagy `org.vik.torrente-linux-x64-1.0.0.jar`), amelyet kézzel is futtathatunk:
```bash
java -jar src/desktopApp/build/compose/jars/org.vik.torrente-<os>-<arch>-1.0.0.jar
```

Az alkalmazás az adatait a futtatás munkakönyvtárában létrehozott `torrent.db` SQLite fájlban tárolja. Ha ezt töröljük, a felvett torrentek listája elveszik (a már letöltött fájlok megmaradnak a letöltési könyvtárban).
