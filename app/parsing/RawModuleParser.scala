package parsing

import models.MetadataProtocol
import models.ModuleProtocol
import models.ModuleRelationProtocol
import models.ModuleWorkload
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
import parser.ParserOps.P16
import parser.ParserOps.P2
import parser.ParserOps.P3
import parser.ParserOps.P4
import parser.ParserOps.P5
import parser.ParserOps.P6
import parser.ParserOps.P7
import parser.ParserOps.P8
import parser.ParserOps.P9
import parsing.metadata._
import parsing.metadata.THKV1Parser.abbreviationParser
import parsing.metadata.THKV1Parser.durationParser
import parsing.metadata.THKV1Parser.idParser
import parsing.metadata.THKV1Parser.titleParser
import parsing.types.ParsedModuleRelation
import service.ContentParsingService

object RawModuleParser {
  def parser: Parser[ModuleProtocol] =
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
      .take(ContentParsingService.parser)
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
              (deContent, enContent)
            ) =>
          ModuleProtocol(
            Some(id),
            MetadataProtocol(
              title,
              abbrev,
              moduleType,
              credits,
              lang,
              dur,
              season,
              ModuleWorkload.fromParsed(workload),
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
            ),
            deContent,
            enContent
          )
      }

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
