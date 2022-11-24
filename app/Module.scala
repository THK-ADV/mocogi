import com.google.inject.{AbstractModule, TypeLiteral}
import database.repo.{MetadataRepository, MetadataRepositoryImpl}
import git.publisher.{
  CoreDataPublisher,
  GitFilesDownloadActor,
  ModuleCompendiumPublisher
}
import git.{
  GitConfig,
  GitFilesBroker,
  GitFilesBrokerImpl,
  ModuleCompendiumSubscribers
}
import parsing.metadata.MetadataParser
import printing.MarkdownConverter
import providers._
import publisher.KafkaPublisher
import service._
import validator.Metadata

class Module() extends AbstractModule {

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
    bind(classOf[PersonService])
      .to(classOf[PersonServiceImpl])
      .asEagerSingleton()
    bind(classOf[StudyFormTypeService])
      .to(classOf[StudyFormTypeServiceImpl])
      .asEagerSingleton()
    bind(classOf[GradeService])
      .to(classOf[GradeServiceImpl])
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
    bind(classOf[MetadataRepository])
      .to(classOf[MetadataRepositoryImpl])
      .asEagerSingleton()
    bind(classOf[MetadataService])
      .to(classOf[MetadataServiceImpl])
      .asEagerSingleton()
    bind(classOf[MetadataParserService])
      .to(classOf[MetadataParserServiceImpl])
      .asEagerSingleton()
    bind(classOf[CompetenceService])
      .to(classOf[CompetenceServiceImpl])
      .asEagerSingleton()
    bind(classOf[MetadataParsingValidator])
      .to(classOf[MetadataParsingValidatorImpl])
      .asEagerSingleton()
    bind(classOf[MetadataValidatorService])
      .to(classOf[MetadataValidatorServiceImpl])
      .asEagerSingleton()
    bind(classOf[GitFilesBroker])
      .to(classOf[GitFilesBrokerImpl])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumParsingValidator])
      .to(classOf[ModuleCompendiumParsingValidatorImpl])
      .asEagerSingleton()

    bind(classOf[MarkdownConverter])
      .toProvider(classOf[MarkdownConverterProvider])
      .asEagerSingleton()
    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumSubscribers])
      .toProvider(classOf[ModuleCompendiumSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[GitFilesDownloadActor])
      .toProvider(classOf[GitFilesDownloadActorProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumPublisher])
      .toProvider(classOf[ModuleCompendiumPublisherProvider])
      .asEagerSingleton()
    bind(classOf[CoreDataPublisher])
      .toProvider(classOf[CoreDataParsingValidatorProvider])
      .asEagerSingleton()

    bind(new TypeLiteral[Set[MetadataParser]] {})
      .toProvider(classOf[MetadataParserProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[KafkaPublisher[Metadata]] {})
      .toProvider(classOf[KafkaPublisherProvider])
      .asEagerSingleton()
  }
}
