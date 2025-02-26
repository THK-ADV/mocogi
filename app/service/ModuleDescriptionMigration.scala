package service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

import scala.annotation.unused
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

import git.GitFileContent
import io.circe.yaml.parser.parse
import io.circe.yaml.syntax.AsYaml
import io.circe.Json
import models.FullPoId
import ops.FileOps.FileOps0
import parsing.YamlParser
import play.api.Logging

@unused
final class ModuleDescriptionMigration extends Logging {

//  private val map = mutable.HashMap.empty[String, Set[String]]

  private def lookupPerson(str: String): String =
    str match
      case "lkoehler" | "lkoe" => "lkoe"
      case "tbb"               => "tbb"
      case "jka"               => "jka"
      case "tkl"               => "tkl"
      case "hwe"               => "hwe"
      case "nn"                => "nn"
      case "cwe"               => "cwe"
      case "sbe" | "sb"        => "sbe"
      case "uwm"               => "umue"
      case "ald"               => "ado"
      case "cfrick" | "cfr"    => "cfr"
      case "hls" | "hst"       => "hst"
      case "fv" | "fvi"        => "fvi"
      case "dgaida"            => "dga"
      case "jschaible"         => "jos"
      case "all"               => "all-inf-prof"
      case "seckstein" | "sec" => "sec"
      case "gh" | "gha"        => "gha"
      case "cn" | "cno"        => "cno"
      case "dz" | "dzue"       => "dzue"
      case "fn"                => "fni"
      case "wk" | "wko"        => "wko"
      case "sstumpf" | "sst"   => "sst"
      case "rmajewski" | "rma" => "rma"
      case "il" | "ili"        => "ili"
      case "skullack"          => "sku"
      case "mza"               => "mza"
      case "ddahl"             => "dda"
      case "rgroten" | "rgr"   => "rgr"
      case "mw" | "mwi"        => "mwi"
      case "jhenke"            => "jhe"
      case "moe" | "men"       => "men"
      case "dpetersen"         => "dps"
      case "ck" | "cko"        => "cko"
      case "mlinden"           => "mli"
      case "dschmitt"          => "dsc"
      case "msator"            => "msa"
      case "priemer"           => "pri"
      case "sk" | "ska"        => "ska"
      case "viet"              => "hvn"
      case "mi"                => "all-mi-prof"
      case "hk" | "hkornacher" => "hko"
      case "bb"                => "bbe"
      case "mbluemm" | "mbl"   => "mbl"
      case "mboehmer" | "mboe" => "mboe"
      case "lbrandhoff"        => "lbr"
      case "simonporten"       => "spo"
      case "alm"               => "ama"
      case "hs"                => "hste"
      case "psc"               => "psc1"
      case "ghe"               => "ghe"
      case "kle"               => "kle"
      case "kfoe"              => "kfoe"
      case "esc"               => "esc"
      case "sfue"              => "sfue"
      case "tga"               => "tga"
      case "cpa"               => "cpa"
      case "bna"               => "bna"
      case "hgue"              => "hgue"
      case "rwoe"              => "rwoe"
      case "fli"               => "fli"
      case "agr"               => "agr"
      case "ela"               => "ela"
      case "jsc"               => "jsc"

  private def lookupLang(str: String): String =
    str match
      case "deutsch" | "Deutsch" | "deutsch, Seminar-Basisliteratur i.d.R. in englischer Sprache" => "de"
      case "englisch" | "Englisch" | "english"                                                    => "en"
      case "Deutsch oder Englisch" | "wahlweise deutsch oder englisch"                            => "de_or_en"
      case "modules.language.options.de"                                                          => "de"
      case "modules.language.options.en"                                                          => "en"
      case "modules.language.options.de_and_en"                                                   => "de_en"
      case "modules.language.options.de_or_en"                                                    => "de_or_en"

  private def lookupLocation(str: String): String =
    str match
      case "Remote, teils hybrid"               => "remote"
      case "Vor Ort" | "vor Ort"                => "gm"
      case "Remote, Projektwoche vor Ort"       => "gm"
      case "Präsenz"                            => "gm"
      case "vor Ort Seminar bzw. Workshop"      => "gm"
      case "modules.location.options.kdeutz"    => "dz"
      case "modules.location.options.all_sites" => "other"
      case "modules.location.options.gm"        => "gm"
      case "modules.location.options.ksued"     => "su"
      case "modules.location.options.remote"    => "remote"

  private def lookupSeason(str: String): String =
    str match
      case "modules.semester.options.ss" | "modules.semester.options.ss_opt"       => "ss"
      case "modules.semester.options.ws"                                           => "ws"
      case "modules.semester.options.ss_ws" | "modules.semester.options.ss_ws_opt" => "ws_ss"

