# API Dokumentation

Eine unvollständige Dokumentation der REST-API.

## Inhaltsverzeichnis

- [Module](#module)
- [Studiengänge und POs](#studiengänge-und-pos)
- [Personen und Gruppen](#personen-und-gruppen)
- [Prüfungsformen](#prüfungsformen)
- [Seasons](#seasons)
- [Modultypen](#modultypen)
- [Sprachen](#sprachen)
- [Status](#status)
- [Prüfungsphasen](#prüfungsphasen)
- [Location](#location)

## Module

GET `/modules?select=metadata&active=true&po=inf_mi5`

Erklärung Query Parameter:

- `select=metadata`: enthält vollständige Modulinformationen (Yaml + Markdown)
- `active=true`: enthält nur Module, die grundsätzlich gelehrt werden (status ist aktiv)
- `po=inf_mi5`: enthält nur Module, die als Pflicht oder Wahlmodul im MI Bachelor PO-5 gelehrt werden. Hier kann man
  auch
  z.B. `inf_mim5` für Module im MI Master PO-5 oder `inf_mi4` für Module im MI Bachelor PO-4 angeben

Erklärung Response Body:

Gibt ein Array von Modulen zurück. Ein Modul besteht aus

- `module.metadata.`
  - `id` (String): die eindeutige Identifizierung eines Modules in unseren Systemen. V4 UUID
  - `title` (String): Modulname
  - `abbrev` (String): Modulkürzel
  - `moduleType` (String): ID des Modultyps (siehe [Modultypen](#modultypen))
  - `ects` (Double): ECTS Punkte
  - `language` (String): ID der Sprache, in der das Modul gelehrt wird (siehe [Sprachen](#sprachen))
  - `duration` (Int): Dauer des Moduls in Semestern. i.d.R. 1
  - `season` (String): ID der Season, wann das Modul angeboten wird (siehe [Seasons](#seasons))
  - `status` (String): ID des Status des Moduls (siehe [Status](#status))
  - `location` (String): ID des Standorts, wo das Modul angeboten wird (siehe [Location](#location))
  - `workload` (Object): Workload des Moduls. Alle Angaben in Stunden
    - `seminar` (Int): Seminar
    - `practical` (Int): Praktikum
    - `projectSupervision` (Int): Projektbetreuung
    - `projectWork` (Int): Projektarbeit
    - `lecture` (Int): Vorlesung
    - `exercise` (Int): Übung
    - `selfStudy` (Int): Selbststudium
    - `total` (Int): Summe aller Stunden
  - `participants` (Object oder null): Teilnehmerbegrenzung, falls vorhanden
    - `min` (Int): Mindestanzahl
    - `max` (Int): Maximalanzahl
  - `moduleRelation` (Object oder null): Ober- und Teilmodul Beziehung (wenn vorhanden)
    - `kind` (String): Entweder `child`, wenn das Modul ein Teilmodul ist, oder `parent`, wenn das Modul ein Obermodul
      ist
    - `parent` (String): ID des Obermoduls, wenn `kind` = `child` ist
    - `children` (Array<String>): ID der Teilmodule, wenn `kind` = `parent` ist
  - `moduleManagement` (Array<String>): ID der Modulverantwortlichen (
    siehe [Personen und Gruppen](#personen-und-gruppen))
  - `lecturers` (Array<String>): ID der Lehrenden (siehe [Personen und Gruppen](#personen-und-gruppen))
  - `assessmentMethods` (Object): Prüfungsformen
    - `mandatory` (Array<Object>): Prüfungsformen für das Modul. Alle Einträge sind mit einer UND-Verbindung kombiniert
      - `method` (String): ID der Prüfungsform (siehe [Prüfungsformen](#prüfungsformen))
      - ~~`precondition`~~ (Array<String>): Gedacht für Prüfungsvoraussetzungen. Bitte nicht verwenden. Wird ersetzt
        durch [Anwesenheitspflicht und Prüfungsvorleistung](#anwesenheitspflicht-und-prüfungsvorleistung)
      - `percentage` (Double oder null): Prozentuale Verteilung bei mehreren mit UND kombinierten Prüfungsformen
    - ~~`optional`~~ (Array): Wird entfernt, bitte nicht verwenden
  - `examiner` (Object): Prüfer (siehe [Personen und Gruppen](#personen-und-gruppen))
    - `first` (String): ID der Erstprüfer*in
    - `second` (String): ID der Zweitprüfer*in
  - `examPhases` (Array<String>): ID der Prüfungsphasen (siehe [Prüfungsphasen](#prüfungsphasen))
  - `prerequisites` (Object): Voraussetzungen
    - `recommended` (Object oder null): Empfohlene Voraussetzungen
      - `modules` (Array<String>): ID der Module, die vorausgesetzt werden
      - ~~`pos`~~ (Array<String>): ID der POs, die vorausgesetzt werden. Wird entfernt, bitte nicht verwenden
      - `text` (String): Freitext
    - `required` (Object oder null): Zwingende Voraussetzungen
      - `modules` (Array<String>): ID der Module, die vorausgesetzt werden
      - ~~`pos`~~ (Array<String>): ID der POs, die vorausgesetzt werden. Wird entfernt, bitte nicht verwenden
      - `text` (String): Freitext
    - `po` (Object): Verwendung des Moduls in Studiengängen und POs
      - `mandatory` (Array<Object>): Verwendung in POs als Pflichtmodul
        - `po` (String): ID der PO (siehe `po.id` in [Studiengänge und POs](#studiengänge-und-pos))
        - `recommendedSemester` (Array<number>): Empfohlene Fachsemester. Kann auch leer sein, wenn es bspw. keine
          Vorgaben gibt (z.B. Master Studiengänge)
        - `specialization` (String oder null): Vertiefung in einer PO, falls vorhanden (siehe `specialization.id`
          in [Studiengänge und POs](#studiengänge-und-pos))
      - `optional` (Array): Verwendung in POs als Wahlmodul
        - `po` (String): ID der PO (siehe `po.id` in [Studiengänge und POs](#studiengänge-und-pos))
        - `recommendedSemester` (Array): Empfohlene Fachsemester. Kann auch leer sein, wenn es bspw. keine Vorgaben
          gibt (z.B. Master Studiengänge)
        - `specialization` (String oder null): Vertiefung in einer PO, falls vorhanden (siehe `specialization.id`
          in [Studiengänge und POs](#studiengänge-und-pos))
        - `instanceOf` (String): ID des generischen Moduls, auf das das Modul einzahlt (siehe [Modultypen](#modultypen))
        - `partOfCatalog` (Boolean): Ob das Modul im Modulhandbuch angezeigt werden soll. Default ist false, da es
          gesonderte WPF Listen gibt
  - `taughtWith` (Array<String>): ID der Module, mit denen das Modul zusammen gelehrt wird
  - `attendanceRequirement` (Object oder null): Anwesenheitspflicht nach Prüfungsordnung. Sollte nur in Ausnahmefällen
    verwendet werden
    - `min` (String): Mindestpräsenzzeit als Zulassungsvoraussetzung zur (Teil) Modulprüfung
    - `reason` (String): Begründung für die Anwesenheitspflicht
    - `absence` (String): Umgang mit Fehlzeiten
  - `assessmentPrerequisite` (Object oder null): Prüfungsvorleistung für die Zulassung zur Modulprüfung. Sollte nur in
    Ausnahmefällen verwendet werden
    - `modules` (String): Betroffene Module
    - `reason` (String): Begründung für die Prüfungsvorleistung
- `module.deContent`: Modulinformationen als Fließtext im Markdown Format. Bestehend aus:
  - `learningOutcome`: Angestrebte Lernergebnisse
  - `content`: Modulinhalte
  - `teachingAndLearningMethods`: Lehr- und Lernmethoden (Medienformen)
  - `recommendedReading`: Empfohlene Literatur
  - `particularities`: Besonderheiten
- `module.enContent`: wie `deContent`, nur in Englisch, falls vorhanden
- `$PO` (optional): enthält alle nicht konsumierten spezifischen Keys der `$PO`. Zudem wird hier auch der Dateiname
  unter `filename` abgelegt, damit das Modul besser zugeordnet werden kann. Beispiele:
  - wenn `inf_mi5`, dann Medieninformatik Bachelor spezifische Keys
  - wenn `inf_mim5`, dann Medieninformatik Master spezifische Keys
  - wenn `inf_dsi1`, dann Digital Sciences Master spezifische Keys (kann aktuell nur wenn in Kombination mit `inf_mim5`
    auftreten)

Keys, die bald entfernt werden (bitte nicht verwenden):

- `module.metadata.competences`
- `module.metadata.globalCriteria`

## Studiengänge und POs

| Methode | Pfad             | Query                     | Beschreibung                                                                                           |
|---------|------------------|---------------------------|--------------------------------------------------------------------------------------------------------|
| GET     | `/studyPrograms` | `filter=currently-active` | Gibt alle aktiven Studiengänge mit PO zurück (PO gestartet und nicht ausgelaufen)                      |
| GET     | `/studyPrograms` | `filter=not-expired`      | Wie `currently-active`, nur dass zusätzlich zukünftige, noch nicht gestartete POs zurückgegeben werden |
| GET     | `/studyPrograms` | –                         | Wie `not-expired` (default)                                                                            |

Gibt alle Studiengänge mit PO zurück. Bei uns haben Studiengänge nicht so einen hohen Wert wie eine PO. So gibt es z.B.
im Studiengang Medieninformatik Bachelor (`inf_mi`) mehrere POs (`inf_mi4`, `inf_mi5`). Module werden entsprechend mit
POs und nicht mit Studiengängen verlinkt.

## Personen und Gruppen

| Methode | Pfad          | Query         | Beschreibung                                                                        |
|---------|---------------|---------------|-------------------------------------------------------------------------------------|
| GET     | `/identities` | –             | Gibt alle Personen und Gruppen zurück                                               |
| GET     | `/identities` | `images=true` | Gibt alle Personen und Gruppen zurück; bei Personen mit Detailseite auch `imageUrl` |

Datenstruktur Person:

```ts
interface Person {
    kind: 'person' // Zur Unterscheidung von Personen und Gruppen
    id: string
    lastname: string
    firstname: string
    title: string
    faculties: string[]
    abbreviation: string
    campusId: string | null
    isActive: boolean
    employmentType: EmploymentType
    imageUrl: string | null // URL zum Bild aus der Personendetailseite
    websiteUrl: string | null // URL zur Personendetailseite
}

type EmploymentType =
    | "prof" // Professor
    | "wma" // Wissenschaftlicher Mitarbeiter
    | "adjunct_lecturer" // Lehrbeauftragter
    | "unknown" // Unbekannt
```

Datenstruktur Gruppe:

```ts
interface Group {
    kind: 'group' // Zur Unterscheidung von Personen und Gruppen
    id: string
    label: string
}
```

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

Auf Wunsch einiger PAVs haben wir noch folgende Prüfungsformen als *valide* deklariert:

- `e-exam`

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

## Anwesenheitspflicht und Prüfungsvorleistung

TODO
