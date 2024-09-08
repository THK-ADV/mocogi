package parsing

import models.{
  MetadataProtocol,
  ModuleProtocol,
  ModuleRelationProtocol,
  ModuleWorkload
}
import parser.Parser
import parser.Parser.{prefix, zeroOrMoreSpaces}
import parser.ParserOps.{
  P0,
  P10,
  P11,
  P12,
  P13,
  P14,
  P15,
  P16,
  P2,
  P3,
  P4,
  P5,
  P6,
  P7,
  P8,
  P9
}
import parsing.metadata.THKV1Parser.{
  abbreviationParser,
  durationParser,
  idParser,
  titleParser
}
import parsing.metadata._
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