  // TODO there is not always a 1 to 1 lookup of the module exams...
  private def lookupDSI1Exam(str: String): List[String] =
    str match
      case "experttalk-or-writtenexam"        => List("oral-exam")                            // forced to one
      case "project-or-experttalk"            => List("project")                              // forced to one
      case "writtenexam-project-presentation" => List("written-exam", "project", "oral-contribution")
      case "writtenexam-assignments"          => List("written-exam", "home-assignment")
      case "writtenexam-postersession"        => List("written-exam", "oral-contribution")
      case "project-presentation"             => List("project", "oral-contribution")
      case "project-presentation-experttalk"  => List("project", "oral-contribution", "oral-exam")
      case "project-portfolio-experttalk"     => List("project", "portfolio", "oral-exam")
      case "project-experttalk"               => List("project", "oral-exam")
      case "project-paper-presentation"       => List("project", "home-assignment", "oral-contribution")
      case "paper-casestudy"                  => List("home-assignment")                      // no distinct matching
      case "paper-presentation"               => List("home-assignment", "oral-contribution") // no distinct matching
      case "experttalk"                       => List("oral-exam")
      case "presentation-reflectivesummary"   => List("oral-contribution", "home-assignment") // no distinct matching
      case "paper"                            => List("home-assignment")                      // no distinct matching
      case "portfolio-reflection-test"        => List("portfolio", "home-assignment")

//  def path = "/Users/alex/Developer/reakkreditierung-main/src/medieninformatik-bachelor/modulbeschreibungen-bpo5"
//  def path = "/Users/alex/Developer/reakkreditierung-main/src/medieninformatik-master/modulbeschreibungen-mpo5"
//  def path = "/Users/alex/Developer/master-digital-sciences-dev/_modules"
  def path = "/Users/alex/Developer/modulhandbuecher_test/modules"

  def isValidYaml() = {
    val src = Paths.get(path)
    val files = Files.walk(src).toList.asScala.toVector.collect {
      case file if !Files.isDirectory(file) => file
    }
    files.foreach { f =>
      YamlParser
        .validateModuleYaml(GitFileContent(Files.readString(f)))
        .foreach(err => logger.error(s"${f.getFileName.toString}\n$err"))
    }
    logger.info("ok")
  }

  def migrate(): Future[Unit] = {
    val src = Paths.get(path)
    val files = Files.walk(src).toList.asScala.toVector.collect {
      case file if !Files.isDirectory(file) => file
    }
//    logger.info(parsePeople(files).toString())
//    logger.info(files.foldLeft(Set.empty[String]) { case (acc, f) => acc ++ keys(f) }.toString())

    val dest = Paths.get("tmp/dsi1")
    val po   = FullPoId("inf_dsi1")
    dest.deleteContentsOfDirectory()
    files.foreach { f =>
      val (json, md) = parseFile(f)
      createFiles(json, md, dest, po)
    }

    Future.unit
  }

  private def parseFile(file: Path): (Json, Option[String]) = {
    logger.info(s"parsing ${file.getFileName}")
    val content = Files.readString(file).split("---\n")
    content.size match
      case 2 =>
        val yaml = content(1)
        parse(yaml) match
          case Left(value) => throw value
          case Right(json) => (json, None)
      case 3 =>
        val yaml = content(1)
        val md   = content(2)
        parse(yaml) match
          case Left(value) => throw value
          case Right(json) => (json, Some(md))
      case _ =>
        throw Exception(s"invalid parsing ${content.toList}")
  }

  private def keys(file: Path): Set[String] = {
    val (json, _) = parseFile(file)
    json.hcursor.keys.get.toSet
  }

  private def parsePeople(files: Vector[Path]): Set[String] = {
    def go(file: Path) = {
      val (json, _) = parseFile(file)
      val mv        = json.\\("modulverantwortlich").map(_.asString.get)
      val dz        = json.\\("dozierende").flatMap(_.asArray.map(_.map(_.asString.get)).getOrElse(Vector.empty))
      mv.toSet.++(dz)
    }
    files.foldLeft(Set.empty[String]) { case (acc, f) => acc ++ go(f) }
  }

  private def createFiles(json: Json, md: Option[String], path: Path, fullPoId: FullPoId) = {
    val (id, coreYaml, companionYaml) = parseJsonDsi1(json)
    coreYaml.append("""
                      |## (de) Angestrebte Lernergebnisse:
                      |
                      |## (en) Learning Outcome:
                      |
                      |## (de) Modulinhalte:
                      |
                      |## (en) Module Content:
                      |
                      |## (de) Lehr- und Lernmethoden (Medienformen):
                      |
                      |## (en) Teaching and Learning Methods:
                      |
                      |## (de) Empfohlene Literatur:
                      |
                      |## (en) Recommended Reading:
                      |
                      |## (de) Besonderheiten:
                      |
                      |## (en) Particularities:
                      |---""".stripMargin)
    md.foreach(md => coreYaml.append(s"\n$md"))
    val coreFile = Files.createFile(path.resolve(s"$id.md"))
    Files.writeString(coreFile, coreYaml)
    val companionFile = Files.createFile(path.resolve(s"${id}_${fullPoId.id}.md"))
    Files.writeString(companionFile, companionYaml)
  }

