# API Dokumentation

Eine unvollständige Dokumentation der REST-API.

## Module

GET `/modules?select=metadata&active=true&po=inf_mi5`

Erklärung Query Parameter:

- `select=metadata`: enthält volle Modulinformationen (quasi Yaml + Markdown)
- `active=true`: enthält nur Module, die grundsätzlich gelehrt werden (status ist aktiv).
- `po=inf_mi5`: enthält nur Module, die als Pflicht oder Wahlmodul im MI Bachelor PO5 gelehrt werden. Hier kann man auch
  z.B. `inf_mim5` für Module im MI Master PO5 oder `inf_mi4` für Module im MI Bachelor PO 5 angeben.

Erklärung Response Body:

Gibt ein Array von Modulen zurück. Ein Modul besteht aus

- `module.metadata`
  - `id` (String): die eindeutige Identifizierung eines Modules in unseren Systemen
  - `title` (String): Modulname
  - `abbrev` (String): Modulkürzel
  - `participants` (Object oder null): Teilnehmerbegrenzung, falls vorhanden
    - `min` (Int): Mindestanzahl
    - `max` (Int): Maximalanzahl
  - `assessmentMethods` (Object): Prüfungsformen
    - `mandatory` (Array): Prüfungsformen für POs, in denen das Modul als Pflichtmodul auftaucht. Mehrere Einträge sind
      mit einer UND-Verbindung kombiniert.
      - `method` (String): ID der Prüfungsform (siehe [Prüfungsformen](#prüfungsformen))
      - `precondition` (Array): Gedacht für Prüfungsvoraussetzungen. Bitte nicht verwenden
      - `percentage` (Double oder null): Prozentuale Verteilung bei mehreren mit UND kombinierten Prüfungsformen
    - `optional` (Array): Prüfungsformen für POs, in denen das Modul als Wahlmodul auftaucht. Achtung: Wir wollen diesen
      Key loswerden. Daher bitte nicht verwenden
  - `season` (String): ID der Season, wann das Modul angeboten wird (siehe [Seasons](#seasons))
  - `language` (String): ID der Sprache, in der das Modul gelehrt wird (siehe [Sprachen](#sprachen))
  - `moduleType` (String): ID des Modultyps (siehe [Modultypen](#modultypen))
  - `location` (String): ID des Standorts (siehe [Location](#location))
  - `status` (String): ID des Status (siehe [Status](#status))
  - `ects` (Double): ECTS Punkte
  - `competences` (Array): Wird bald entfernt. Bitte nicht verwenden
  - `globalCriteria` (Array): Wird bald entfernt. Bitte nicht verwenden
  - `duration` (Int): Dauer des Moduls in Semestern, i.d.R. 1
  - `examiner` (Object): Prüfer (siehe [Personen und Gruppen](#personen-und-gruppen))
    - `first` (String): ID der Erstprüfer*in
    - `second` (String): ID der Zweitprüfer*in
  - `examPhases` (Array): ID der Prüfungsphasen (siehe [Prüfungsphasen](#prüfungsphasen))
  - `moduleManagement` (Array): ID der Modulverantwortlichen (siehe [Personen und Gruppen](#personen-und-gruppen))
  - `lecturers` (Array): ID der Lehrenden (siehe [Personen und Gruppen](#personen-und-gruppen))
  - `workload` (Object): Workload des Moduls. Jeweils in Stunden
    - `seminar` (Int): Seminar
    - `practical` (Int): Praktikums
    - `projectSupervision` (Int): Projektbetreuungs
    - `projectWork` (Int): Projektarbeit
    - `lecture` (Int): Vorlesungs
    - `exercise` (Int): Übungs
    - `selfStudy` (Int): Selbststudium
    - `total` (Int): Summe aller Stunden
  - `moduleRelation` (Object oder null): Ober- und Submodul Beziehung (wenn vorhanden)
    - `kind` (String): Entweder `child`, wenn das Modul ein Submodul ist, oder `parent`, wenn das Modul ein Obermodul
      ist
    - `parent` (String): ID des Obermoduls, wenn `kind` = `child` ist
    - `children` (Array): ID der Submodule, wenn `kind` = `parent` ist
  - `taughtWith` (Array): ID der Module, mit denen das Modul zusammen gelehrt wird
  - `prerequisites` (Object): Voraussetzungen
    - `recommended` (Object oder null): Empfohlene Voraussetzungen
      - `modules` (Array): ID der Module, die vorausgesetzt werden
      - `pos` (Array): ID der POs, die vorausgesetzt werden
      - `text` (String): Freitext
    - `required` (Object oder null): Zwingende Voraussetzungen
      - `modules` (Array): ID der Module, die vorausgesetzt werden
      - `pos` (Array): ID der POs, die vorausgesetzt werden
      - `text` (String): Freitext
    - `po` (Object): Verwendung des Moduls in POs
      - `mandatory` (Array): Verwendung in POs als Pflichtmodul
        - `po` (String): ID der PO (siehe `po.id` in [Studiengänge und POs](#studiengänge-und-pos))
        - `recommendedSemester` (Array): Empfohlene Fachsemester
        - `specialization` (String oder null): Vertiefung in einer PO, falls vorhanden (siehe `specialization.id`
          in [Studiengänge und POs](#studiengänge-und-pos))
      - `optional` (Array): Verwendung in POs als Wahlmodul
        - `po` (String): ID der PO (siehe `po.id` in [Studiengänge und POs](#studiengänge-und-pos))
        - `recommendedSemester` (Array): Empfohlene Fachsemester
        - `specialization` (String oder null): Vertiefung in einer PO, falls vorhanden (siehe `specialization.id`
          in [Studiengänge und POs](#studiengänge-und-pos))
        - `instanceOf` (String): ID des generischen Moduls, auf das das Modul einzahlt (siehe [Modultypen](#modultypen))
        - `partOfCatalog` (Boolean): Ob das Modul im Modulhandbuch angezeigt werden soll
- `module.deContent`: Modulinformationen als Fließtext im Markdown Format. Bestehend aus:
  - `learningOutcome`: Angestrebte Lernergebnisse
  - `content`: Modulinhalte
  - `teachingAndLearningMethods`: Lehr- und Lernmethoden (Medienformen)
  - `recommendedReading`: Empfohlene Literatur
  - `particularities`: Besonderheiten
- `module.enContent`: wie `deContent`, nur in englisch, falls vorhanden
- `$PO` (optional): enthält alle nicht konsumierten spezifischen Keys der `$PO`. Zudem wird hier auch der Dateiname
  unter `filename` abgelegt, besser zugeordnet werden kann. Beispiele:
  - wenn `inf_mi5`, dann Medieninformatik Bachelor spezifische Keys
  - wenn `inf_mim5`, dann Medieninformatik Master spezifische Keys
  - wenn `inf_dsi1`, dann Digital Sciences Master spezifische Keys (kann aktuell nur wenn in Kombination mit `inf_mim5`
    auftreten)

## Studiengänge und POs

GET `/studyPrograms?extend=true`

Gibt alle Studiengänge mit PO zurück. Bei uns haben Studiengänge nicht so einen hohen Wert wie eine PO. So gibt es z.B.
im Studiengang Medieninformatik Bachelor (`inf_mi`) mehrere POs (`inf_mi4`, `inf_mi5`). Module werden entsprechend mit
POs und nicht mit Studiengängen verlinkt.

## Personen und Gruppen

GET `/identities`

Gibt alle menschlichen Personen und Gruppen zurück

## Prüfungsformen

GET `/assessmentMethods`

Gibt alle Prüfungsformen zurück.

Aktuell befinden sich noch Prüfungsformen im System, die nicht den Prüfungsformen der Rahmenprüfungsordnung (RPO)
entsprechen. Diese werden bald entfernt. Die RPO konformen Prüfungsformen sind:

- `written-exam`
- `written-exam-answer-choice-method`
- `oral-exam`
- `home-assignment`
- `open-book-exam`
- `project`
- `portfolio`
- `practical-report`
- `oral-contribution`
- `certificate-achievement`
- `performance-assessment`
- `role-play`
- `admission-colloquium`
- `specimen`

## Seasons

GET `/seasons`

Gibt alle Seasons zurück

## Modultypen

GET `/moduleTypes`

Gibt alle Modultypen zurück. Wir unterscheiden aktuell zwischen zwei Typen von Modulen

- `module`: ein normales Modul, welches so in der Form von Studierenden belegt wird und im Studienverlaufsplan /
  Stundenplan auftaucht. Beispiele: Mathe, AP2.
- `generic_module`: ein Platzhalter Modul, worauf andere Module einzahlen bzw. welches von anderen konkreten Modulen
  instanziert wird. Taucht im Studienverlaufsplan, aber nicht im Stundenplan auf. Beispiele: Wahlmodul, WPF, Guided
  Project.

## Sprachen

GET `/languages`

Gibt alle Sprachen zurück

## Status

GET `/status`

Gibt alle Status zurück

## Prüfungsphasen

GET `/examPhases`

Gibt alle Prüfungsphasen zurück

## Location

GET `/locations`

Gibt alle Standorte zurück
