package parsing

import java.util.UUID

import models.*
import parser.Parser
import parser.Parser.prefix
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
import parsing.content.ModuleContentParser
import parsing.metadata.*
import parsing.metadata.THKV1Parser.abbreviationParser
import parsing.metadata.THKV1Parser.durationParser
import parsing.metadata.THKV1Parser.idParser
import parsing.metadata.THKV1Parser.titleParser
import parsing.types.ParsedModuleRelation

object RawModuleParser {

  def parseCreatedModuleInformation(input: String): CreatedModule = {
    val res = metadataParser.parse(input)._1
    res match {
      case Left(err) => throw err
      case Right((id, p)) =>
        CreatedModule(
          id,
          p.title,
          p.abbrev,
          p.moduleManagement.toList,
          p.ects,
          p.moduleType,
          p.po.mandatory.map(_.fullPo),
          p.po.optional.map(_.fullPo)
        )
    }
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
          .zip(ModuleTaughtWithParser.parser.option.map(_.getOrElse(Nil)))
          .skip(zeroOrMoreSpaces)
          .take(AttendanceRequirementParser.parser.option)
          .skip(zeroOrMoreSpaces)
          .take(AssessmentPrerequisiteParser.parser.option)
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
              (parts, taughtWiths, attReq, assPre),
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
              workload,
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
              taughtWiths,
              attReq,
              assPre
            )
          )
      }

  def parser: Parser[ModuleProtocol] =
    metadataParser
      .zip(ModuleContentParser.parser)
      .map {
        case ((id, metadata), (deContent, enContent)) =>
          ModuleProtocol(
            Some(id),
            metadata,
            deContent,
            enContent
          )
      }

  private def toModuleRelation(mr: ParsedModuleRelation): ModuleRelationProtocol =
    mr match {
      case ParsedModuleRelation.Parent(children) =>
        ModuleRelationProtocol.Parent(children)
      case ParsedModuleRelation.Child(parent) =>
        ModuleRelationProtocol.Child(parent)
    }
}