  private def parseJsonDsi1(json: Json) = {
    val coreKeys      = mutable.HashMap.empty[String, String]
    val untouchedKeys = ListBuffer.empty[String]

    json.hcursor.keys.get.foreach { key =>
      val jss = json.\\(key)
      assume(jss.size == 1)
      val js = jss.head

      key match
        case "title" =>
          assume(js.isString)
          coreKeys += ("title" -> js.asString.get)
        case "acronym" =>
          assume(js.isString)
          coreKeys += ("abbreviation" -> js.asString.get)
        case "language" =>
          assume(js.isString)
          coreKeys += ("language" -> s"lang.${lookupLang(js.asString.get)}")
        case "semester" =>
          assume(js.isString)
          coreKeys += ("season" -> s"season.${lookupSeason(js.asString.get)}")
        case "location" =>
          assume(js.isString)
          coreKeys += ("location" -> s"location.${lookupLocation(js.asString.get)}")
        case "duration" =>
          assume(js.isNumber)
          coreKeys += ("duration" -> js.asNumber.get.toInt.get.toString)
        case "responsible" =>
          assume(js.isObject)
          val obj = js.asObject.get
          val mm  = obj.apply("module_management").get.asString.get
          coreKeys += ("module_management" -> s"person.${lookupPerson(mm)}")
          obj.apply("lecturers").foreach { js =>
            val value = js.asString.get.split(';').map(l => s"person.${lookupPerson(l)}").mkString(";")
            coreKeys += ("lecturers" -> value)
          }
        case "participants" =>
          assume(js.isObject)
          val obj = js.asObject.get
          coreKeys += ("participants_min" -> obj.apply("min").map(_.asNumber.get.toInt.get).getOrElse(0).toString)
          coreKeys += ("participants_max" -> obj.apply("max").get.asNumber.get.toInt.get.toString)
        case "ects" =>
          assume(js.isObject && !coreKeys.contains("ects"))
          val obj = js.asObject.get.apply("contributions_to_focus_areas").get.asObject.get
          val value = obj.values.foldLeft(0) {
            case (acc, n) => acc + n.asObject.get.apply("num").get.asNumber.map(_.toInt.get).getOrElse(0)
          }
          coreKeys += ("ects" -> value.toString)
          untouchedKeys.append(key) // keep the key
        case "is_thesis" =>
          assume(js.isObject && !coreKeys.contains("ects"))
          val obj = js.asObject.get
          val value = obj.apply("ects_thesis").get.asNumber.get.toInt.get + obj
            .apply("ects_colloquium")
            .get
            .asNumber
            .get
            .toInt
            .get
          coreKeys += ("ects" -> value.toString)
          untouchedKeys.append(key) // keep the key
        case "exam" =>
          assume(js.isString)
          val value = js.asString.get
          assume(value.nonEmpty)
          coreKeys += ("assessment" -> value)
          untouchedKeys.append(key) // keep the key
        case "precondition" | "recommendation" =>
          assume(js.isNull || js.isString)
          if !js.isNull then {
            val value = js.asString.get
            if value.nonEmpty then coreKeys += (key -> value)
          }
        case "effort" =>
          assume(js.isObject)
          val obj = js.asObject.get
          coreKeys += ("workload_lecture" -> obj.apply("lecture").map(_.asNumber.get.toInt.get).getOrElse(0).toString)
          coreKeys += ("workload_seminar" -> obj.apply("seminar").map(_.asNumber.get.toInt.get).getOrElse(0).toString)
          coreKeys += ("workload_practical" -> obj
            .apply("practical")
            .map(_.asNumber.get.toInt.get)
            .getOrElse(0)
            .toString)
          coreKeys += ("workload_exercise" -> obj.apply("exercise").map(_.asNumber.get.toInt.get).getOrElse(0).toString)
          coreKeys += ("workload_project_supervision" -> obj
            .apply("project_supervision")
            .map(_.asNumber.get.toInt.get)
            .getOrElse(0)
            .toString)
          coreKeys += ("workload_project_work" -> obj
            .apply("project_work")
            .map(_.asNumber.get.toInt.get)
            .getOrElse(0)
            .toString)
        case other =>
          untouchedKeys.append(other)
    }

    val companionYaml        = json.mapObject(_.filterKeys(untouchedKeys.contains)).asYaml.spaces2
    val companionTouchedYaml = json.mapObject(_.filterKeys(!untouchedKeys.contains(_))).asYaml.spaces2
    val (id, coreYaml)       = toYamlDsi1(coreKeys)
    assume(coreKeys.isEmpty, coreKeys.toString())
    (id, coreYaml, s"---\n$companionYaml---\n$companionTouchedYaml---")
  }

