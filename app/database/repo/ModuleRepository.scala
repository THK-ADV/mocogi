package database.repo

import database._
import database.table._
import models._
import parsing.types.{Module, _}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import validator._

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleDbEntry, ModuleTable] {
  import profile.api._

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

  protected val makeFilter
      : PartialFunction[(String, String), ModuleTable => Rep[
        Boolean
      ]] = {
    case ("user", value) =>
      t =>
        responsibilityTable
          .filter(r => r.module === t.id && r.isIdentity(value))
          .exists
    case ("id", value) => _.id === UUID.fromString(value)
    case ("po_mandatory", value) =>
      t =>
        poMandatoryTable
          .filter(a => a.module === t.id && a.po === value)
          .exists
  }

  def createOrUpdateMany(modules: Seq[Module], timestamp: LocalDateTime) = {
    def flat(module: Module) =
      module.copy(
        metadata = module.metadata.copy(
          relation = None,
          prerequisites = ModulePrerequisites(None, None),
          pos = ModulePOs(Nil, Nil),
          taughtWith = Nil
        )
      )
    val createOrUpdateInstant = modules.map { module =>
      for {
        exists <- existsAction(module)
        res <-
          if (exists) updateAction(module, timestamp)
          else createAction(flat(module), timestamp)
      } yield res
    }
    val updateAfterCreation = modules.map { module =>
      updateAction(module, timestamp)
    }
    val actions = createOrUpdateInstant.appendedAll(updateAfterCreation)
    db.run(DBIO.sequence(actions).transactionally.map(_.size / 2))
  }

  def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  def allModuleCore(filter: Map[String, Seq[String]]) =
    db.run(
      allWithFilter(filter)
        .map(m => (m.id, m.title, m.abbrev))
        .result
        .map(_.map((ModuleCore.apply _).tupled))
    )

  def allFromPos(pos: Seq[String]) = {
    // TODO expand to optional if "partOfCatalog" is set
    val isMandatoryPO = isMandatoryPOQuery(pos)
    retrieve(tableQuery.filter(_.id.in(isMandatoryPO)))
  }

  private def isMandatoryPOQuery(pos: Seq[String]) =
    poMandatoryTable.filter(_.po.inSet(pos)).map(_.module)

  private def updateAction(module: Module, timestamp: LocalDateTime) =
    for {
      _ <- deleteDependencies(module.metadata)
      _ <- tableQuery
        .filter(_.id === module.metadata.id)
        .update(toDbEntry(module, timestamp))
      _ <- createDependencies(module.metadata)
    } yield ()

  private def createAction(module: Module, timestamp: LocalDateTime) =
    for {
      _ <- tableQuery += toDbEntry(module, timestamp)
      _ <- createDependencies(module.metadata)
    } yield ()

  private def deleteDependencies(metadata: Metadata) =
    for {
      _ <- metadataTaughtWithTable.filter(_.module === metadata.id).delete
      _ <- metadataGlobalCriteriaTable.filter(_.module === metadata.id).delete
      _ <- metadataCompetenceTable.filter(_.module === metadata.id).delete
      _ <- poOptionalTable.filter(_.module === metadata.id).delete
      _ <- poMandatoryTable.filter(_.module === metadata.id).delete
      prerequisitesQuery = prerequisitesTable.filter(_.module === metadata.id)
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
        _.module === metadata.id
      )
      _ <- metadataAssessmentMethodPreconditionTable
        .filter(
          _.moduleAssessmentMethod in metadataAssessmentMethodQuery.map(_.id)
        )
        .delete
      _ <- metadataAssessmentMethodQuery.delete
      _ <- responsibilityTable.filter(_.module === metadata.id).delete
      _ <- moduleRelationTable.filter(_.module === metadata.id).delete
      _ <- ectsFocusAreaContributionTable
        .filter(_.module === metadata.id)
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
    metadata.globalCriteria.map(gc =>
      ModuleGlobalCriteriaDbEntry(metadata.id, gc.id)
    )

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
    val pos = ListBuffer[PrerequisitesPODbEntry]()

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
      modules ++= entry.modules.map(m =>
        PrerequisitesModuleDbEntry(prerequisites.id, m.id)
      )
      pos ++= entry.pos.map(po =>
        PrerequisitesPODbEntry(prerequisites.id, po.id)
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
  ): List[ModuleResponsibilityDbEntry] =
    metadata.responsibilities.lecturers.map(p =>
      ModuleResponsibilityDbEntry(
        metadata.id,
        p.id,
        ResponsibilityType.Lecturer
      )
    ) :::
      metadata.responsibilities.moduleManagement.map(p =>
        ModuleResponsibilityDbEntry(
          metadata.id,
          p.id,
          ResponsibilityType.ModuleManagement
        )
      )

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

  private def retrieve(
      query: Query[ModuleTable, ModuleDbEntry, Seq]
  ): Future[Seq[ModuleProtocol]] = {
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
    db.run(action.map(_.groupBy(_._1._1._1._1._1._1._1._1._1).map {
      case (m, deps) =>
        val participants = for {
          min <- m.participantsMin
          max <- m.participantsMax
        } yield ModuleParticipants(min, max)
        val relations = mutable.HashSet[ModuleRelationDbEntry]()
        val moduleManagement = mutable.HashSet[String]()
        val lecturer = mutable.HashSet[String]()
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
        val poMandatory = mutable.HashSet[ModulePOMandatoryProtocol]()
        val poOptional = mutable.HashSet[ModulePOOptionalProtocol]()
        val competences = mutable.HashSet[String]()
        val globalCriteria = mutable.HashSet[String]()
        val taughtWith = mutable.HashSet[UUID]()

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
              p.foreach { case ((e, m), po) =>
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
                .Parent(relations.map(_.relationModule).toList)
            case ModuleRelationType.Child =>
              ModuleRelationProtocol.Child(r.relationModule)
          }
        }

        ModuleProtocol(
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
            moduleManagement.toList,
            lecturer.toList,
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
    }.toSeq))
  }
}
