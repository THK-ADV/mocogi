package parsing

import parser.Parser
import parser.Parser.newline
import parsing.content.ContentParser.contentParser
import parsing.metadata.MetadataCompositeParser
import parsing.types.ModuleCompendium
import service._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCompendiumParser @Inject() (
    private val metadataParser: MetadataCompositeParser,
    private val locationService: LocationService,
    private val languageService: LanguageService,
    private val statusService: StatusService,
    private val assessmentMethodService: AssessmentMethodService,
    private val moduleTypeService: ModuleTypeService,
    private val seasonService: SeasonService,
    private val personService: PersonService,
    private implicit val ctx: ExecutionContext
) {
  def parser(): Future[Parser[ModuleCompendium]] = {
    for {
      locations <- locationService.all()
      languages <- languageService.all()
      status <- statusService.all()
      assessmentMethods <- assessmentMethodService.all()
      moduleTypes <- moduleTypeService.all()
      seasons <- seasonService.all()
      persons <- personService.all()
    } yield metadataParser
      .parser(
        locations,
        languages,
        status,
        assessmentMethods,
        moduleTypes,
        seasons,
        persons,
        Nil, // TODO
        Nil,
        Nil,
        Nil
      )
      .skip(newline.many())
      .zip(contentParser)
      .map(a => ModuleCompendium(a._1, a._2._1, a._2._2))
  }
}
