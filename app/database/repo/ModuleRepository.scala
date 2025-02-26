package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.NonEmptyList
import database.*
import database.table.*
import models.*
import models.core.Specialization
import parsing.types.*
import parsing.types.Module
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleDbEntry, ModuleTable] {
  import profile.api.*

  private type PrerequisitesDbResult = (
      List[ModulePrerequisitesDbEntry],
      List[PrerequisitesModuleDbEntry],
      List[PrerequisitesPODbEntry]
  )

  val tableQuery = TableQuery[ModuleTable]

  private val ectsFocusAreaContributionTable =
    TableQuery[ModuleECTSFocusAreaContributionTable]

  private val moduleRelationTable =
    TableQuery[ModuleRelationTable]

  private val responsibilityTable = TableQuery[ModuleResponsibilityTable]

  private val metadataAssessmentMethodTable =
    TableQuery[ModuleAssessmentMethodTable]

  private val metadataAssessmentMethodPreconditionTable =
    TableQuery[ModuleAssessmentMethodPreconditionTable]

  private val prerequisitesTable =
    TableQuery[ModulePrerequisitesTable]

  private val prerequisitesModuleTable =
    TableQuery[PrerequisitesModuleTable]

  private val prerequisitesPOTable =
    TableQuery[PrerequisitesPOTable]

  private val poMandatoryTable =
    TableQuery[ModulePOMandatoryTable]

  private val poOptionalTable =
    TableQuery[ModulePOOptionalTable]

  private val metadataCompetenceTable =
    TableQuery[ModuleCompetenceTable]

  private val metadataGlobalCriteriaTable =
    TableQuery[ModuleGlobalCriteriaTable]

  private val metadataTaughtWithTable =
    TableQuery[ModuleTaughtWithTable]

  protected val makeFilter: PartialFunction[(String, String), ModuleTable => Rep[
    Boolean
  ]] = {
    case ("user", value) =>
      t =>
        responsibilityTable
          .filter(r => r.module === t.id && r.isIdentity(value))
          .exists
    case ("id", value) => _.id === UUID.fromString(value)
    case ("po", value) =>
      t =>
        poMandatoryTable
          .filter(a => a.module === t.id && a.fullPo === value)
          .exists ||
          poOptionalTable
            .filter(a => a.module === t.id && a.fullPo === value)
            .exists
  }

  def createOrUpdateMany(modules: Seq[(Module, LocalDateTime)]) = {
    def createOrUpdateInstant = modules.map {
      case (module, lastModified) =>
        val db = toDbEntry(module, lastModified)
        for {
          exists <- existsAction(module)
          _ <-
            if exists then tableQuery.filter(_.id === module.metadata.id).update(db) else tableQuery += db
        } yield ()
    }
    def dependencies = modules.map {
      case (module, _) =>
        for
          _ <- deleteDependencies(module.metadata.id)
          _ <- createDependencies(module.metadata)
        yield ()
    }

    val actions = DBIO
      .seq(
        DBIO.sequence(createOrUpdateInstant),
        DBIO.sequence(dependencies)
      )
      .transactionally
    db.run(actions)
  }

  def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  def allModuleCore() =
    db.run(
      tableQuery
        .map(m => (m.id, m.title, m.abbrev))
        .result
        .map(_.map(ModuleCore.apply.tupled))
    )

  // TODO the po join does not consider full po id
  def allGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    db.run(
      tableQuery
        .filter(_.moduleType === "generic_module")
        .join(poMandatoryTable)
        .on(_.id === _.module)
        .map { case (m, po) => ((m.id, m.title, m.abbrev), po.po) }
        .result
        .map(
          _.groupBy(_._1)
            .map {
              case (m, pos) =>
                (ModuleCore(m._1, m._2, m._3), pos.map(_._2))
            }
            .toSeq
        )
    )

  // TODO this should be used to fetch modules for po
  def allFromMandatoryPO(po: String | Specialization): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val poFilter: ModuleTable => Rep[Boolean] = po match
      case po: String => t => poMandatoryTable.filter(a => t.id === a.module && a.po === po).exists
      case Specialization(id, _, po) =>
        t =>
          poMandatoryTable
            .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
            .exists
    retrieve(tableQuery.filter(poFilter))
  }

  // TODO this should be used to fetch modules for po
  def allFromPO(po: String | Specialization, activeOnly: Boolean): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val poFilter: ModuleTable => Rep[Boolean] = po match
      case po: String =>
        t =>
          poMandatoryTable.filter(a => t.id === a.module && a.po === po).exists ||
            poOptionalTable.filter(a => t.id === a.module && a.po === po).exists
      case Specialization(id, _, po) =>
        t =>
          poMandatoryTable
            .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
            .exists ||
            poOptionalTable
              .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
              .exists
    retrieve(tableQuery.filter(a => if activeOnly then a.isActive() && poFilter(a) else poFilter(a)))
  }

  def deleteDependencies(moduleId: UUID) =
    for {
      _ <- metadataTaughtWithTable.filter(_.module === moduleId).delete
      _ <- metadataGlobalCriteriaTable.filter(_.module === moduleId).delete
      _ <- metadataCompetenceTable.filter(_.module === moduleId).delete
      _ <- poOptionalTable.filter(_.module === moduleId).delete
      _ <- poMandatoryTable.filter(_.module === moduleId).delete
      prerequisitesQuery = prerequisitesTable.filter(_.module === moduleId)
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
        _.module === moduleId
      )
      _ <- metadataAssessmentMethodPreconditionTable
        .filter(
          _.moduleAssessmentMethod in metadataAssessmentMethodQuery.map(_.id)
        )
        .delete
      _ <- metadataAssessmentMethodQuery.delete
      _ <- responsibilityTable.filter(_.module === moduleId).delete
      _ <- moduleRelationTable.filter(_.module === moduleId).delete
      _ <- ectsFocusAreaContributionTable
        .filter(_.module === moduleId)
        .delete
    } yield ()

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
    } yield ()
  }

  private def toDbEntry(module: Module, timestamp: LocalDateTime) =
    ModuleDbEntry(
      module.metadata.id,
      timestamp,
      module.metadata.title,
      module.metadata.abbrev,
      module.metadata.kind.id,
      module.metadata.ects.value,
      module.metadata.language.id,
      module.metadata.duration,
      module.metadata.season.id,
      module.metadata.workload,
      module.metadata.status.id,
      module.metadata.location.id,
      module.metadata.participants.map(_.min),
      module.metadata.participants.map(_.max),
      Examiner(
        module.metadata.examiner.first.id,
        module.metadata.examiner.second.id
      ),
      module.metadata.examPhases.map(_.id),
      module.deContent,
      module.enContent
    )

  private def metadataTaughtWith(
      metadata: Metadata
  ): List[ModuleTaughtWithDbEntry] =
    metadata.taughtWith.map(m => ModuleTaughtWithDbEntry(metadata.id, m.id))

  private def metadataGlobalCriteria(
      metadata: Metadata
  ): List[ModuleGlobalCriteriaDbEntry] =
    metadata.globalCriteria.map(gc => ModuleGlobalCriteriaDbEntry(metadata.id, gc.id))

  private def metadataCompetences(
      metadata: Metadata
  ): List[ModuleCompetenceDbEntry] =
    metadata.competences.map(c => ModuleCompetenceDbEntry(metadata.id, c.id))

  private def pos(
      metadata: Metadata
  ): (List[ModulePOMandatoryDbEntry], List[ModulePOOptionalDbEntry]) =
    (
      metadata.pos.mandatory.map(po =>
        ModulePOMandatoryDbEntry(
          UUID.randomUUID(),
          metadata.id,
          po.po.id,
          po.specialization.map(_.id),
          po.recommendedSemester
        )
      ),
      metadata.pos.optional.map(po =>
        ModulePOOptionalDbEntry(
          UUID.randomUUID(),
          metadata.id,
          po.po.id,
          po.specialization.map(_.id),
          po.instanceOf.id,
          po.partOfCatalog,
          po.recommendedSemester
        )
      )
    )

  private def prerequisites(metadata: Metadata): PrerequisitesDbResult = {
    val entries = ListBuffer[ModulePrerequisitesDbEntry]()
    val modules = ListBuffer[PrerequisitesModuleDbEntry]()
    val pos     = ListBuffer[PrerequisitesPODbEntry]()

    def go(
        entry: ModulePrerequisiteEntry,
        prerequisiteType: PrerequisiteType
    ) = {
      val prerequisites = ModulePrerequisitesDbEntry(
        UUID.randomUUID,
        metadata.id,
        prerequisiteType,
        entry.text
      )
      entries += prerequisites
      modules ++= entry.modules.map(m => PrerequisitesModuleDbEntry(prerequisites.id, m.id))
      pos ++= entry.pos.map(po => PrerequisitesPODbEntry(prerequisites.id, po.id))
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
    metadata.relation match {
      case Some(ModuleRelation.Parent(children)) =>
        children.toList.map(child =>
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Parent,
            child.id
          )
        )
      case Some(ModuleRelation.Child(parent)) =>
        List(
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Child,
            parent.id
          )
        )
      case None =>
        Nil
    }

  private def contributionsToFocusAreas(
      metadata: Metadata
  ): List[ModuleECTSFocusAreaContributionDbEntry] =
    metadata.ects.contributionsToFocusAreas.map(c =>
      ModuleECTSFocusAreaContributionDbEntry(
        metadata.id,
        c.focusArea.id,
        c.ectsValue,
        c.deDesc,
        c.enDesc
      )
    )

  private def responsibilities(
      metadata: Metadata
  ): List[ModuleResponsibilityDbEntry] = {
    val result = ListBuffer.empty[ModuleResponsibilityDbEntry]
    metadata.responsibilities.lecturers.map(p =>
      result += ModuleResponsibilityDbEntry(
        metadata.id,
        p.id,
        ResponsibilityType.Lecturer
      )
    )
    metadata.responsibilities.moduleManagement.map(p =>
      result += ModuleResponsibilityDbEntry(
        metadata.id,
        p.id,
        ResponsibilityType.ModuleManagement
      )
    )
    result.toList
  }

  private def metadataAssessmentMethods(
      metadata: Metadata
  ): (
      List[ModuleAssessmentMethodDbEntry],
      List[ModuleAssessmentMethodPreconditionDbEntry]
  ) = {
    val metadataAssessmentMethods =
      ListBuffer[ModuleAssessmentMethodDbEntry]()
    val metadataAssessmentMethodPreconditions =
      ListBuffer[ModuleAssessmentMethodPreconditionDbEntry]()

    def go(
        xs: List[ModuleAssessmentMethodEntry],
        `type`: AssessmentMethodType
    ): Unit =
      xs.foreach { m =>
        val metadataAssessmentMethod = ModuleAssessmentMethodDbEntry(
          UUID.randomUUID,
          metadata.id,
          m.method.id,
          `type`,
          m.percentage
        )
        metadataAssessmentMethods += metadataAssessmentMethod
        m.precondition.foreach { m2 =>
          val precondition = ModuleAssessmentMethodPreconditionDbEntry(
            m2.id,
            metadataAssessmentMethod.id
          )
          metadataAssessmentMethodPreconditions += precondition
        }
      }

    go(metadata.assessmentMethods.mandatory, AssessmentMethodType.Mandatory)
    go(metadata.assessmentMethods.optional, AssessmentMethodType.Optional)

    (
      metadataAssessmentMethods.toList,
      metadataAssessmentMethodPreconditions.toList
    )
  }

  private def existsAction(module: Module) =
    tableQuery.filter(_.id === module.metadata.id).exists.result

  private def retrieve(query: Query[ModuleTable, ModuleDbEntry, Seq]): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val methods = metadataAssessmentMethodTable
      .joinLeft(metadataAssessmentMethodPreconditionTable)
      .on(_.id === _.moduleAssessmentMethod)

    val prerequisites = prerequisitesTable
      .joinLeft(prerequisitesModuleTable)
      .on(_.id === _.prerequisites)
      .joinLeft(prerequisitesPOTable)
      .on(_._1.id === _.prerequisites)

    val action = query
      .joinLeft(moduleRelationTable)
      .on(_.id === _.module)
      .join(responsibilityTable)
      .on(_._1.id === _.module)
      .joinLeft(methods)
      .on(_._1._1.id === _._1.module)
      .joinLeft(prerequisites)
      .on(_._1._1._1.id === _._1._1.module)
      .joinLeft(poMandatoryTable)
      .on(_._1._1._1._1.id === _.module)
      .joinLeft(poOptionalTable)
      .on(_._1._1._1._1._1.id === _.module)
      .joinLeft(metadataCompetenceTable)
      .on(_._1._1._1._1._1._1.id === _.module)
      .joinLeft(metadataGlobalCriteriaTable)
      .on(_._1._1._1._1._1._1._1.id === _.module)
      .joinLeft(metadataTaughtWithTable)
      .on(_._1._1._1._1._1._1._1._1.id === _.module)
      .result
    db.run(
      action.map(
        _.groupBy(_._1._1._1._1._1._1._1._1._1)
          .map {
            case (m, deps) =>
              val participants = for {
                min <- m.participantsMin
                max <- m.participantsMax
              } yield ModuleParticipants(min, max)
              val relations        = mutable.HashSet[ModuleRelationDbEntry]()
              val moduleManagement = mutable.HashSet[String]()
              val lecturer         = mutable.HashSet[String]()
              val mandatoryAssessmentMethods =
                mutable.HashSet[(UUID, ModuleAssessmentMethodEntryProtocol)]()
              val optionalAssessmentMethods =
                mutable.HashSet[(UUID, ModuleAssessmentMethodEntryProtocol)]()
              val preconditions =
                mutable.HashSet[ModuleAssessmentMethodPreconditionDbEntry]()
              var recommendedPrerequisite =
                Option.empty[(UUID, ModulePrerequisiteEntryProtocol)]
              var requiredPrerequisite =
                Option.empty[(UUID, ModulePrerequisiteEntryProtocol)]
              val prerequisitesModules =
                mutable.HashSet[PrerequisitesModuleDbEntry]()
              val prerequisitesPOS = mutable.HashSet[PrerequisitesPODbEntry]()
              val poMandatory      = mutable.HashSet[ModulePOMandatoryProtocol]()
              val poOptional       = mutable.HashSet[ModulePOOptionalProtocol]()
              val competences      = mutable.HashSet[String]()
              val globalCriteria   = mutable.HashSet[String]()
              val taughtWith       = mutable.HashSet[UUID]()

              deps
                .foreach {
                  case (((((((((_, mr), r), am), p), poM), poO), c), gc), tw) =>
                    tw.foreach(taughtWith += _.module)
                    gc.foreach(globalCriteria += _.globalCriteria)
                    c.foreach(competences += _.competence)
                    poM.foreach(po =>
                      poMandatory += ModulePOMandatoryProtocol(
                        po.po,
                        po.specialization,
                        po.recommendedSemester
                      )
                    )
                    poO.foreach(po =>
                      poOptional += models.ModulePOOptionalProtocol(
                        po.po,
                        po.specialization,
                        po.instanceOf,
                        po.partOfCatalog,
                        po.recommendedSemester
                      )
                    )
                    p.foreach {
                      case ((e, m), po) =>
                        val prerequisite =
                          Some(
                            e.id -> ModulePrerequisiteEntryProtocol(e.text, Nil, Nil)
                          )
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
                        moduleManagement += r.identity
                      case ResponsibilityType.Lecturer => lecturer += r.identity
                    }
                    am.foreach {
                      case (am, amp) =>
                        amp.foreach(p =>
                          preconditions += ModuleAssessmentMethodPreconditionDbEntry(
                            p.assessmentMethod,
                            p.moduleAssessmentMethod
                          )
                        )
                        am.assessmentMethodType match {
                          case AssessmentMethodType.Mandatory =>
                            mandatoryAssessmentMethods +=
                              am.id ->
                                ModuleAssessmentMethodEntryProtocol(
                                  am.assessmentMethod,
                                  am.percentage,
                                  Nil
                                )

                          case AssessmentMethodType.Optional =>
                            optionalAssessmentMethods +=
                              am.id ->
                                ModuleAssessmentMethodEntryProtocol(
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
                    ModuleRelationProtocol
                      .Parent(
                        NonEmptyList.fromListUnsafe(
                          relations.map(_.relationModule).toList
                        )
                      )
                  case ModuleRelationType.Child =>
                    ModuleRelationProtocol.Child(r.relationModule)
                }
              }

              val module = ModuleProtocol(
                Some(m.id),
                MetadataProtocol(
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
                  NonEmptyList.fromListUnsafe(moduleManagement.toList),
                  NonEmptyList.fromListUnsafe(lecturer.toList),
                  ModuleAssessmentMethodsProtocol(
                    mandatoryAssessmentMethods
                      .map(a =>
                        a._2.copy(precondition =
                          preconditions
                            .filter(_.moduleAssessmentMethod == a._1)
                            .map(_.assessmentMethod)
                            .toList
                        )
                      )
                      .toList,
                    optionalAssessmentMethods
                      .map(a =>
                        a._2.copy(precondition =
                          preconditions
                            .filter(_.moduleAssessmentMethod == a._1)
                            .map(_.assessmentMethod)
                            .toList
                        )
                      )
                      .toList
                  ),
                  m.examiner,
                  m.examPhases,
                  ModulePrerequisitesProtocol(
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
                  ModulePOProtocol(
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
              (module, m.lastModified)
          }
          .toSeq
      )
    )
  }
}
