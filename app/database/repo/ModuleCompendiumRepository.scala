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

trait ModuleCompendiumRepository {
  def exists(moduleCompendium: ModuleCompendium): Future[Boolean]
  def create(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[ModuleCompendium]
  def update(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[ModuleCompendium]
  def all(filter: Map[String, Seq[String]]): Future[Seq[ModuleCompendiumOutput]]
  def allPreview(
      filter: Map[String, Seq[String]]
  ): Future[Seq[(UUID, String, String)]]
  def allIds(): Future[Seq[(UUID, String)]]
  def get(id: UUID): Future[ModuleCompendiumOutput]
  def paths(ids: Seq[UUID]): Future[Seq[(UUID, GitFilePath)]]
}

@Singleton
final class ModuleCompendiumRepositoryImpl @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumRepository
    with HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleCompendiumDbEntry, ModuleCompendiumTable] {
  import profile.api._

  type PrerequisitesDbResult = (
      List[PrerequisitesDbEntry],
      List[PrerequisitesModuleDbEntry],
      List[PrerequisitesPODbEntry]
  )

  val tableQuery = TableQuery[ModuleCompendiumTable]

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
      : PartialFunction[(String, String), ModuleCompendiumTable => Rep[
        Boolean
      ]] = { case ("user", value) =>
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
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    ModuleCompendiumDbEntry(
      moduleCompendium.metadata.id,
      path.value,
      timestamp,
      moduleCompendium.metadata.title,
      moduleCompendium.metadata.abbrev,
      moduleCompendium.metadata.kind.abbrev,
      moduleCompendium.metadata.ects.value,
      moduleCompendium.metadata.language.abbrev,
      moduleCompendium.metadata.duration,
      moduleCompendium.metadata.season.abbrev,
      moduleCompendium.metadata.workload,
      moduleCompendium.metadata.status.abbrev,
      moduleCompendium.metadata.location.abbrev,
      moduleCompendium.metadata.participants.map(_.min),
      moduleCompendium.metadata.participants.map(_.max),
      moduleCompendium.deContent,
      moduleCompendium.enContent
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

  override def exists(moduleCompendium: ModuleCompendium) =
    db.run(
      tableQuery
        .filter(_.id === moduleCompendium.metadata.id)
        .result
        .map(_.nonEmpty)
    )

  override def update(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    db.run(
      (
        for {
          _ <- deleteDependencies(moduleCompendium.metadata)
          _ <- tableQuery
            .filter(_.id === moduleCompendium.metadata.id)
            .update(toDbEntry(moduleCompendium, path, timestamp))
          _ <- createDependencies(moduleCompendium.metadata)
        } yield moduleCompendium
      ).transactionally
    )

  override def create(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    db.run(
      (
        for {
          _ <- tableQuery += toDbEntry(moduleCompendium, path, timestamp)
          _ <- createDependencies(moduleCompendium.metadata)
        } yield moduleCompendium
      ).transactionally
    )

  override def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  private def retrieve(
      query: Query[ModuleCompendiumTable, ModuleCompendiumDbEntry, Seq]
  ): Future[Seq[ModuleCompendiumOutput]] = {
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

          ModuleCompendiumOutput(
            m.gitPath,
            MetadataOutput(
              m.id,
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
            ),
            m.deContent,
            m.enContent
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
    retrieve(tableQuery.filter(_.id === id)).single

  override def paths(ids: Seq[UUID]) =
    db.run(
      tableQuery
        .filter(_.id.inSet(ids))
        .map(a => (a.id, a.gitPath))
        .result
        .map(_.map(a => (a._1, GitFilePath(a._2))))
    )
}
