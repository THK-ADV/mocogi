import auth.{Authorization, UserToken}
import catalog.{ModuleCatalogLatexActor, WPFCatalogueGeneratorActor}
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, TypeLiteral}
import database.repo.{ModuleDraftRepository, ModuleDraftRepositoryImpl}
import git.GitConfig
import git.publisher.{CoreDataPublisher, ModulePublisher}
import git.subscriber.ModuleSubscribers
import models.ModuleKeysToReview
import ops.ConfigurationOps.Ops
import parsing.metadata.MetadataParser
import play.api.{Configuration, Environment}
import printing.markdown.ModuleMarkdownPrinter
import printing.pandoc.PandocApi
import printing.yaml.MetadataYamlPrinter
import providers._
import publisher.KafkaPublisher
import service._
import service.core._
import validator.Metadata
import webhook.GitPushEventHandler

import scala.annotation.unused

class Module(@unused environment: Environment, configuration: Configuration)
    extends AbstractModule {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[LocationService])
      .to(classOf[LocationServiceImpl])
      .asEagerSingleton()
    bind(classOf[LanguageService])
      .to(classOf[LanguageServiceImpl])
      .asEagerSingleton()
    bind(classOf[StatusService])
      .to(classOf[StatusServiceImpl])
      .asEagerSingleton()
    bind(classOf[AssessmentMethodService])
      .to(classOf[AssessmentMethodServiceImpl])
      .asEagerSingleton()
    bind(classOf[ModuleTypeService])
      .to(classOf[ModuleTypeServiceImpl])
      .asEagerSingleton()
    bind(classOf[SeasonService])
      .to(classOf[SeasonServiceImpl])
      .asEagerSingleton()
    bind(classOf[IdentityService])
      .to(classOf[IdentityServiceImpl])
      .asEagerSingleton()
    bind(classOf[DegreeService])
      .to(classOf[DegreeServiceImpl])
      .asEagerSingleton()
    bind(classOf[FacultyService])
      .to(classOf[FacultyServiceImpl])
      .asEagerSingleton()
    bind(classOf[GlobalCriteriaService])
      .to(classOf[GlobalCriteriaServiceImpl])
      .asEagerSingleton()
    bind(classOf[StudyProgramService])
      .to(classOf[StudyProgramServiceImpl])
      .asEagerSingleton()
    bind(classOf[POService])
      .to(classOf[POServiceImpl])
      .asEagerSingleton()
    bind(classOf[FocusAreaService])
      .to(classOf[FocusAreaServiceImpl])
      .asEagerSingleton()
    bind(classOf[ModuleService])
      .to(classOf[ModuleServiceImpl])
      .asEagerSingleton()
    bind(classOf[MetadataParsingService])
      .to(classOf[MetadataParsingServiceImpl])
      .asEagerSingleton()
    bind(classOf[CompetenceService])
      .to(classOf[CompetenceServiceImpl])
      .asEagerSingleton()
    bind(classOf[SpecializationService])
      .to(classOf[SpecializationServiceImpl])
      .asEagerSingleton()
    bind(classOf[ModuleDraftService])
      .to(classOf[ModuleDraftServiceImpl])
      .asEagerSingleton()
    bind(classOf[ModuleDraftRepository])
      .to(classOf[ModuleDraftRepositoryImpl])
      .asEagerSingleton()

    bind(classOf[PandocApi])
      .toProvider(classOf[MarkdownConverterProvider])
      .asEagerSingleton()
    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleSubscribers])
      .toProvider(classOf[ModuleSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[GitPushEventHandler])
      .toProvider(classOf[GitMergeEventHandlingActorProvider])
      .asEagerSingleton()
    bind(classOf[CoreDataPublisher])
      .toProvider(classOf[CoreDataPublisherProvider])
      .asEagerSingleton()
    bind(classOf[ModulePublisher])
      .toProvider(classOf[ModulePublisherProvider])
      .asEagerSingleton()
    bind(classOf[ModuleKeysToReview])
      .toProvider(classOf[ModuleKeysToReviewProvider])
      .asEagerSingleton()
    bind(classOf[ModuleKeyService])
      .toProvider(classOf[ModuleKeyServiceProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCatalogLatexActor])
      .toProvider(classOf[ModuleCatalogLatexActorProvider])
      .asEagerSingleton()
    bind(classOf[WPFCatalogueGeneratorActor])
      .toProvider(classOf[WPFCatalogueGeneratorActorProvider])
      .asEagerSingleton()

    bind(new TypeLiteral[Set[MetadataParser]] {})
      .toProvider(classOf[MetadataParserProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[KafkaPublisher[Metadata]] {})
      .toProvider(classOf[KafkaPublisherProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Authorization[UserToken]] {})
      .toProvider(classOf[AuthorizationProvider])
      .asEagerSingleton()

    bind(classOf[MetadataYamlPrinter]).toInstance(
      new MetadataYamlPrinter(2)
    )
    bind(classOf[ModuleMarkdownPrinter]).toInstance(
      new ModuleMarkdownPrinter(true)
    )

    // TODO use more constant bindings
    bind(classOf[String])
      .annotatedWith(Names.named("gitHost"))
      .toInstance(configuration.nonEmptyString("git.host"))
  }
}