  private def parseJsonMim5(json: Json) = {
    def handleExaminer(value: String): String = {
      val values = value.split(',')
      if values.length > 1 then s"TODO: ${values.mkString(", ")}" else s"person.${lookupPerson(value)}"
    }

    val coreKeys      = mutable.HashMap.empty[String, String]
    val untouchedKeys = ListBuffer.empty[String]

    json.hcursor.keys.get.foreach { key =>
      val jss = json.\\(key)
      assume(jss.size == 1)
      val js = jss.head

      key match
        case "title" =>
          assume(js.isString)
          coreKeys += ("title" -> js.asString.get)
        case "kuerzel" =>
          assume(js.isString)
          coreKeys += ("abbreviation" -> js.asString.get)
        case "published" =>
          assume(js.isBoolean)
          val value = s"status.${if js.asBoolean.get then "active" else "inactive"}"
          coreKeys += ("status" -> value)
        case "modulverantwortlich" =>
          assume(js.isString)
          val values = js.asString.get.split(',')
          val value =
            if values.length > 1 then values.map(p => s"person.${lookupPerson(p.trim)}").mkString(";")
            else s"person.${lookupPerson(values.head)}"
          coreKeys += ("module_management" -> value)
        case "dozierende" =>
          assume(js.isArray)
          val value = js.asArray.get
            .map(j => s"person.${lookupPerson(j.asString.get)}")
            .mkString(";")
          coreKeys += ("lecturers" -> value)
        case "sprache" =>
          assume(js.isString)
          val value = s"lang.${lookupLang(js.asString.get).trim}"
          coreKeys += ("language" -> value)
        case "kreditpunkte" =>
          assume(js.isNumber)
          val value = js.asNumber.get.toDouble.toString
          coreKeys += ("ects" -> value)
        case "voraussetzungenNachPruefungsordnung" | "empfohleneVoraussetzungen" =>
          assume(js.isString || js.isNull, js.toString)
          if js.isString && js.asString.get.trim != "keine" then {
            val value = js.asString.get.trim
            if value.nonEmpty then {
              val coreKey = key match
                case "voraussetzungenNachPruefungsordnung" => "required_prerequisites"
                case "empfohleneVoraussetzungen"           => "recommended_prerequisites"
              coreKeys += (s"$coreKey;text" -> value)
            }
          }
        case "kategorie" =>
          assume(js.isString)
          js.asString.get.trim match
            case "schwerpunkt" =>
              coreKeys += ("po_optional;study_program;WAMO-SP" -> "study_program.inf_mi5") // each WAMO-SP can be a WAMO too. Since multiple instance_of connections are not supported yet, we keep just the main relation
            case "wahl"                  => coreKeys += ("po_optional;study_program;WAMO" -> "study_program.inf_mi5")
            case "pflicht" | "abschluss" => coreKeys += ("po_mandatory;study_program"     -> "study_program.inf_mi5")
        case "angebotImSs" =>
          assume(js.isBoolean || js.isNull, js.toString)
          val boolValue = if js.isNull then false else js.asBoolean.get
          val value     = s"season.${if boolValue then "ss" else "ws"}"
          coreKeys.updateWith("season") {
            case Some(s) =>
              if s.contains("season.ws") && value == "season.ss" then Some("season.ws_ss") else Some(value)
            case None => Some(value)
          }
        case "angebotImWs" =>
          assume(js.isBoolean || js.isNull, js.toString)
          val boolValue = if js.isNull then false else js.asBoolean.get
          val value     = s"season.${if boolValue then "ws" else "ss"}"
          coreKeys.updateWith("season") {
            case Some(s) =>
              if s.contains("season.ss") && value == "season.ws" then Some("season.ws_ss") else Some(value)
            case None => Some(value)
          }
        case "veranstaltungsform" =>
          assume(js.isNull, js.toString)
          coreKeys += ("location" -> "location.other")
        case "lehrmethoden" =>
          assume(js.isArray || js.isNull, js.toString)
          if !js.isNull && js.asArray.get.nonEmpty then {
            val value = js.asArray.get.map(_.asString.get.trim).mkString(";")
            coreKeys += ("teaching_and_learning_methods" -> value)
          }
        case "lehrformen" | "lehrform" =>
          assume(js.isArray || js.isNull, js.toString)
          if !js.isNull && js.asArray.get.nonEmpty then {
            val value = js.asArray.get.map(_.asString.get.trim).mkString(";")
            coreKeys += ("teaching_and_learning_methods_sws" -> value)
          }
        case "participants" =>
          assume(js.isObject || js.isNull)
          if !js.isNull then {
            val obj = js.asObject.get
            coreKeys += ("participants_min" -> obj.apply("min").get.asNumber.map(_.toInt.get.toString).getOrElse("0"))
            coreKeys += ("participants_max" -> obj.apply("max").get.asNumber.get.toInt.get.toString)
          }
        case "selbstStudium" => // consume
        case "praesenzZeit" =>
          assume(js.isNumber || js.isNull)
          if !js.isNull then {
            coreKeys += ("workload_lecture" -> js.asNumber.get.toInt.get.toString) // assumes lecture for a lack of better distinction
          }
        case "effort" =>
          assume(js.isObject)
          val obj  = js.asObject.get
          coreKeys += ("workload_lecture2"  -> obj.apply("lecture").get.asNumber.get.toInt.get.toString) // consider
          coreKeys += ("workload_seminar"   -> obj.apply("seminar").get.asNumber.get.toInt.get.toString)
          coreKeys += ("workload_practical" -> obj.apply("practical").get.asNumber.get.toInt.get.toString)
          coreKeys += ("workload_exercise"  -> obj.apply("exercise").get.asNumber.get.toInt.get.toString)
          coreKeys += ("workload_project_supervision" -> obj
            .apply("project_supervision")
            .get
            .asNumber
            .get
            .toInt
            .get
            .toString)
          coreKeys += ("workload_project_work" -> obj.apply("project_work").get.asNumber.get.toInt.get.toString)
        case "weitereStudiengaenge" =>
          assume(js.isObject || js.isArray || js.isNull, js.toString)
          if js.isObject then {
            val obj = js.asObject.get
            val value = obj.keys
              .filter(obj(_).get.asBoolean.get)
              .map(_.trim)
              .map {
                case "ds" => "study_program.inf_dsi1"
              }
              .mkString(";")
            coreKeys += ("po;study_program" -> value)
          }
          if js.isArray then {
            val value = js.asArray.get
              .map(_.asString.get.trim)
              .map {
                case "ds"                               => "study_program.inf_dsi1"
                case "Master Produktdesign"             => "study_program.ing_pdpd5"
                case "Master Wirtschaftsingenieurwesen" => "study_program.ing_wiwm2"
              }
              .mkString(";")
            coreKeys += ("po;study_program" -> value)
          }
        case "studienleistungen" =>
          assume(js.isObject, js.toString)
          val obj = js.asObject.get
          assume(obj.keys.size == 1)
          val methodJs = obj.apply("einzelleistung").get
          assume(methodJs.isObject)
          val methodObj = methodJs.asObject.get
          methodObj.keys.foreach { key =>
            val value = methodObj.apply(key).get
            key match
              case "pruefungsform" =>
                assume(value.isString, value.toString)
                val strValue = value.asString.get
                assume(strValue.nonEmpty)
                coreKeys += ("method" -> strValue)
              case "erstpruefer" =>
                assume(value.isString || value.isNull, value.toString)
                if !value.isNull then {
                  val strValue = value.asString.get.trim
                  coreKeys += ("first_examiner" -> handleExaminer(strValue)) // handle edge case
                }
              case "zweitpruefer" =>
                assume(value.isString || value.isNull, value.toString)
                if !value.isNull then {
                  val strValue = value.asString.get.trim
                  coreKeys += ("second_examiner" -> handleExaminer(strValue)) // handle edge case
                }
              case "datum" =>
                assume(value.isString || value.isNull, value.toString)
                if !value.isNull then {
                  val strValue = value.asString.get.trim
                  if strValue.nonEmpty then coreKeys += ("exam_phases" -> strValue)
                }
              case "artkey" | "art-key" =>
                assume(value.isString, value.toString)
                val strValue = value.asString.get.trim
                if strValue.nonEmpty then coreKeys += ("method;id" -> strValue)
          }
        case other =>
          untouchedKeys.append(other)
    }

    assume(!(coreKeys.contains("workload_lecture") && coreKeys.contains("workload_lecture2")))

    val companionYaml        = json.mapObject(_.filterKeys(untouchedKeys.contains)).asYaml.spaces2
    val companionTouchedYaml = json.mapObject(_.filterKeys(!untouchedKeys.contains(_))).asYaml.spaces2
    val (id, coreYaml)       = toYamlMim5(coreKeys)
    assume(coreKeys.isEmpty, coreKeys.toString())
    (id, coreYaml, s"---\n$companionYaml---\n$companionTouchedYaml---")
  }

