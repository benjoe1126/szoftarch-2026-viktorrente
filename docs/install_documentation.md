# Telepítési dokumentáció

A program forráskódból buildelendő, ehhez szükség van a következőkre:
- openjdk (>=21.0.11, tesztelt verzió) - https://jdk.java.net/27/
- gradle (>=9.5.1, tesztelt verzió) - https://docs.gradle.org/current/userguide/installation.html
- az előbbi függőségek letölthetőek az Intelij IDEA IDEE-vel együtt is - https://www.jetbrains.com/idea/download/?section=windows

Ha a függőségeket letöltöttük, akkor a következő parancsokkal futtathatjuk is az alkalmazásunkat
```bash
./gradlew :desktopApp:run # src mappában kell kiadni
```

Ha esetleg szeretnénk a binárist előállítani csak, akkor ezt megtehetjük a következő paranccsal:
```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
```
Ezt követően a `src/desktopApp/build/compose/jars/org.vik.torrente-windows-x64-1.0.0.jar` alatt megtaláljuk a generált jar filet, amit akár kézzel is lefuttathatunk.
```bash
java -jar src/desktopApp/build/compose/jars/org.vik.torrente-windows-x64-1.0.0.jar
```
