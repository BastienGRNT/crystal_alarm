# CLAUDE.md

**Crystal Alarm** (toujours ce nom complet, jamais « Crystal » seul) : réveil Android minimaliste, en Kotlin + Jetpack Compose. Un seul réveil, pas de snooze, et un code (QR / code-barres) à scanner pour l'arrêter.

## Règles métier (non négociables)

Ces règles sont la raison d'être de l'app. Ne jamais les assouplir sans demande explicite.

1. **Un seul réveil à la fois.** `SetAlarmUseCase` remplace le précédent. Pas de liste, pas de multi-réveils.
2. **Pas de snooze, pas de pause.** La notification de sonnerie n'a aucune action. Le bouton retour ne fait rien sur `RingingActivity`.
3. **Arrêt uniquement par :**
   - scan du code enregistré (`DismissWithCodeUseCase`), ou
   - **mode secours** (`DismissWithRescueUseCase`) : recopier un texte de `RescueChallenge.TEXT_WORDS` mots tirés au hasard (environ 4 lignes ; casse, accents et espaces ignorés), puis résoudre un calcul simple (addition 12–49 ou table de 3 à 9). Un mauvais résultat change le calcul sans refaire le texte. Le texte cible n'est pas sélectionnable (pas de copier-coller).
4. **Verrou après réveil** : une fois l'alarme arrêtée, impossible de créer ou de modifier un réveil pendant `Settings.lockMinutes` (30 min par défaut, 5 à 180, pas de 5). Désactiver reste possible (réactiver = créer, donc bloqué). **La durée n'est pas modifiable tant qu'un verrou est actif** (sinon on pourrait le contourner).
5. **Codes nommés** : l'utilisateur enregistre plusieurs codes avec un nom (« Dentifrice », « Café »…). **Un seul est actif** et seul lui arrête l'alarme. Pas de code actif, pas de réveil (`SetAlarmResult.MissingWakeCode`). Pendant la sonnerie on ne peut ni ajouter, ni activer, ni supprimer un code. On ne peut pas supprimer le dernier code si un réveil est programmé.
6. **Anti-triche** : redémarrer le téléphone pendant la sonnerie la relance, et un réveil raté pendant que le téléphone était éteint sonne au redémarrage (dans la limite de `RestoreAlarmUseCase.MISSED_GRACE`). Le volume d'alarme choisi est réimposé en continu pendant la sonnerie (baisser le son ne sert à rien). Quitter l'écran de sonnerie (accueil, autre appli, écran éteint) le fait revenir toutes les 1,5 s via `RingingScreenKeeper`, tant que ça sonne (nécessite « Afficher par-dessus les autres applis »).
7. **Pas de répétition** : le réveil sonne une seule fois, puis se désactive (`FinishRingingUseCase`). L'utilisateur le réactive à la main. Pas de jours de la semaine, volontairement.
8. **Son** (`SoundSettings`) : sonnerie (intégrée, importée ou celle du téléphone), volume de `MIN_VOLUME` (20 %) à 100 %, démarrage direct ou progressif (montée sur `RAMP_SECONDS`), vibreur on/off. Si la sonnerie choisie est illisible, on retombe sur la sonnerie par défaut puis sur celle du système : l'alarme doit **toujours** sonner.
9. **Activer / désactiver** : désactiver (`CancelAlarmUseCase`) annule le réveil mais garde son heure (`AlarmRepository.lastTime`), tout comme un réveil qui a sonné. `EnableAlarmUseCase` le réactive à la même heure en passant par `SetAlarmUseCase` (mêmes règles). L'éditeur part toujours de la dernière heure. Accueil : interrupteur, et toucher l'heure ouvre l'éditeur. Widget : `--:--` dès que rien n'est actif.

## Architecture (Clean Architecture)

