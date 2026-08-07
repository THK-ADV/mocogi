package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

import database.table.core.*
import models.core.Identity
import models.core.PO
import models.core.Specialization
import models.ModuleCore
import models.ModuleRelation
import parsing.types.Module
import parsing.types.ModuleContent
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.*
import service.moduledetails.ModuleDetails
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleDetailRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_AssessmentMethodSource
  import profile.api.*

  private val assessmentMethodTable = TableQuery[AssessmentMethodTable]
  private val degreeTable           = TableQuery[DegreeTable]
  private val identityTable         = TableQuery[IdentityTable]
  private val peopleImagesTable     = TableQuery[PeopleImagesTable]
  private val studyProgramTable     = TableQuery[StudyProgramTable]

  def getModuleDetails(id: UUID): Future[Option[Try[ModuleDetails]]] = {
    val query = sql"select modules.get_module_details(${id.toString}::uuid)".as[Option[String]].head
    db.run(query).map(_.map(json => Try(Json.parse(json).as[ModuleDetails])))
  }

  // Note(MD7F2A): Semantics must match Note(MD7F2A) in modules.get_module_details.
  def assemble(module: Module, lastModified: LocalDateTime): Future[ModuleDetails] = {
    val metadata    = module.metadata
    val identityIds = (
      metadata.responsibilities.moduleManagement.toList ++
        metadata.responsibilities.lecturers.toList ++
        List(metadata.examiner.first, metadata.examiner.second)
    ).map(_.id).toSet
    val assessmentIds = metadata.assessmentMethods.mandatory.map(_.method.id).toSet
    val programIds    =
      (metadata.pos.mandatory.map(_.po.program) ++ metadata.pos.optional.map(_.po.program)).toSet

    val query = for {
      identities <- identityTable
        .filter(_.id.inSet(identityIds))
        .joinLeft(peopleImagesTable)
        .on(_.id === _.person)
        .result
      assessmentSources <- assessmentMethodTable
        .filter(_.id.inSet(assessmentIds))
        .map(a => (a.id, a.source))
        .result
      programs <- studyProgramTable
        .filter(_.id.inSet(programIds))
        .join(degreeTable)
        .on(_.degree === _.id)
        .result
    } yield (identities, assessmentSources, programs)

    db.run(query).map { (identities, sources, programs) =>
      val identitiesById    = identities.map((identity, image) => identity.id -> (identity, image)).toMap
      val assessmentSources = sources.map((id, source) => id -> source.id).toMap
      val programsById      = programs.map((program, degree) => program.id -> (program, degree)).toMap

      def identityJson(id: String, withImage: Boolean): JsObject = {
        val (identity, image) = identitiesById(id)
        val json              =
          if identity.kind == Identity.PersonKind then
            Json.obj(
              "id"             -> identity.id,
              "kind"           -> identity.kind,
              "title"          -> identity.title,
              "lastname"       -> Json.toJson(identity.lastname),
              "firstname"      -> Json.toJson(identity.firstname),
              "faculties"      -> Json.toJson(identity.faculties),
              "isActive"       -> identity.isActive,
              "websiteUrl"     -> Json.toJson(identity.websiteUrl),
              "abbreviation"   -> Json.toJson(identity.abbreviation),
              "employmentType" -> Json.toJson(identity.employmentType.map(_.id))
            )
          else
            Json.obj(
              "id"       -> identity.id,
              "kind"     -> identity.kind,
              "title"    -> identity.title,
              "isActive" -> identity.isActive
            )
        if withImage then json + ("imageUrl" -> image.fold[JsValue](JsNull)(value => JsString(value.imageUrl)))
        else json
      }

      def contentJson(content: ModuleContent) =
        Json.obj(
          "learningOutcome" -> content.learningOutcome,
          "moduleContent"   -> content.content,
          "learningMethods" -> content.teachingAndLearningMethods,
          "literature"      -> content.recommendedReading,
          "particularities" -> content.particularities
        )

      def moduleJson(module: ModuleCore) =
        Json.obj("id" -> module.id, "title" -> module.title, "abbreviation" -> module.abbrev)

      def poJson(po: PO, specialization: Option[Specialization], recommendedSemester: List[Int]) = {
        val (studyProgram, degree) = programsById(po.program)
        Json.obj(
          "poId"                     -> po.id,
          "poVersion"                -> po.version,
          "poECTSFactor"             -> po.ectsFactor,
          "studyProgramLabel"        -> studyProgram.deLabel,
          "studyProgramAbbreviation" -> studyProgram.abbreviation,
          "degree"                   -> degree.deLabel,
          "specializationLabel"      -> Json.toJson(specialization.map(_.label)),
          "specializationAbbrev"     -> Json.toJson(specialization.map(_.abbreviation)),
          "recommendedSemester"      -> recommendedSemester,
          "studyProgramId"           -> studyProgram.id
        )
      }

      def prerequisiteJson(prerequisite: models.ModulePrerequisiteEntry) =
        Json.obj("text" -> prerequisite.text, "modules" -> prerequisite.modules.map(moduleJson))

      ModuleDetails(
        metadata.id,
        lastModified,
        metadata.title,
        metadata.abbrev,
        Json.obj("label" -> metadata.kind.deLabel, "id" -> metadata.kind.id),
        metadata.ects.value,
        Json.obj("id" -> metadata.language.id, "label" -> metadata.language.deLabel),
        metadata.duration,
        metadata.season.deLabel,
        metadata.workload,
        Json.obj("label" -> metadata.status.deLabel, "id" -> metadata.status.id),
        metadata.location.deLabel,
        identityJson(metadata.examiner.first.id, withImage = false),
        identityJson(metadata.examiner.second.id, withImage = false),
        metadata.examPhases.toList.map(_.id),
        metadata.participants,
        metadata.prerequisites.recommended.map(prerequisiteJson),
        metadata.prerequisites.required.map(prerequisiteJson),
        contentJson(module.deContent),
        contentJson(module.enContent),
        metadata.responsibilities.moduleManagement.toList.map(i => identityJson(i.id, withImage = true)),
        metadata.responsibilities.lecturers.toList.map(i => identityJson(i.id, withImage = true)),
        metadata.assessmentMethods.mandatory.map(a =>
          Json.obj(
            "id"            -> a.method.id,
            "label"         -> a.method.deLabel,
            "source"        -> assessmentSources(a.method.id),
            "percentage"    -> Json.toJson(a.percentage),
            "preconditions" -> a.precondition.map(_.deLabel)
          )
        ),
        metadata.pos.mandatory.map(po => poJson(po.po, po.specialization, po.recommendedSemester)),
        metadata.pos.optional.map(po =>
          poJson(po.po, po.specialization, po.recommendedSemester) + ("instanceOf" -> moduleJson(po.instanceOf))
        ),
        metadata.taughtWith.map(moduleJson),
        metadata.relation.map {
          case ModuleRelation(children) =>
            Json.obj(
              "relationType" -> "parent",
              "modules"      -> children.toList.sortBy(module => (module.title, module.id)).map(moduleJson)
            )
        },
        metadata.attendanceRequirement,
        metadata.assessmentPrerequisite
      )
    }
  }
}
