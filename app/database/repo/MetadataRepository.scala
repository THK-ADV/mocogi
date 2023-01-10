package database.repo

import database.table.{PrerequisiteType, PrerequisitesTable, _}
import git.GitFilePath
import parsing.types._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import validator.{Metadata, Module, ModuleRelation, PrerequisiteEntry, Workload}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

case class POMandatoryOutput(
    po: String,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)

case class POOptionalOutput(
    po: String,
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

case class AssessmentMethodEntryOutput(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

case class PrerequisiteEntryOutput(
    text: String,
    modules: List[UUID],
    pos: List[String]
)

case class AssessmentMethodsOutput(
    mandatory: List[AssessmentMethodEntryOutput],
    optional: List[AssessmentMethodEntryOutput]
)

case class PrerequisitesOutput(
    recommended: Option[PrerequisiteEntryOutput],
    required: Option[PrerequisiteEntryOutput]
)

case class POOutput(
    mandatory: List[POMandatoryOutput],
    optional: List[POOptionalOutput]
)

case class MetadataOutput(
    id: UUID,
    gitPath: String,
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: Workload,
    status: String,
    location: String,
    participants: Option[Participants],
    moduleRelation: Option[ModuleRelation],
    moduleManagement: List[String],
    lecturers: List[String],
    assessmentMethods: AssessmentMethodsOutput,
    prerequisites: PrerequisitesOutput,
    po: POOutput,
    competences: List[String],
    globalCriteria: List[String],
    taughtWith: List[UUID]
)

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
  def all(): Future[Seq[MetadataOutput]]
  def allIds(): Future[Seq[(UUID, String)]]
  def allOfUser(user: String): Future[Seq[MetadataOutput]]
  def allPreviewOfUser(user: String): Future[Seq[(UUID, String, String)]]
  def allPreview(): Future[Seq[(UUID, String, String)]]
  def get(id: UUID): Future[MetadataOutput]
}

@Singleton
final class MetadataRepositoryImpl @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends MetadataRepository
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  type PrerequisitesDbResult = (
      List[PrerequisitesDbEntry],
      List[PrerequisitesModuleDbEntry],
      List[PrerequisitesPODbEntry]
  )

  private val metadataTable = TableQuery[MetadataTable]

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

  override def exists(metadata: Metadata) =
    db.run(metadataTable.filter(_.id === metadata.id).result.map(_.nonEmpty))

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

  override def update(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    db.run(
      (
        for {
          _ <- deleteDependencies(metadata)
          _ <- metadataTable
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
          _ <- metadataTable += toDbEntry(metadata, path, timestamp)
          _ <- createDependencies(metadata)
        } yield metadata
      ).transactionally
    )

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

  override def all() =
    retrieve(metadataTable)

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

          val relation = relations.headOption.map { r => // TODO test
            r.relationType match {
              case ModuleRelationType.Parent =>
                ModuleRelation
                  .Parent(
                    relations.map(d => Module(d.module, "???")).toList
                  ) // TODO replace ??? with real data
              case ModuleRelationType.Child =>
                ModuleRelation.Child(Module(r.module, "???"))
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
      metadataTable
        .map(m => (m.id, m.abbrev))
        .result
    )

  override def allOfUser(user: String) =
    retrieve(
      metadataTable
        .joinLeft(responsibilityTable)
        .on(_.id === _.metadata)
        .filter(_._2.map(_.person.toLowerCase === user.toLowerCase))
        .map(_._1)
    )

  def allPreviewOfUser(user: String) =
    db.run(
      metadataTable
        .joinLeft(responsibilityTable)
        .on(_.id === _.metadata)
        .filter(_._2.map(_.person.toLowerCase === user.toLowerCase))
        .map(a => (a._1.id, a._1.title, a._1.abbrev))
        .distinct
        .result
    )

  def allPreview() =
    db.run(
      metadataTable
        .map(m => (m.id, m.title, m.abbrev))
        .result
    )

  override def get(id: UUID) =
    retrieve(metadataTable.filter(_.id === id))
      .flatMap(xs =>
        if (xs.size > 1)
          Future.failed(new Throwable(s"expected one element, but found: $xs"))
        else
          Future.successful(xs.head)
      )
}