```
:domain  (module Kotlin/JVM pur, aucune dépendance Android)
  model/        Alarm, AlarmState (None | Scheduled | Ringing), Lock, Settings, WakeCode, SoundSettings, Ringtone, WakeUpRecord
  repository/   interfaces AlarmRepository, LockRepository, SettingsRepository, RingtoneRepository, WakeUpHistoryRepository
  port/         interfaces vers le système : AlarmScheduler, TimeSource, RingtonePreviewer
  usecase/      une classe = une action métier, `operator fun invoke`
  rescue/       RescueChallenge (logique pure du mode secours : texte à recopier + calcul)

:app  (Android)
  data/           implémentations des repositories (DataStore Preferences, listes en JSON)
    ringtone/     sonneries intégrées (res/raw) + importées (copiées dans filesDir/ringtones)
  platform/       adaptateurs système Android
    alarm/        AndroidAlarmScheduler (setAlarmClock), AlarmReceiver, SystemEventsReceiver,
                  AlarmRingingService (foreground service), AlarmNotifications
    sound/        AlarmSoundPlayer (volume, rampe, vibreur), AndroidRingtonePreviewer
    widget/       widget Glance (NextAlarmWidget), WidgetUpdater, WidgetTicker
    system/       AppPermissions, helpers de BroadcastReceiver
    time/         SystemTimeSource
  presentation/   Compose : un dossier par écran (Screen + ViewModel + UiState)
    theme/ components/ format/ scanner/
    home/ editor/ settings/ sound/ register/ ringing/ rescue/ history/ main/
  di/             modules Hilt (bindings domaine -> implémentations) et EntryPoint des receivers
```

### Règle de dépendance

`presentation` → `domain` ← `data` / `platform`. Le domaine ne connaît rien d'Android ni des couches externes.

- Le module `:domain` est un module JVM : **aucun import `android.*` / `androidx.*`** n'y compile, c'est voulu.
- Toute règle métier vit dans un use case du domaine, jamais dans un ViewModel, un receiver ou un service.
- Les ViewModels appellent des use cases ; ils n'accèdent pas directement aux repositories.
- Exceptions assumées : `RingingActivity` et `AlarmRingingService` observent `AlarmRepository.state` en lecture seule pour savoir quand s'arrêter ; le service lit `SettingsRepository` pour les réglages de son ; `AlarmSoundPlayer` et `AndroidRingtonePreviewer` utilisent `DataStoreRingtoneRepository.playbackCandidates()` pour résoudre l'URI d'une sonnerie.
- Le temps passe toujours par `TimeSource` (jamais `Instant.now()` dans le domaine) pour que les tests restent déterministes.
- Une nouvelle capacité système = une interface dans `domain/port`, une implémentation dans `platform/`, un `@Binds` dans `di/BindingsModule`.

### Flux de sonnerie