  private def parseJsonMi5(json: Json) = {
    val coreKeys      = mutable.HashMap.empty[String, String]
    val untouchedKeys = ListBuffer.empty[String]

    json.hcursor.keys.get.foreach { key =>
      val jss = json.\\(key)
      assume(jss.size == 1)
      val js = jss.head

      key match
        case "title" =>
          assume(js.isString)
          coreKeys += ("title" -> js.asString.get)
        case "kuerzel" =>
          assume(js.isString)
          coreKeys += ("abbreviation" -> js.asString.get)
        case "modulverantwortlich" =>
          assume(js.isString)
          val values = js.asString.get.split(',')
          val value =
            if values.length > 1 then values.map(p => s"person.${lookupPerson(p.trim)}").mkString(";")
            else s"person.${lookupPerson(values.head)}"
          coreKeys += ("module_management" -> value)
        case "dozierende" =>
          assume(js.isArray)
          val value = js.asArray.get
            .map(j => s"person.${lookupPerson(j.asString.get)}")
            .mkString(";")
          coreKeys += ("lecturers" -> value)
        case "sprache" =>
          assume(js.isString)
          val value = s"lang.${lookupLang(js.asString.get)}"
          coreKeys += ("language" -> value)
        case "kreditpunkte" =>
          assume(js.isNumber)
          val value = js.asNumber.get.toDouble.toString
          coreKeys += ("ects" -> value)
        case "voraussetzungenNachPruefungsordnung" | "empfohleneVoraussetzungen" =>
          assume(js.isString || js.isNull, js.toString)
          if js.isString && js.asString.get.trim != "keine" then {
            val value = js.asString.get.trim
            if value.nonEmpty then {
              val coreKey = key match
                case "voraussetzungenNachPruefungsordnung" => "required_prerequisites"
                case "empfohleneVoraussetzungen"           => "recommended_prerequisites"
              coreKeys += (s"$coreKey;text" -> value)
            }
          }
        case "published" =>
          assume(js.isBoolean)
          val value = s"status.${if js.asBoolean.get then "active" else "inactive"}"
          coreKeys += ("status" -> value)
        case "typ" =>
          assume(js.isString)
          js.asString.get.trim match
            case "pm" | "tm" => coreKeys += ("po_mandatory;study_program" -> "study_program.inf_mi5")
            case "wpf"       => coreKeys += ("po_optional;study_program"  -> "study_program.inf_mi5")
        case "studiensemester" =>
          assume(js.isNumber, js.toString)
          coreKeys += ("po;recommended_semester" -> js.asNumber.get.toInt.get.toString)
        case "angebotImSs" =>
          assume(js.isBoolean, js.toString)
          val value = s"season.${if js.asBoolean.get then "ss" else "ws"}"
          coreKeys.updateWith("season") {
            case Some(s) =>
              if s.contains("season.ws") && value == "season.ss" then Some("season.ws_ss") else Some(value)
            case None => Some(value)
          }
        case "angebotImWs" =>
          assume(js.isBoolean, js.toString)
          val value = s"season.${if js.asBoolean.get then "ws" else "ss"}"
          coreKeys.updateWith("season") {
            case Some(s) =>
              if s.contains("season.ss") && value == "season.ws" then Some("season.ws_ss") else Some(value)
            case None => Some(value)
          }
        case "veranstaltungsform" =>
          assume(js.isString || js.isNull, js.toString)
          val value = if !js.isNull then s"location.${lookupLocation(js.asString.get.trim)}" else "location.other"
          coreKeys += ("location" -> value)
        case "lehrmethoden" =>
          assume(js.isArray || js.isNull, js.toString)
          if !js.isNull && js.asArray.get.nonEmpty then {
            val value = js.asArray.get.map(_.asString.get.trim).mkString(";")
            coreKeys += ("teaching_and_learning_methods" -> value)
          }
        case "lehrformen" | "lehrform" =>
          assume(js.isArray || js.isNull, js.toString)
          if !js.isNull && js.asArray.get.nonEmpty then {
            val value = js.asArray.get.map(_.asString.get.trim).mkString(";")
            coreKeys += ("teaching_and_learning_methods_sws" -> value)
          }
        case "besonderheiten" =>
          assume(js.isString || js.isArray || js.isNull, js.toString)
          if !js.isNull then {
            val value =
              if js.isArray then js.asArray.get.map(_.asString.get.trim).mkString(";") else js.asString.get.trim
            if value.nonEmpty then coreKeys += ("particularities" -> value)
          }
        case "selbstStudium" => // consume
        case "praesenzZeit" =>
          assume(js.isNumber)
          val value = js.asNumber.get.toInt.get.toString
          coreKeys += ("workload_lecture" -> value) // assumes lecture for a lack of better distinction
        case "weitereStudiengaenge" =>
          assume(js.isObject || js.isArray || js.isNull, js.toString)
          if js.isObject then {
            val obj = js.asObject.get
            val value = obj.keys
              .filter(obj(_).get.asBoolean.get)
              .map(_.trim)
              .filter(_ != "coco")
              .map {
                case "itm" => "study_program.inf_itm2"
                case "i"   => "study_program.inf_inf2"
                case "wi"  => "study_program.inf_wi5"
              }
              .mkString(";")
            coreKeys += ("po;study_program" -> value)
          }
          if js.isArray then {
            val value = js.asArray.get
              .map(_.asString.get.trim)
              .map {
                case "itm" => "study_program.inf_itm2"
                case "i"   => "study_program.inf_inf2"
                case "wi"  => "study_program.inf_wi5"
              }
              .mkString(";")
            coreKeys += ("po;study_program" -> value)
          }
        case "studienleistungen" =>
          assume(js.isObject, js.toString)
        case other =>
          untouchedKeys.append(other)
    }

    val companionYaml        = json.mapObject(_.filterKeys(untouchedKeys.contains)).asYaml.spaces2
    val companionTouchedYaml = json.mapObject(_.filterKeys(!untouchedKeys.contains(_))).asYaml.spaces2
    val (id, coreYaml)       = toYamlMi5(coreKeys)
    if coreKeys.nonEmpty then logger.error(coreKeys.toString())
    (id, coreYaml, s"---\n$companionYaml---\n$companionTouchedYaml---")
  }

