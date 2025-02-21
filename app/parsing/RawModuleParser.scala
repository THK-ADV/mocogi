package parsing

import java.util.UUID

import io.circe.yaml.parser.parse
import io.circe.JsonObject
import models.*
import parser.Parser
import parser.Parser.prefix
import parser.Parser.prefixUntil
import parser.Parser.zeroOrMoreSpaces
import parser.ParserOps.P0
import parser.ParserOps.P10
import parser.ParserOps.P11
import parser.ParserOps.P12
import parser.ParserOps.P13
import parser.ParserOps.P14
import parser.ParserOps.P15
import parser.ParserOps.P2
import parser.ParserOps.P3
import parser.ParserOps.P4
import parser.ParserOps.P5
import parser.ParserOps.P6
import parser.ParserOps.P7
import parser.ParserOps.P8
import parser.ParserOps.P9
import parser.ParsingError
import parsing.metadata.*
import parsing.metadata.THKV1Parser.abbreviationParser
import parsing.metadata.THKV1Parser.durationParser
import parsing.metadata.THKV1Parser.idParser
import parsing.metadata.THKV1Parser.titleParser
import parsing.types.ParsedModuleRelation
import parsing.types.ParsedWorkload
import service.ContentParsingService
import service.MetadataValidatingService

object RawModuleParser {

  // TODO this function uses the new way of parsing yaml
  def parseCreatedModuleInformation(input: String): CreatedModule = {
    def parseModuleManagement(obj: JsonObject) = {
      def parseIdentity(str: String) =
        str.stripPrefix(IdentityParser.prefix)
      val js = obj.apply(ModuleResponsibilitiesParser.key).get
      assume(js.isObject)
      val managementJs = js.asObject.get.apply(ModuleResponsibilitiesParser.moduleManagementKey).get
      assume(managementJs.isString || managementJs.isArray)
      if managementJs.isString then {
        val id = parseIdentity(managementJs.asString.get)
        List(id)
      } else {
        val ids = managementJs.asArray.get.map(a => parseIdentity(a.asString.get))
        ids.toList
      }
    }

    def parseId(obj: JsonObject) = {
      val js = obj.apply(THKV1Parser.idKey).get
      assume(js.isString, s"expected id to be a string, but was: ${js.toString}")
      UUID.fromString(js.asString.get)
    }

    def parseTitle(obj: JsonObject) = {
      val js = obj.apply(THKV1Parser.titleKey).get
      assume(js.isString, s"expected title to be a string, but was: ${js.toString}")
      js.asString.get
    }

    def parseAbbrev(obj: JsonObject) = {
      val js = obj.apply(THKV1Parser.abbreviationKey).get
      assume(js.isString, s"expected abbreviation to be a string, but was: ${js.toString}")
      js.asString.get
    }

    def parseECTS(obj: JsonObject) = {
      val js = obj.apply(ModuleECTSParser.key).get
      // TODO Remove this check when ECTS will become numbers only
      assume(js.isNumber || js.isObject, s"expected ects to be a number, but was: ${js.toString}")
      if js.isNumber then js.asNumber.get.toDouble else 1.0
    }

    def parseModuleType(obj: JsonObject) = {
      val js = obj.apply(ModuleTypeParser.key).get
      assume(js.isString, s"expected module type to be a string, but was: ${js.toString}")
      js.asString.get.stripPrefix(ModuleTypeParser.prefix)
    }

    def parseMandatoryPOs(obj: JsonObject) = {
      var key = ModulePOParser.modulePOMandatoryKey
      if key.last == ':' then {
        key = key.dropRight(1)
      }
      obj.apply(key) match
        case Some(js) =>
          assume(js.isArray, s"expected po mandatory to be an array, but was: ${js.toString}")
          js.asArray.get
            .map(
              _.asObject.get
                .apply(ModulePOParser.studyProgramKey)
                .get
                .asString
                .get
                .stripPrefix(ModulePOParser.studyProgramPrefix)
            )
            .toList
        case None => Nil
    }

    def parseOptionalPOs(obj: JsonObject) = {
      var key = ModulePOParser.modulePOElectiveKey
      if key.last == ':' then {
        key = key.dropRight(1)
      }
      obj.apply(key) match
        case Some(js) =>
          assume(js.isArray, s"expected po optional to be an array, but was: ${js.toString}")
          js.asArray.get
            .map(
              _.asObject.get
                .apply(ModulePOParser.studyProgramKey)
                .get
                .asString
                .get
                .stripPrefix(ModulePOParser.studyProgramPrefix)
            )
            .toList
        case None => Nil
    }

    val res = prefix("---")
      .skip(VersionSchemeParser.parser)
      .skip(zeroOrMoreSpaces)
      .take(prefixUntil("---"))
      .parse(input)
      ._1

    res match
      case Left(value) => throw value
      case Right(yaml) =>
        parse(yaml) match
          case Left(value) => throw value
          case Right(js) =>
            assume(js.isObject)
            val obj = js.asObject.get
            CreatedModule(
              parseId(obj),
              parseTitle(obj),
              parseAbbrev(obj),
              parseModuleManagement(obj),
              parseECTS(obj),
              parseModuleType(obj),
              parseMandatoryPOs(obj),
              parseOptionalPOs(obj),
            )
  }