1. `AndroidAlarmScheduler` arme `AlarmManager.setAlarmClock` → `AlarmReceiver`.
2. `AlarmReceiver` démarre `AlarmRingingService` (foreground, type `mediaPlayback`).
3. Le service appelle `StartRingingUseCase` (état → `Ringing`), joue le son, poste la notification full-screen qui ouvre `RingingActivity` (par-dessus l'écran de verrouillage).
4. Le dismiss (scan ou secours) passe par `FinishRingingUseCase`, qui change l'état et pose le verrou. **Le service et l'activité réagissent au changement d'état** : personne n'appelle le service directement.

### Widget

`WidgetUpdater.startSync()` (lancé dans `CrystalApp`) observe `ObserveDashboardUseCase` et pousse un `WidgetSnapshot` pré-formaté dans l'état Glance. **L'info principale du widget est le temps restant avant la sonnerie** (« 7 h 32 »), l'heure et le jour viennent en second mais restent lisibles. En dessous : le temps restant avant l'heure du coucher (`Alarm.bedtime(Settings.sleepGoal)`, objectif de sommeil réglable de 5 h à 10 h par pas de 15 min, 7 h 30 par défaut, masqué au-delà de 24 h) et, si un verrou tourne, le temps de verrou restant. Rien d'actif (désactivé ou jamais réglé) : `--:--`. Le compte à rebours et la pastille prennent la couleur de `SleepIndicator` autour de l'objectif : vert menthe pastel entre l'objectif et objectif + 2 h, toujours rouge en dessous de l'objectif, du rouge clair au rouge profond à 4 h (l'utilisateur est daltonien : ne faire varier que l'intensité, jamais la teinte), couleurs classiques au-delà. Trois mises en page selon la taille réelle (`SizeMode.Exact`) : bandeau 4x1 (hauteur < 100dp), compact 2x2 (largeur < 220dp), large 4x2. `WidgetTicker` rafraîchit le compte à rebours chaque minute via une alarme RTC **non-wakeup** (coût nul quand le téléphone dort).

### Suivi des réveils

`FinishRingingUseCase` enregistre chaque arrêt dans `WakeUpHistoryRepository` (`WakeUpRecord` : heure du réveil, début de sonnerie = `Ringing.since`, arrêt, `DismissMethod` code ou secours ; les 90 derniers, le plus récent en premier). Un redémarrage pendant la sonnerie ne remet pas le chrono à zéro. `ObserveWakeUpStatsUseCase` calcule la moyenne des 7 derniers, le record et le nombre de secours. Affichage : carte « Éteint en… » sur l'accueil et écran `history/` (accessible aussi depuis les Réglages).

## Conventions

- Kotlin officiel, 4 espaces, virgules finales. Pas de `!!` sauf invariant évident.
- UI 100 % Compose + Material 3, thème sombre uniquement. Couleurs et typo **uniquement** via `CrystalColors` / `CrystalType` ; réutiliser les composants de `presentation/components` (`CrystalButton`, `CrystalCard`, `InfoRow`, `ScreenHeader`…) avant d'en créer de nouveaux.
- Style visuel : minimaliste, fond quasi noir, gros chiffres tabulaires, cartes arrondies 24dp, bouton principal blanc sur noir, accent violet/glace. Pas de surcharge.
- Textes : tout en français dans `res/values/strings.xml`, jamais en dur dans le code. Tutoiement.
- État UI : `StateFlow` exposé par le ViewModel, collecté avec `collectAsStateWithLifecycle`. Événements one-shot via `Channel`.
- Navigation type-safe (routes `@Serializable` dans `presentation/main/CrystalNavHost.kt`).
- DI : Hilt. Les use cases du domaine ont un constructeur `@Inject` (javax.inject, seule dépendance non-coroutines du domaine). Les `BroadcastReceiver` passent par `ReceiverEntryPoint`.
- Persistance : un seul DataStore Preferences (`crystal`), clés dans `data/local/PreferenceKeys.kt`, listes sérialisées en JSON (DTO dans `data/local/Dtos.kt`). Toute évolution de format doit lire l'ancien (voir la migration du code unique v1.0 dans `DataStoreSettingsRepository`).
- Sonneries intégrées : générées par `tools/generate_ringtones.py` (Python pur, WAV en boucle). Pour en ajouter une : une fonction dans le script, une entrée dans `BuiltinRingtone`, deux strings (nom + description). Ne jamais ajouter de son sous droits.
- Tout nouveau use case ou règle métier est accompagné d'un test dans `domain/src/test` (fakes dans `Fakes.kt`).

## Commandes

```bash
./gradlew :domain:test          # tests unitaires du domaine (rapides, JVM)
./gradlew :app:assembleDebug    # APK debug
./gradlew :app:installDebug     # installer sur l'appareil connecté
./gradlew :app:lintDebug        # lint Android
./gradlew :app:assembleRelease  # build R8 (vérifie les règles proguard)
python3 tools/generate_ringtones.py  # régénère les sonneries intégrées
```

Toolchain : JDK 17, AGP 9 (Kotlin intégré), Kotlin 2.4, KSP, compileSdk 37, minSdk 29, targetSdk 36. Versions centralisées dans `gradle/libs.versions.toml`.

## Points d'attention Android

- Permissions critiques : notifications, intent plein écran (Android 14+), alarmes exactes, caméra, affichage par-dessus les autres applis (`SYSTEM_ALERT_WINDOW`, pour relancer l'écran de sonnerie depuis l'arrière-plan ; une fenêtre overlay invisible de 1 px est posée pendant la sonnerie car Android 15+ n'honore cette exemption que si l'app a un overlay visible). `PermissionsCard` les affiche sur l'accueil si elles manquent.
- `USE_EXACT_ALARM` est légitime ici (app de réveil) ; `SCHEDULE_EXACT_ALARM` seulement jusqu'à l'API 32.
- Un force-stop efface les alarmes : `CrystalApp` relance `RestoreAlarmUseCase` à chaque démarrage du process (idempotent).
- Ne pas utiliser `goAsync()` dans `NextAlarmWidgetReceiver` (Glance l'utilise déjà).