  private def toYamlDsi1(map: mutable.Map[String, String]) = {
    def values(key: String, fallback: => String) = {
      map.remove(key) match
        case Some(value) =>
          val v = value.split(";")
          if v.size == 1 then s" ${v.head}" else v.map(s => s"\n${" ".repeat(4)}- $s").mkString("")
        case None => fallback
    }

    val yaml             = new StringBuilder()
    val id               = UUID.randomUUID()
    val moduleManagement = values("module_management", ???)
    yaml.append("---\n")
    yaml.append(s"id: $id\n")
    yaml.append(s"title: ${map.remove("title").get}\n")
    yaml.append(s"abbreviation: ${map.remove("abbreviation").get}\n")
    yaml.append("type: type.module\n")
    yaml.append(s"ects: ${map.remove("ects").getOrElse("TODO")}\n")
    yaml.append(s"language: ${map.remove("language").getOrElse("lang.de")}\n")
    yaml.append(s"duration: ${map.remove("duration").getOrElse("1")}\n")
    yaml.append(s"frequency: ${map.remove("season").get}\n")
    yaml.append(s"""responsibilities:
                   |  module_management:$moduleManagement
                   |  lecturers:${values("lecturers", moduleManagement)}\n""".stripMargin)
    yaml.append(s"""assessment_methods_mandatory:
                   |  - method: ${map.remove("assessment").getOrElse("TODO")}\n""".stripMargin)
    yaml.append(s"first_examiner: $moduleManagement\n")
    yaml.append("second_examiner: person.nn\n")
    yaml.append("exam_phases: exam_phase.none\n")
    yaml.append(s"""workload:
                   |  lecture: ${map.remove("workload_lecture").getOrElse("0")}
                   |  seminar: ${map.remove("workload_seminar").getOrElse("0")}
                   |  practical: ${map.remove("workload_practical").getOrElse("0")}
                   |  exercise: ${map.remove("workload_exercise").getOrElse("0")}
                   |  project_supervision: ${map.remove("workload_project_supervision").getOrElse("0")}
                   |  project_work: ${map.remove("workload_project_work").getOrElse("0")}\n""".stripMargin)
    map.remove("recommendation").foreach { v =>
      yaml.append(s"recommended_prerequisites:\n  text: $v\n")
    }
    map.remove("precondition").foreach { v =>
      yaml.append(s"required_prerequisites:\n  text: $v\n")
    }
    yaml.append("status: status.active\n")
    yaml.append(s"location: ${map.remove("location").getOrElse("location.other")}\n")
    yaml.append(s"""po_mandatory:
                   |  - study_program: study_program.inf_dsi1\n""".stripMargin)
    (map.remove("participants_min"), map.remove("participants_max")) match
      case (Some(min), Some(max)) => yaml.append(s"""participants:
                                                    |  min: $min
                                                    |  max: $max""".stripMargin)
      case (None, None) => // consume
      case other        => assume(false, s"invalid combination of participants: $other")
    yaml.append("\n---")

    assume(map.isEmpty, map.toString)

    (id, yaml)
  }