  def metadataParser: Parser[(UUID, MetadataProtocol)] =
    prefix("---")
      .skip(VersionSchemeParser.parser)
      .skip(zeroOrMoreSpaces)
      .take(
        idParser.zip(titleParser).take(abbreviationParser)
      )
      .zip(ModuleTypeParser.raw)
      .take(ModuleRelationParser.parser.map(_.map(toModuleRelation)))
      .take(ModuleECTSParser.raw)
      .skip(zeroOrMoreSpaces)
      .take(ModuleLanguageParser.raw)
      .take(durationParser)
      .skip(zeroOrMoreSpaces)
      .take(ModuleSeasonParser.raw)
      .take(ModuleResponsibilitiesParser.raw)
      .take(ModuleAssessmentMethodParser.raw)
      .skip(zeroOrMoreSpaces)
      .take(
        ExaminerParser.raw.skip(zeroOrMoreSpaces).zip(ExamPhaseParser.raw)
      )
      .skip(zeroOrMoreSpaces)
      .take(ModuleWorkloadParser.parser)
      .skip(zeroOrMoreSpaces)
      .take(ModulePrerequisitesParser.raw)
      .take(ModuleStatusParser.raw)
      .take(ModuleLocationParser.raw)
      .skip(zeroOrMoreSpaces)
      .take(ModulePOParser.raw)
      .take(
        ModuleParticipantsParser.parser.option
          .skip(zeroOrMoreSpaces)
          .zip(ModuleCompetencesParser.raw.option.map(_.getOrElse(Nil)))
          .skip(zeroOrMoreSpaces)
          .take(ModuleGlobalCriteriaParser.raw.option.map(_.getOrElse(Nil)))
          .skip(zeroOrMoreSpaces)
          .take(ModuleTaughtWithParser.parser.option.map(_.getOrElse(Nil)))
          .skip(zeroOrMoreSpaces)
      )
      .skip(prefix("---"))
      .map {
        case (
              (id, title, abbrev),
              moduleType,
              relation,
              credits,
              lang,
              dur,
              season,
              (management, lectures),
              assessments,
              (examiner, examPhases),
              workload,
              prerequisites,
              status,
              location,
              pos,
              (parts, competences, criteria, taughtWiths),
            ) =>
          (
            id,
            MetadataProtocol(
              title,
              abbrev,
              moduleType,
              credits,
              lang,
              dur,
              season,
              toModuleWorkload(workload, credits),
              status,
              location,
              parts,
              relation,
              management,
              lectures,
              assessments,
              examiner,
              examPhases,
              prerequisites,
              pos,
              competences,
              criteria,
              taughtWiths
            )
          )
      }

  def parser: Parser[ModuleProtocol] =
    metadataParser
      .zip(ContentParsingService.parser)
      .map {
        case ((id, metadata), (deContent, enContent)) =>
          ModuleProtocol(
            Some(id),
            metadata,
            deContent,
            enContent
          )
      }

  private def toModuleWorkload(workload: ParsedWorkload, ects: Double) =
    MetadataValidatingService.validateWorkload(workload, ects) match
      case Left(value) =>
        throw Exception(s"unexpected exception. can't validate workload $workload. Errors: ${value.mkString(", ")}")
      case Right(workload) =>
        workload

  private def toModuleRelation(
      mr: ParsedModuleRelation
  ): ModuleRelationProtocol =
    mr match {
      case ParsedModuleRelation.Parent(children) =>
        ModuleRelationProtocol.Parent(children)
      case ParsedModuleRelation.Child(parent) =>
        ModuleRelationProtocol.Child(parent)
    }
}
