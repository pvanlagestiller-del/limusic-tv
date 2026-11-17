# LiMusic TV

LiMusic für Fire TV / Android TV: YouTube Music und YouTube ohne Werbung, schwarz-rotes Design, Bedienung mit der Fernbedienung.

## Funktionen
- Oben die Tabs **Musik · Videos · Suche · Einstellungen**. Ein Tab wird schon beim Draufgehen gewechselt, wie bei Apple TV.
- Musik: Zuletzt gehört, Charts, Neu erschienen, Deutschpop, Musikvideos.
- Videos: Zuletzt gesehen, Dokumentationen, Natur, Nachrichten.
- Suche über die Fire-TV-Tastatur (auch per Spracheingabe der Tastatur). Ergebnisse getrennt nach Songs und Videos.
- Player: Weiter und Zurück, Spulen, Medientasten der Fernbedienung. Bei Musik wird das Cover groß angezeigt.
- Autoplay: Am Ende der Liste kommen passende Titel automatisch hinterher.
- Einstellungen: maximale Auflösung (360p bis 2160p), Musik nur als Audio, Autoplay, Verlauf löschen.
- **Keine Werbung:** Die Streams werden direkt über den NewPipe Extractor geladen, eine YouTube-Oberfläche gibt es nicht. Ein Konto ist nicht nötig.

## Bauen
In ein GitHub-Repo pushen. Die Action **Build APK** baut die Datei, danach unter *Artifacts* `app-release.apk` herunterladen.

## Installieren
```bash
adb connect <IP>:5555
adb install -r app-release.apk
```
Danach erscheint LiMusic im LiMaD TV Launcher.

## Hinweise
- Wenn YouTube etwas umstellt, bricht der Extractor gelegentlich. Dann reicht es meist, in `app/build.gradle.kts` die Version von `NewPipeExtractor` auf die neueste Release zu setzen und neu zu bauen.
- Bei sehr hohen Auflösungen (1440p/2160p) liefert YouTube meist VP9 oder AV1. Der Fire TV Stick 4K (2. Gen) kann beides dekodieren.
- Lizenz: Der NewPipe Extractor steht unter GPLv3. Für die private Nutzung spielt das keine Rolle. Wenn du die App weitergibst, muss auch der Quellcode mitgegeben werden.