  private def toYamlMim5(map: mutable.Map[String, String]) = {
    def values(key: String, fallback: => String) = {
      map.remove(key) match
        case Some(value) =>
          val v = value.split(";")
          if v.size == 1 then s" ${v.head}" else v.map(s => s"\n${" ".repeat(4)}- $s").mkString("")
        case None => fallback
    }

    def assessmentMethod() = {
      val method = map.remove("method").get
      map.get("method;id") match
        case Some(value) =>
          map.remove("method;id")
          s"$method;;;$value"
        case None =>
          method
    }

    def pos(pos: String, instanceOf: Option[String]) = {
      val studyPrograms = new StringBuilder(s"\n  - study_program: $pos")
      instanceOf.foreach(i => studyPrograms.append(s"\n  - instance_of: $i"))
      map.remove("po;study_program").foreach { v =>
        if v.nonEmpty then {
          val arr = v.split(";")
          if arr.size == 1 then {
            studyPrograms.append(s"\n  - study_program: ${arr.head}")
            instanceOf.foreach(i => studyPrograms.append(s"\n  - instance_of: $i"))
          } else {
            arr.foreach { v =>
              if v.nonEmpty then {
                studyPrograms.append(s"\n  - study_program: $v")
                instanceOf.foreach(i => studyPrograms.append(s"\n  - instance_of: $i"))
              }
            }
          }
        }
      }
      map.remove("po;recommended_semester")
      studyPrograms.toString()
    }

    val yaml             = new StringBuilder()
    val id               = UUID.randomUUID()
    val moduleManagement = values("module_management", ???)
    yaml.append("---\n")
    yaml.append(s"id: $id\n")
    yaml.append(s"title: ${map.remove("title").get}\n")
    yaml.append(s"abbreviation: ${map.remove("abbreviation").get}\n")
    yaml.append("type: type.module\n")
    yaml.append(s"ects: ${map.remove("ects").get}\n")
    yaml.append(s"language: ${map.remove("language").getOrElse("lang.de")}\n")
    yaml.append("duration: 1\n")
    yaml.append(s"frequency: ${map.remove("season").get}\n")
    yaml.append(s"""responsibilities:
                   |  module_management:$moduleManagement
                   |  lecturers:${values("lecturers", moduleManagement)}\n""".stripMargin)
    yaml.append(s"""assessment_methods_mandatory:
                   |  - method: ${assessmentMethod()}\n""".stripMargin)
    yaml.append(s"first_examiner: ${map.remove("first_examiner").getOrElse("person.nn")}\n")
    yaml.append(s"second_examiner: ${map.remove("second_examiner").getOrElse("person.nn")}\n")
    yaml.append(s"exam_phases: ${map.remove("exam_phases").getOrElse("exam_phase.none")}\n")
    yaml.append(s"""workload:
                   |  lecture: ${map.remove("workload_lecture").orElse(map.remove("workload_lecture2")).getOrElse("0")}
                   |  seminar: ${map.remove("workload_seminar").getOrElse("0")}
                   |  practical: ${map.remove("workload_practical").getOrElse("0")}
                   |  exercise: ${map.remove("workload_exercise").getOrElse("0")}
                   |  project_supervision: ${map.remove("workload_project_supervision").getOrElse("0")}
                   |  project_work: ${map.remove("workload_project_work").getOrElse("0")}\n""".stripMargin)
    map.remove("recommended_prerequisites;text").foreach { v =>
      yaml.append(s"recommended_prerequisites:\n  text: $v\n")
    }
    map.remove("required_prerequisites;text").foreach { v =>
      yaml.append(s"required_prerequisites:\n  text: $v\n")
    }
    yaml.append(s"status: ${map.remove("status").get}\n")
    yaml.append(s"location: ${map.remove("location").getOrElse("location.other")}\n")
    map.remove("po_mandatory;study_program").foreach { v =>
      yaml.append(s"po_mandatory:${pos(v, None)}")
    }
    map.remove("po_optional;study_program;WAMO-SP").foreach { v =>
      yaml.append(s"po_optional:${pos(v, Some("WAMO-SP"))}")
    }
    map.remove("po_optional;study_program;WAMO").foreach { v =>
      yaml.append(s"po_optional:${pos(v, Some("WAMO"))}")
    }
    (map.remove("participants_min"), map.remove("participants_max")) match
      case (Some(min), Some(max)) => yaml.append(s"""participants
                                                    |  min: $min
                                                    |  max: $max""".stripMargin)
      case (None, None) => // consume
      case other        => assume(false, s"invalid combination of participants: $other")
    yaml.append("\n---")
    map.remove("teaching_and_learning_methods").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Lehr- und Lernmethoden (Medienformen)\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }
    map.remove("teaching_and_learning_methods_sws").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Lehr- und Lernmethoden (Medienformen)\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }
    map.remove("particularities").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Besonderheiten\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }

    assume(map.isEmpty, map.toString)

    (id, yaml)
  }

  private def toYamlMi5(map: mutable.Map[String, String]) = {
    def values(key: String, fallback: => String) = {
      map.remove(key) match
        case Some(value) =>
          val v = value.split(";")
          if v.size == 1 then s" ${v.head}" else v.map(s => s"\n${" ".repeat(4)}- $s").mkString("")
        case None => fallback
    }

    def appendSemester(sb: StringBuilder) =
      map.get("po;recommended_semester").foreach { v =>
        sb.append(s"\n${" ".repeat(4)}recommended_semester: $v")
      }

    def pos(pos: String) = {
      val studyPrograms = new StringBuilder(s"\n  - study_program: $pos")
      appendSemester(studyPrograms)
      map.remove("po;study_program").foreach { v =>
        if v.nonEmpty then {
          val arr = v.split(";")
          if arr.size == 1 then {
            studyPrograms.append(s"\n  - study_program: ${arr.head}")
            appendSemester(studyPrograms)
          } else {
            arr.foreach { v =>
              if v.nonEmpty then {
                studyPrograms.append(s"\n  - study_program: $v")
                appendSemester(studyPrograms)
              }
            }
          }
        }
      }
      map.remove("po;recommended_semester")
      studyPrograms.toString()
    }

    val yaml             = new StringBuilder()
    val id               = UUID.randomUUID()
    val moduleManagement = values("module_management", ???)
    yaml.append("---\n")
    yaml.append(s"id: $id\n")
    yaml.append(s"title: ${map.remove("title").get}\n")
    yaml.append(s"abbreviation: ${map.remove("abbreviation").get}\n")
    yaml.append("type: type.module\n")
    yaml.append(s"ects: ${map.remove("ects").get}\n")
    yaml.append(s"language: ${map.remove("language").getOrElse("lang.de")}\n")
    yaml.append("duration: 1\n")
    yaml.append(s"frequency: ${map.remove("season").get}\n")
    yaml.append(s"""responsibilities:
                   |  module_management:$moduleManagement
                   |  lecturers:${values("lecturers", moduleManagement)}\n""".stripMargin)
    yaml.append("assessment_methods_mandatory: TODO\n")
    yaml.append("first_examiner: TODO\n")
    yaml.append("second_examiner: TODO\n")
    yaml.append("exam_phases: TODO\n")
    yaml.append(s"""workload:
                   |  lecture: ${map.remove("workload_lecture").getOrElse("0")}
                   |  seminar: 0
                   |  practical: 0
                   |  exercise: 0
                   |  project_supervision: 0
                   |  project_work: 0\n""".stripMargin)
    map.remove("recommended_prerequisites;text").foreach { v =>
      yaml.append(s"recommended_prerequisites:\n  text: $v\n")
    }
    map.remove("required_prerequisites;text").foreach { v =>
      yaml.append(s"required_prerequisites:\n  text: $v\n")
    }
    yaml.append(s"status: ${map.remove("status").get}\n")
    yaml.append(s"location: ${map.remove("location").getOrElse("location.other")}\n")

    (map.remove("po_mandatory;study_program"), map.remove("po_optional;study_program")) match
      case (Some(mandatory), None) =>
        yaml.append(s"po_mandatory:${pos(mandatory)}")
      case (None, Some(optional)) =>
        yaml.append(s"po_optional:${pos(optional)}")
      case other => assume(false, s"invalid combination of pos: $other")
    yaml.append("\n---")
    map.remove("teaching_and_learning_methods").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Lehr- und Lernmethoden (Medienformen)\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }
    map.remove("teaching_and_learning_methods_sws").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Lehr- und Lernmethoden (Medienformen)\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }
    map.remove("particularities").collect {
      case v if v.nonEmpty =>
        yaml.append("\n\n## Besonderheiten\n")
        v.split(";").foreach(v => yaml.append(s"\n- $v"))
    }

    assume(map.isEmpty, map.toString)

    (id, yaml)
  }

//  private def update(key: String, values: String | Set[String]) = {
//    map.updateWith(key) {
//      case Some(cur) =>
//        val nxt = values match
//          case x: String       => cur + x
//          case xs: Set[String] => cur ++ xs
//        Some(nxt)
//      case None =>
//        val nxt = values match
//          case x: String       => Set(x)
//          case xs: Set[String] => xs
//        Some(nxt)
//    }
//  }
}
