package parsing

import parser.Parser
import parsing.metadata.MetadataCompositeParser
import parsing.types.ModuleCompendium
import service._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCompendiumParser @Inject() (
    private val metadataParserService: MetadataParserService,
    private implicit val ctx: ExecutionContext
) {
  def parser(): Future[Parser[ModuleCompendium]] =
    Future.failed(new Throwable("TODO"))
  /*    for {
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
      .map(a => ModuleCompendium(???, a._2._1, a._2._2)) // TODO*/
}
