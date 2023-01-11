package database.repo

import database._
import database.table.{PrerequisiteType, PrerequisitesTable, _}
import git.GitFilePath
import parsing.types._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import validator.{Metadata, ModuleRelation, PrerequisiteEntry}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait MetadataRepository {
  def exists(metadata: Metadata): Future[Boolean]
  def create(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[Metadata]
  def update(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[Metadata]
  def all(filter: Map[String, Seq[String]]): Future[Seq[MetadataOutput]]
  def allPreview(
      filter: Map[String, Seq[String]]
  ): Future[Seq[(UUID, String, String)]]
  def allIds(): Future[Seq[(UUID, String)]]
  def get(id: UUID): Future[MetadataOutput]
}

@Singleton
final class MetadataRepositoryImpl @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends MetadataRepository
    with HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[MetadataDbEntry, MetadataTable] {
  import profile.api._

  type PrerequisitesDbResult = (
      List[PrerequisitesDbEntry],
      List[PrerequisitesModuleDbEntry],
      List[PrerequisitesPODbEntry]
  )

  val tableQuery = TableQuery[MetadataTable]

  private val ectsFocusAreaContributionTable =
    TableQuery[ECTSFocusAreaContributionTable]

  private val moduleRelationTable =
    TableQuery[ModuleRelationTable]

  private val responsibilityTable = TableQuery[ResponsibilityTable]

  private val metadataAssessmentMethodTable =
    TableQuery[MetadataAssessmentMethodTable]

  private val metadataAssessmentMethodPreconditionTable =
    TableQuery[MetadataAssessmentMethodPreconditionTable]

  private val prerequisitesTable =
    TableQuery[PrerequisitesTable]

  private val prerequisitesModuleTable =
    TableQuery[PrerequisitesModuleTable]

  private val prerequisitesPOTable =
    TableQuery[PrerequisitesPOTable]

  private val poMandatoryTable =
    TableQuery[POMandatoryTable]

  private val poOptionalTable =
    TableQuery[POOptionalTable]

  private val metadataCompetenceTable =
    TableQuery[MetadataCompetenceTable]

  private val metadataGlobalCriteriaTable =
    TableQuery[MetadataGlobalCriteriaTable]

  private val metadataTaughtWithTable =
    TableQuery[MetadataTaughtWithTable]

  // Filter
  // TODO use this everywhere where all is used
  override protected val makeFilter
      : PartialFunction[(String, String), MetadataTable => Rep[Boolean]] = {
    case ("user", value) =>
      t =>
        responsibilityTable
          .filter(r => r.metadata === t.id && r.isPerson(value))
          .exists
  }

  private def deleteDependencies(metadata: Metadata) =
    for {
      _ <- metadataTaughtWithTable.filter(_.metadata === metadata.id).delete
      _ <- metadataGlobalCriteriaTable.filter(_.metadata === metadata.id).delete
      _ <- metadataCompetenceTable.filter(_.metadata === metadata.id).delete
      _ <- poOptionalTable.filter(_.metadata === metadata.id).delete
      _ <- poMandatoryTable.filter(_.metadata === metadata.id).delete
      prerequisitesQuery = prerequisitesTable.filter(_.metadata === metadata.id)
      _ <- prerequisitesPOTable
        .filter(
          _.prerequisites in prerequisitesQuery
            .map(_.id)
        )
        .delete
      _ <- prerequisitesModuleTable
        .filter(
          _.prerequisites in prerequisitesQuery
            .map(_.id)
        )
        .delete
      _ <- prerequisitesQuery.delete
      metadataAssessmentMethodQuery = metadataAssessmentMethodTable.filter(
        _.metadata === metadata.id
      )
      _ <- metadataAssessmentMethodPreconditionTable
        .filter(
          _.metadataAssessmentMethod in metadataAssessmentMethodQuery.map(_.id)
        )
        .delete
      _ <- metadataAssessmentMethodQuery.delete
      _ <- responsibilityTable.filter(_.metadata === metadata.id).delete
      _ <- moduleRelationTable.filter(_.module === metadata.id).delete
      _ <- ectsFocusAreaContributionTable
        .filter(_.metadata === metadata.id)
        .delete
    } yield metadata

  private def createDependencies(metadata: Metadata) = {
    val (methods, preconditions) = metadataAssessmentMethods(metadata)
    val (entries, prerequisitesModules, prerequisitesPOs) = prerequisites(
      metadata
    )
    val (poMandatory, poOptional) = pos(metadata)

    for {
      _ <- ectsFocusAreaContributionTable ++= contributionsToFocusAreas(
        metadata
      )
      _ <- moduleRelationTable ++= moduleRelations(metadata)
      _ <- responsibilityTable ++= responsibilities(metadata)
      _ <- metadataAssessmentMethodTable ++= methods
      _ <- metadataAssessmentMethodPreconditionTable ++= preconditions
      _ <- prerequisitesTable ++= entries
      _ <- prerequisitesModuleTable ++= prerequisitesModules
      _ <- prerequisitesPOTable ++= prerequisitesPOs
      _ <- poMandatoryTable ++= poMandatory
      _ <- poOptionalTable ++= poOptional
      _ <- metadataCompetenceTable ++= metadataCompetences(metadata)
      _ <- metadataGlobalCriteriaTable ++= metadataGlobalCriteria(metadata)
      _ <- metadataTaughtWithTable ++= metadataTaughtWith(metadata)
    } yield metadata
  }

  private def toDbEntry(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    MetadataDbEntry(
      metadata.id,
      path.value,
      timestamp,
      metadata.title,
      metadata.abbrev,
      metadata.kind.abbrev,
      metadata.ects.value,
      metadata.language.abbrev,
      metadata.duration,
      metadata.season.abbrev,
      metadata.workload,
      metadata.status.abbrev,
      metadata.location.abbrev,
      metadata.participants.map(_.min),
      metadata.participants.map(_.max)
    )

  private def metadataTaughtWith(
      metadata: Metadata
  ): List[MetadataTaughtWithDbEntry] =
    metadata.taughtWith.map(m => MetadataTaughtWithDbEntry(metadata.id, m.id))

  private def metadataGlobalCriteria(
      metadata: Metadata
  ): List[MetadataGlobalCriteriaDbEntry] =
    metadata.globalCriteria.map(gc =>
      MetadataGlobalCriteriaDbEntry(metadata.id, gc.abbrev)
    )

  private def metadataCompetences(
      metadata: Metadata
  ): List[MetadataCompetenceDbEntry] =
    metadata.competences.map(c =>
      MetadataCompetenceDbEntry(metadata.id, c.abbrev)
    )

  private def pos(
      metadata: Metadata
  ): (List[POMandatoryDbEntry], List[POOptionalDbEntry]) =
    (
      metadata.validPOs.mandatory.map(po =>
        POMandatoryDbEntry(
          metadata.id,
          po.po.abbrev,
          po.recommendedSemester,
          po.recommendedSemesterPartTime
        )
      ),
      metadata.validPOs.optional.map(po =>
        POOptionalDbEntry(
          metadata.id,
          po.po.abbrev,
          po.instanceOf.id,
          po.partOfCatalog,
          po.recommendedSemester
        )
      )
    )

  private def prerequisites(metadata: Metadata): PrerequisitesDbResult = {
    val entries = ListBuffer[PrerequisitesDbEntry]()
    val modules = ListBuffer[PrerequisitesModuleDbEntry]()
    val pos = ListBuffer[PrerequisitesPODbEntry]()

    def go(
        entry: PrerequisiteEntry,
        prerequisiteType: PrerequisiteType
    ) = {
      val prerequisites = PrerequisitesDbEntry(
        UUID.randomUUID,
        metadata.id,
        prerequisiteType,
        entry.text
      )
      entries += prerequisites
      modules ++= entry.modules.map(m =>
        PrerequisitesModuleDbEntry(prerequisites.id, m.id)
      )
      pos ++= entry.pos.map(po =>
        PrerequisitesPODbEntry(prerequisites.id, po.abbrev)
      )
    }

    metadata.prerequisites.required.foreach(
      go(_, PrerequisiteType.Required)
    )
    metadata.prerequisites.recommended.foreach(
      go(_, PrerequisiteType.Recommended)
    )

    (
      entries.toList,
      modules.toList,
      pos.toList
    )
  }

  private def moduleRelations(metadata: Metadata): List[ModuleRelationDbEntry] =
    metadata.relation.fold(List.empty[ModuleRelationDbEntry]) {
      case ModuleRelation.Parent(children) =>
        children.map(child =>
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Parent,
            child.id
          )
        )
      case ModuleRelation.Child(parent) =>
        List(
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Child,
            parent.id
          )
        )
    }

  private def contributionsToFocusAreas(
      metadata: Metadata
  ): List[ECTSFocusAreaContributionDbEntry] =
    metadata.ects.contributionsToFocusAreas.map(c =>
      ECTSFocusAreaContributionDbEntry(
        metadata.id,
        c.focusArea.abbrev,
        c.ectsValue,
        c.description
      )
    )

  private def responsibilities(
      metadata: Metadata
  ): List[ResponsibilityDbEntry] =
    metadata.responsibilities.lecturers.map(p =>
      ResponsibilityDbEntry(metadata.id, p.id, ResponsibilityType.Lecturer)
    ) :::
      metadata.responsibilities.moduleManagement.map(p =>
        ResponsibilityDbEntry(
          metadata.id,
          p.id,
          ResponsibilityType.ModuleManagement
        )
      )

  private def metadataAssessmentMethods(
      metadata: Metadata
  ): (
      List[MetadataAssessmentMethodDbEntry],
      List[MetadataAssessmentMethodPreconditionDbEntry]
  ) = {
    val metadataAssessmentMethods =
      ListBuffer[MetadataAssessmentMethodDbEntry]()
    val metadataAssessmentMethodPreconditions =
      ListBuffer[MetadataAssessmentMethodPreconditionDbEntry]()

    metadata.assessmentMethods.mandatory.foreach { m =>
      val metadataAssessmentMethod = MetadataAssessmentMethodDbEntry(
        UUID.randomUUID,
        metadata.id,
        m.method.abbrev,
        AssessmentMethodType.Mandatory,
        m.percentage
      )
      metadataAssessmentMethods += metadataAssessmentMethod
      m.precondition.foreach { m2 =>
        val precondition = MetadataAssessmentMethodPreconditionDbEntry(
          m2.abbrev,
          metadataAssessmentMethod.id
        )
        metadataAssessmentMethodPreconditions += precondition
      }
    }
    (
      metadataAssessmentMethods.toList,
      metadataAssessmentMethodPreconditions.toList
    )
  }

  override def exists(metadata: Metadata) =
    db.run(tableQuery.filter(_.id === metadata.id).result.map(_.nonEmpty))

  override def update(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    db.run(
      (
        for {
          _ <- deleteDependencies(metadata)
          _ <- tableQuery
            .filter(_.id === metadata.id)
            .update(toDbEntry(metadata, path, timestamp))
          _ <- createDependencies(metadata)
        } yield metadata
      ).transactionally
    )

  override def create(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    db.run(
      (
        for {
          _ <- tableQuery += toDbEntry(metadata, path, timestamp)
          _ <- createDependencies(metadata)
        } yield metadata
      ).transactionally
    )

  override def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  private def retrieve(
      query: Query[MetadataTable, MetadataDbEntry, Seq]
  ): Future[Seq[MetadataOutput]] = {
    val methods = metadataAssessmentMethodTable
      .joinLeft(metadataAssessmentMethodPreconditionTable)
      .on(_.id === _.metadataAssessmentMethod)

    val prerequisites = prerequisitesTable
      .joinLeft(prerequisitesModuleTable)
      .on(_.id === _.prerequisites)
      .joinLeft(prerequisitesPOTable)
      .on(_._1.id === _.prerequisites)

    db.run(
      query
        .joinLeft(moduleRelationTable)
        .on(_.id === _.module)
        .join(responsibilityTable)
        .on(_._1.id === _.metadata)
        .joinLeft(methods)
        .on(_._1._1.id === _._1.metadata)
        .joinLeft(prerequisites)
        .on(_._1._1._1.id === _._1._1.metadata)
        .joinLeft(poMandatoryTable)
        .on(_._1._1._1._1.id === _.metadata)
        .joinLeft(poOptionalTable)
        .on(_._1._1._1._1._1.id === _.metadata)
        .joinLeft(metadataCompetenceTable)
        .on(_._1._1._1._1._1._1.id === _.metadata)
        .joinLeft(metadataGlobalCriteriaTable)
        .on(_._1._1._1._1._1._1._1.id === _.metadata)
        .joinLeft(metadataTaughtWithTable)
        .on(_._1._1._1._1._1._1._1._1.id === _.metadata)
        .result
        .map(_.groupBy(_._1._1._1._1._1._1._1._1._1).map { case (m, deps) =>
          val participants = for {
            min <- m.participantsMin
            max <- m.participantsMax
          } yield Participants(min, max)
          val relations = mutable.HashSet[ModuleRelationDbEntry]()
          val moduleManagement = mutable.HashSet[String]()
          val lecturer = mutable.HashSet[String]()
          val mandatoryAssessmentMethods =
            mutable.HashSet[(UUID, AssessmentMethodEntryOutput)]()
          val optionalAssessmentMethods =
            mutable.HashSet[(UUID, AssessmentMethodEntryOutput)]()
          val preconditions =
            mutable.HashSet[MetadataAssessmentMethodPreconditionDbEntry]()
          var recommendedPrerequisite =
            Option.empty[(UUID, PrerequisiteEntryOutput)]
          var requiredPrerequisite =
            Option.empty[(UUID, PrerequisiteEntryOutput)]
          val prerequisitesModules =
            mutable.HashSet[PrerequisitesModuleDbEntry]()
          val prerequisitesPOS = mutable.HashSet[PrerequisitesPODbEntry]()
          val poMandatory = mutable.HashSet[POMandatoryOutput]()
          val poOptional = mutable.HashSet[POOptionalOutput]()
          val competences = mutable.HashSet[String]()
          val globalCriteria = mutable.HashSet[String]()
          val taughtWith = mutable.HashSet[UUID]()

          deps.foreach {
            case (((((((((_, mr), r), am), p), poM), poO), c), gc), tw) =>
              tw.foreach(taughtWith += _.module)
              gc.foreach(globalCriteria += _.globalCriteria)
              c.foreach(competences += _.competence)
              poM.foreach(po =>
                poMandatory += POMandatoryOutput(
                  po.po,
                  po.recommendedSemester,
                  po.recommendedPartTimeSemester
                )
              )
              poO.foreach(po =>
                poOptional += POOptionalOutput(
                  po.po,
                  po.instanceOf,
                  po.partOfCatalog,
                  po.recommendedSemester
                )
              )
              p.foreach { case ((e, m), po) =>
                val prerequisite =
                  Some(e.id -> PrerequisiteEntryOutput(e.text, Nil, Nil))
                e.prerequisitesType match {
                  case PrerequisiteType.Required =>
                    requiredPrerequisite = prerequisite
                  case PrerequisiteType.Recommended =>
                    recommendedPrerequisite = prerequisite
                }
                m.foreach(prerequisitesModules += _)
                po.foreach(prerequisitesPOS += _)
              }
              mr.foreach(relations += _)
              r.responsibilityType match {
                case ResponsibilityType.ModuleManagement =>
                  moduleManagement += r.person
                case ResponsibilityType.Lecturer => lecturer += r.person
              }
              am.foreach {
                case (am, amp) =>
                  amp.foreach(p =>
                    preconditions += MetadataAssessmentMethodPreconditionDbEntry(
                      p.assessmentMethod,
                      p.metadataAssessmentMethod
                    )
                  )
                  am.assessmentMethodType match {
                    case AssessmentMethodType.Mandatory =>
                      mandatoryAssessmentMethods +=
                        am.id ->
                          AssessmentMethodEntryOutput(
                            am.assessmentMethod,
                            am.percentage,
                            Nil
                          )

                    case AssessmentMethodType.Optional =>
                      optionalAssessmentMethods +=
                        am.id ->
                          AssessmentMethodEntryOutput(
                            am.assessmentMethod,
                            am.percentage,
                            Nil
                          )
                  }
              }
          }

          val relation = relations.headOption.map { r =>
            r.relationType match {
              case ModuleRelationType.Parent =>
                ModuleRelationOutput
                  .Parent(relations.map(_.module).toList)
              case ModuleRelationType.Child =>
                ModuleRelationOutput.Child(r.module)
            }
          }

          MetadataOutput(
            m.id,
            m.gitPath,
            m.title,
            m.abbrev,
            m.moduleType,
            m.ects,
            m.language,
            m.duration,
            m.season,
            m.workload,
            m.status,
            m.location,
            participants,
            relation,
            moduleManagement.toList,
            lecturer.toList,
            AssessmentMethodsOutput(
              mandatoryAssessmentMethods
                .map(a =>
                  a._2.copy(precondition =
                    preconditions
                      .filter(_.metadataAssessmentMethod == a._1)
                      .map(_.assessmentMethod)
                      .toList
                  )
                )
                .toList,
              optionalAssessmentMethods
                .map(a =>
                  a._2.copy(precondition =
                    preconditions
                      .filter(_.metadataAssessmentMethod == a._1)
                      .map(_.assessmentMethod)
                      .toList
                  )
                )
                .toList
            ),
            PrerequisitesOutput(
              recommendedPrerequisite.map(a =>
                a._2.copy(
                  modules = prerequisitesModules
                    .filter(_.prerequisites == a._1)
                    .map(_.module)
                    .toList,
                  pos = prerequisitesPOS
                    .filter(_.prerequisites == a._1)
                    .map(_.po)
                    .toList
                )
              ),
              requiredPrerequisite.map(a =>
                a._2.copy(
                  modules = prerequisitesModules
                    .filter(_.prerequisites == a._1)
                    .map(_.module)
                    .toList,
                  pos = prerequisitesPOS
                    .filter(_.prerequisites == a._1)
                    .map(_.po)
                    .toList
                )
              )
            ),
            POOutput(
              poMandatory.toList,
              poOptional.toList
            ),
            competences.toList,
            globalCriteria.toList,
            taughtWith.toList
          )
        }.toSeq)
    )
  }

  override def allIds() =
    db.run(
      tableQuery
        .map(m => (m.id, m.abbrev))
        .result
    )

  def allPreview() =
    db.run(
      tableQuery
        .map(m => (m.id, m.title, m.abbrev))
        .result
    )

  override def allPreview(filter: Map[String, Seq[String]]) =
    db.run(
      allWithFilter(filter)
        .map(m => (m.id, m.title, m.abbrev))
        .result
    )

  override def get(id: UUID) =
    retrieve(tableQuery.filter(_.id === id))
      .flatMap(xs =>
        if (xs.size > 1)
          Future.failed(new Throwable(s"expected one element, but found: $xs"))
        else
          Future.successful(xs.head)
      )
}
