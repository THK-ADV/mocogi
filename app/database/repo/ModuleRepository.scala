package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
import play.api.Logging
import slick.jdbc.JdbcProfile
import slick.jdbc.TransactionIsolation

@Singleton
final class ModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleDbEntry, ModuleTable]
    with Logging {
  import profile.api.*

  val tableQuery = TableQuery[ModuleTable]

  private val moduleRelationTable =
    TableQuery[ModuleRelationTable]

  private val moduleResponsibilityTable =
    TableQuery[ModuleResponsibilityTable]

  private val moduleAssessmentMethodTable =
    TableQuery[ModuleUsedAssessmentMethodTable]

  private val modulePOMandatoryTable =
    TableQuery[ModulePOMandatoryTable]

  private val modulePOOptionalTable =
    TableQuery[ModulePOOptionalTable]

  private val moduleTaughtWithTable =
    TableQuery[ModuleTaughtWithTable]

  protected val makeFilter: PartialFunction[(String, String), ModuleTable => Rep[
    Boolean
  ]] = {
    case ("user", value) =>
      t =>
        moduleResponsibilityTable
          .filter(r => r.module === t.id && r.isIdentity(value))
          .exists
    case ("id", value) => _.id === UUID.fromString(value)
    case ("po", value) =>
      t =>
        modulePOMandatoryTable
          .filter(a => a.module === t.id && a.fullPo === value)
          .exists ||
          modulePOOptionalTable
            .filter(a => a.module === t.id && a.fullPo === value)
            .exists
  }

  def createOrUpdateMany(modules: Seq[(Module, LocalDateTime)]) = {
    def upsertActions = modules.map {
      case (module, lastModified) =>
        val db = toDbEntry(module, lastModified)
        for {
          exists <- existsAction(module.metadata.id)
          _      <-
            if exists then tableQuery.filter(_.id === module.metadata.id).update(db) else tableQuery += db
        } yield ()
    }
    def deleteDependencyActions =
      modules.map { case (module, _) => deleteDependencies(module.metadata.id) }

    def createDependencyActions =
      modules.map { case (module, _) => createDependencies(module.metadata) }

    // all dependencies are deleted before any is created, so that a child module can move to
    // another parent of the same batch without violating the unique constraint on module_relation
    val actions = DBIO
      .seq(
        DBIO.sequence(upsertActions),
        DBIO.sequence(deleteDependencyActions),
        DBIO.sequence(createDependencyActions)
      )
      .transactionally
    db.run(actions)
  }

  def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  def getLecturers(id: UUID) =
    db.run(moduleResponsibilityTable.filter(a => a.module === id && a.isLecturer).map(_.identity).result)

  /**
   * Returns all POs associated with the given module.
   *
   * @param id the module identifier
   * @return a tuple whose first element holds the mandatory POs and whose second element holds the elective POs
   */
  def getPOs(id: UUID): Future[(Seq[ModulePOMandatoryProtocol], Seq[ModulePOMandatoryProtocol])] =
    db.run(
      modulePOMandatoryTable
        .filter(_.module === id)
        .result
        .map(_.map(m => ModulePOMandatoryProtocol(m.po, m.specialization, m.recommendedSemester)))
        .zip(
          modulePOOptionalTable
            .filter(_.module === id)
            .result
            .map(_.map(o => ModulePOMandatoryProtocol(o.po, o.specialization, o.recommendedSemester)))
        )
    )

  def allModuleCore() =
    db.run(
      tableQuery
        .map(m => (m.id, m.title, m.abbrev))
        .result
        .map(_.map(ModuleCore.apply.tupled))
    )

  def allModuleCoreWithRelations(): Future[(Seq[ModuleCore], Map[UUID, Set[UUID]])] = {
    val action = for {
      cores <- tableQuery
        .map(module => (module.id, module.title, module.abbrev))
        .result
        .map(_.map(ModuleCore.apply.tupled))
      relations <- moduleRelationTable.result
    } yield {
      val relationsByParent = relations
        .groupMap(_.parent)(_.child)
        .view
        .mapValues(_.toSet)
        .toMap

      cores -> relationsByParent
    }

    // repeatable read so that modules and relations describe the same snapshot
    db.run(action.transactionally.withTransactionIsolation(TransactionIsolation.RepeatableRead))
  }

  def allGeneric(): Future[Seq[ModuleCore]] =
    db.run(tableQuery.filter(_.isGeneric).map(a => (a.id, a.title, a.abbrev)).result.map(_.map(ModuleCore.apply)))

  def allGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    db.run(
      tableQuery
        .filter(_.isGeneric)
        .join(modulePOMandatoryTable)
        .on(_.id === _.module)
        .map { case (m, po) => ((m.id, m.title, m.abbrev), po.po, po.specialization) }
        .result
        .map(
          _.groupBy(_._1)
            .map {
              case (m, pos) =>
                (ModuleCore(m._1, m._2, m._3), pos.map { case (_, po, spec) => spec.getOrElse(po) })
            }
            .toSeq
        )
    )

  def allFromPO(po: String | Specialization, activeOnly: Boolean): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val poFilter: ModuleTable => Rep[Boolean] = po match
      case po: String =>
        t =>
          modulePOMandatoryTable.filter(a => t.id === a.module && a.po === po).exists ||
            modulePOOptionalTable.filter(a => t.id === a.module && a.po === po).exists
      case Specialization(id, _, _, po) =>
        t =>
          modulePOMandatoryTable
            .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
            .exists ||
            modulePOOptionalTable
              .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
              .exists

    val parentMatchesPO = (parent: ModuleTable) =>
      if activeOnly then parent.isActive() && poFilter(parent) else poFilter(parent)

    val poOrChildOfPOParent = (module: ModuleTable) =>
      poFilter(module) ||
        moduleRelationTable
          .filter(relation =>
            relation.child === module.id &&
              tableQuery
                .filter(parent => parent.id === relation.parent && parentMatchesPO(parent))
                .exists
          )
          .exists

    val modulesForPO = tableQuery.filter(module =>
      if activeOnly then module.isActive() && poOrChildOfPOParent(module)
      else poOrChildOfPOParent(module)
    )

    retrieve(modulesForPO)
  }

  def deleteDependencies(moduleId: UUID) =
    for {
      _ <- moduleTaughtWithTable.filter(_.module === moduleId).delete
      _ <- modulePOOptionalTable.filter(_.module === moduleId).delete
      _ <- modulePOMandatoryTable.filter(_.module === moduleId).delete
      _ <- moduleAssessmentMethodTable.filter(_.module === moduleId).delete
      _ <- moduleResponsibilityTable.filter(_.module === moduleId).delete
      _ <- moduleRelationTable.filter(_.parent === moduleId).delete
    } yield ()

  def deleteRelations(moduleId: UUID) =
    moduleRelationTable
      .filter(relation => relation.parent === moduleId || relation.child === moduleId)
      .delete

  private def createDependencies(metadata: Metadata) = {
    val methods                   = metadataAssessmentMethods(metadata)
    val (poMandatory, poOptional) = pos(metadata)

    for {
      _ <- moduleRelationTable ++= moduleRelations(metadata)
      _ <- moduleResponsibilityTable ++= responsibilities(metadata)
      _ <- moduleAssessmentMethodTable ++= methods
      _ <- modulePOMandatoryTable ++= poMandatory
      _ <- modulePOOptionalTable ++= poOptional
      _ <- moduleTaughtWithTable ++= metadataTaughtWith(metadata)
    } yield ()
  }

  def exists(module: UUID): Future[Boolean] =
    db.run(existsAction(module))

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
      Examiner(
        module.metadata.examiner.first.id,
        module.metadata.examiner.second.id
      ),
      module.metadata.examPhases.map(_.id),
      module.metadata.participants,
      module.metadata.prerequisites.recommended.map(ModulePrerequisiteEntry.toProtocol),
      module.metadata.prerequisites.required.map(ModulePrerequisiteEntry.toProtocol),
      module.metadata.attendanceRequirement,
      module.metadata.assessmentPrerequisite,
      module.deContent,
      module.enContent
    )

  private def metadataTaughtWith(metadata: Metadata): List[ModuleTaughtWithDbEntry] =
    metadata.taughtWith.map(m => ModuleTaughtWithDbEntry(metadata.id, m.id))

  private def pos(metadata: Metadata): (List[ModulePOMandatoryDbEntry], List[ModulePOOptionalDbEntry]) =
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

  private def moduleRelations(metadata: Metadata): List[ModuleRelationDbEntry] =
    metadata.relation match {
      case Some(ModuleRelation(children)) =>
        children.toList.map(child => ModuleRelationDbEntry(metadata.id, child.id))
      case None =>
        Nil
    }

  private def responsibilities(metadata: Metadata): List[ModuleResponsibilityDbEntry] = {
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

  private def metadataAssessmentMethods(metadata: Metadata): List[ModuleUsedAssessmentMethodDbEntry] = {
    val metadataAssessmentMethods = ListBuffer[ModuleUsedAssessmentMethodDbEntry]()

    metadata.assessmentMethods.mandatory.foreach { m =>
      // this check prevents from adding duplicate values
      if !metadataAssessmentMethods.exists(_.assessmentMethod == m.method.id) then {
        val metadataAssessmentMethod = ModuleUsedAssessmentMethodDbEntry(
          UUID.randomUUID,
          metadata.id,
          m.method.id,
          m.percentage,
          Option.when(m.precondition.nonEmpty)(m.precondition.map(_.id))
        )
        metadataAssessmentMethods += metadataAssessmentMethod
      }
    }

    metadataAssessmentMethods.toList
  }

  private def existsAction(module: UUID) =
    tableQuery.filter(_.id === module).exists.result

  private def retrieve(query: Query[ModuleTable, ModuleDbEntry, Seq]): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val action = query.result.flatMap { modules =>
      val moduleIds = modules.map(_.id).toSet

      if modules.isEmpty then DBIO.successful(Seq.empty)
      else
        for {
          relations         <- moduleRelationTable.filter(_.parent.inSetBind(moduleIds)).result
          responsibilities  <- moduleResponsibilityTable.filter(_.module.inSetBind(moduleIds)).result
          assessmentMethods <- moduleAssessmentMethodTable.filter(_.module.inSetBind(moduleIds)).result
          mandatoryPOs      <- modulePOMandatoryTable.filter(_.module.inSetBind(moduleIds)).result
          optionalPOs       <- modulePOOptionalTable.filter(_.module.inSetBind(moduleIds)).result
          taughtWith        <- moduleTaughtWithTable.filter(_.module.inSetBind(moduleIds)).result
        } yield {
          val relationsByParent         = relations.groupMap(_.parent)(_.child)
          val responsibilitiesByModule  = responsibilities.groupBy(_.module)
          val assessmentMethodsByModule = assessmentMethods.groupBy(_.module)
          val mandatoryPOsByModule      = mandatoryPOs.groupBy(_.module)
          val optionalPOsByModule       = optionalPOs.groupBy(_.module)
          val taughtWithByModule        = taughtWith.groupBy(_.module)

          // a module without any responsibility is incomplete and cannot be assembled
          val incomplete = modules.filterNot(module => responsibilitiesByModule.contains(module.id))
          if incomplete.nonEmpty then {
            logger.error(s"skipping modules without responsibilities: ${incomplete.map(_.id).mkString(", ")}")
          }

          modules.flatMap { module =>
            responsibilitiesByModule.get(module.id).map { responsibilities =>
              val moduleManagement = responsibilities.collect {
                case responsibility if responsibility.responsibilityType == ResponsibilityType.ModuleManagement =>
                  responsibility.identity
              }.toSet
              val lecturer = responsibilities.collect {
                case responsibility if responsibility.responsibilityType == ResponsibilityType.Lecturer =>
                  responsibility.identity
              }.toSet
              val mandatoryAssessmentMethods = assessmentMethodsByModule
                .getOrElse(module.id, Seq.empty)
                .map(method =>
                  ModuleAssessmentMethodEntryProtocol(
                    method.assessmentMethod,
                    method.percentage,
                    method.precondition.getOrElse(Nil)
                  )
                )
                .toSet
              val poMandatory = mandatoryPOsByModule
                .getOrElse(module.id, Seq.empty)
                .map(po =>
                  ModulePOMandatoryProtocol(
                    po.po,
                    po.specialization,
                    po.recommendedSemester
                  )
                )
                .toSet
              val poOptional = optionalPOsByModule
                .getOrElse(module.id, Seq.empty)
                .map(po =>
                  models.ModulePOOptionalProtocol(
                    po.po,
                    po.specialization,
                    po.instanceOf,
                    po.partOfCatalog,
                    po.recommendedSemester
                  )
                )
                .toSet
              val relation = NonEmptyList
                .fromList(
                  relationsByParent
                    .getOrElse(module.id, Seq.empty)
                    .distinct
                    .sortBy(_.toString)
                    .toList
                )
                .map(ModuleRelationProtocol.apply)
              val taughtWithModules = taughtWithByModule
                .getOrElse(module.id, Seq.empty)
                .map(_.moduleTaught)
                .toSet

              (
                ModuleProtocol(
                  Some(module.id),
                  MetadataProtocol(
                    module.title,
                    module.abbrev,
                    module.moduleType,
                    module.ects,
                    module.language,
                    module.duration,
                    module.season,
                    module.workload,
                    module.status,
                    module.location,
                    module.participants,
                    relation,
                    NonEmptyList.fromListUnsafe(moduleManagement.toList),
                    NonEmptyList.fromListUnsafe(lecturer.toList),
                    ModuleAssessmentMethodsProtocol(mandatoryAssessmentMethods.toList),
                    module.examiner,
                    module.examPhases,
                    ModulePrerequisitesProtocol(module.recommendedPrerequisites, module.requiredPrerequisites),
                    ModulePOProtocol(poMandatory.toList, poOptional.toList),
                    taughtWithModules.toList,
                    module.attendanceRequirement,
                    module.assessmentPrerequisite
                  ),
                  module.deContent,
                  module.enContent
                ),
                module.lastModified
              )
            }
          }
        }
    }

    db.run(action.transactionally.withTransactionIsolation(TransactionIsolation.RepeatableRead))
  }
}
