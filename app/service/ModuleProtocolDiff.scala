package service

import models.ModuleProtocol
import monocle.Lens
import monocle.macros.GenLens
import play.api.Logging

object ModuleProtocolDiff extends Logging {

  import scala.reflect.runtime.universe._

  private def allFields[T: TypeTag]: List[String] = {
    def rec(tpe: Type): List[List[Name]] = {
      val collected = tpe.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }.toList
      if (collected.isEmpty) List(Nil)
      else
        collected.flatMap { m =>
          val opt = m.returnType.typeArgs.flatMap(rec)
          (if (opt.isEmpty) rec(m.returnType) else opt).map(m.name :: _)
        }
    }
    rec(typeOf[T]).map(_.mkString("."))
  }

  val fields: Set[String] =
    allFields[ModuleProtocol].foldLeft(Set.empty[String]) { case (acc, prop) =>
      val simplified = prop match {
        case "metadata.po.optional.recommendedSemester" |
            "metadata.po.optional.partOfCatalog" |
            "metadata.po.optional.instanceOf" |
            "metadata.po.optional.specialization" | "metadata.po.optional.po" =>
          "metadata.po.optional"
        case "metadata.po.mandatory.recommendedSemester" |
            "metadata.po.mandatory.specialization" |
            "metadata.po.mandatory.po" =>
          "metadata.po.mandatory"
        case "metadata.prerequisites.required.pos" |
            "metadata.prerequisites.required.modules" |
            "metadata.prerequisites.required.text" =>
          "metadata.prerequisites.required"
        case "metadata.prerequisites.recommended.pos" |
            "metadata.prerequisites.recommended.modules" |
            "metadata.prerequisites.recommended.text" =>
          "metadata.prerequisites.recommended"
        case "metadata.assessmentMethods.optional.precondition" |
            "metadata.assessmentMethods.optional.percentage" |
            "metadata.assessmentMethods.optional.method" =>
          "metadata.assessmentMethods.optional"
        case "metadata.assessmentMethods.mandatory.precondition" |
            "metadata.assessmentMethods.mandatory.percentage" |
            "metadata.assessmentMethods.mandatory.method" =>
          "metadata.assessmentMethods.mandatory"
        case "metadata.participants.max" | "metadata.participants.min" =>
          "metadata.participants"
        case other => other
      }
      acc + simplified
    }

  def nonEmptyKeys(p: ModuleProtocol): Set[String] =
    fields.foldLeft(Set.empty[String]) { case (acc, field) =>
      def takeIf(p: Boolean): Set[String] = if (p) acc + field else acc

      field match {
        case "enContent.particularities" =>
          takeIf(p.enContent.particularities.nonEmpty)
        case "enContent.recommendedReading" =>
          takeIf(p.enContent.recommendedReading.nonEmpty)
        case "enContent.teachingAndLearningMethods" =>
          takeIf(p.enContent.teachingAndLearningMethods.nonEmpty)
        case "enContent.content" =>
          takeIf(p.enContent.content.nonEmpty)
        case "enContent.learningOutcome" =>
          takeIf(p.enContent.learningOutcome.nonEmpty)
        case "deContent.particularities" =>
          takeIf(p.deContent.particularities.nonEmpty)
        case "deContent.recommendedReading" =>
          takeIf(p.deContent.recommendedReading.nonEmpty)
        case "deContent.teachingAndLearningMethods" =>
          takeIf(p.deContent.teachingAndLearningMethods.nonEmpty)
        case "deContent.content" =>
          takeIf(p.deContent.content.nonEmpty)
        case "deContent.learningOutcome" =>
          takeIf(p.deContent.learningOutcome.nonEmpty)
        case "metadata.taughtWith" =>
          takeIf(p.metadata.taughtWith.nonEmpty)
        case "metadata.globalCriteria" =>
          takeIf(p.metadata.globalCriteria.nonEmpty)
        case "metadata.competences" =>
          takeIf(p.metadata.competences.nonEmpty)
        case "metadata.po.optional" =>
          takeIf(p.metadata.po.optional.nonEmpty)
        case "metadata.po.mandatory" =>
          takeIf(p.metadata.po.mandatory.nonEmpty)
        case "metadata.prerequisites.required" =>
          takeIf(p.metadata.prerequisites.required.nonEmpty)
        case "metadata.prerequisites.recommended" =>
          takeIf(p.metadata.prerequisites.recommended.nonEmpty)
        case "metadata.assessmentMethods.optional" =>
          takeIf(p.metadata.assessmentMethods.optional.nonEmpty)
        case "metadata.assessmentMethods.mandatory" =>
          takeIf(p.metadata.assessmentMethods.mandatory.nonEmpty)
        case "metadata.lecturers" =>
          takeIf(true)
        case "metadata.moduleManagement" =>
          takeIf(true)
        case "metadata.moduleRelation" =>
          takeIf(p.metadata.moduleRelation.nonEmpty)
        case "metadata.participants" =>
          takeIf(p.metadata.participants.nonEmpty)
        case "metadata.location" =>
          takeIf(p.metadata.location.nonEmpty)
        case "metadata.status" =>
          takeIf(p.metadata.status.nonEmpty)
        case "metadata.workload.projectWork" =>
          takeIf(p.metadata.workload.projectWork != 0)
        case "metadata.workload.projectSupervision" =>
          takeIf(p.metadata.workload.projectSupervision != 0)
        case "metadata.workload.exercise" =>
          takeIf(p.metadata.workload.exercise != 0)
        case "metadata.workload.practical" =>
          takeIf(p.metadata.workload.practical != 0)
        case "metadata.workload.seminar" =>
          takeIf(p.metadata.workload.seminar != 0)
        case "metadata.workload.lecture" =>
          takeIf(p.metadata.workload.lecture != 0)
        case "metadata.season" =>
          takeIf(p.metadata.season.nonEmpty)
        case "metadata.duration" =>
          takeIf(p.metadata.duration != 0)
        case "metadata.language" =>
          takeIf(p.metadata.language.nonEmpty)
        case "metadata.ects" =>
          takeIf(p.metadata.ects.isPosInfinity)
        case "metadata.moduleType" =>
          takeIf(p.metadata.moduleType.nonEmpty)
        case "metadata.abbrev" =>
          takeIf(p.metadata.abbrev.nonEmpty)
        case "metadata.title" =>
          takeIf(p.metadata.title.nonEmpty)
        case other =>
          logger.error(s"unsupported key: $other")
          acc
      }
    }

  def diff(
      existing: ModuleProtocol,
      newP: ModuleProtocol,
      origin: Option[ModuleProtocol],
      existingUpdatedKeys: Set[String]
  ): (ModuleProtocol, Set[String]) =
    fields.foldLeft(
      (existing, existingUpdatedKeys)
    ) { case ((existing, existingUpdatedKeys), field) =>
      def go[A](lens: Lens[ModuleProtocol, A]) = {
        val lensExisting = lens.get(existing)
        val lensNewProtocol = lens.get(newP)
        if (lensExisting != lensNewProtocol) {
          val id = existing.id.fold("-")(_.toString)
          log(field, lensExisting, lensNewProtocol, id)
          val newAcc = lens.replace(lensNewProtocol).apply(existing)
          if (origin.exists(mco => lens.get(mco) == lensNewProtocol)) {
            (newAcc, existingUpdatedKeys - field)
          } else {
            val newKeys = existingUpdatedKeys + field
            (newAcc, newKeys)
          }
        } else {
          (existing, existingUpdatedKeys)
        }
      }

      field match {
        case "enContent.particularities" =>
          go(GenLens[ModuleProtocol].apply(_.enContent.particularities))
        case "enContent.recommendedReading" =>
          go(GenLens[ModuleProtocol].apply(_.enContent.recommendedReading))
        case "enContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleProtocol].apply(
              _.enContent.teachingAndLearningMethods
            )
          )
        case "enContent.content" =>
          go(GenLens[ModuleProtocol].apply(_.enContent.content))
        case "enContent.learningOutcome" =>
          go(GenLens[ModuleProtocol].apply(_.enContent.learningOutcome))
        case "deContent.particularities" =>
          go(GenLens[ModuleProtocol].apply(_.deContent.particularities))
        case "deContent.recommendedReading" =>
          go(GenLens[ModuleProtocol].apply(_.deContent.recommendedReading))
        case "deContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleProtocol].apply(
              _.deContent.teachingAndLearningMethods
            )
          )
        case "deContent.content" =>
          go(GenLens[ModuleProtocol].apply(_.deContent.content))
        case "deContent.learningOutcome" =>
          go(GenLens[ModuleProtocol].apply(_.deContent.learningOutcome))
        case "metadata.taughtWith" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.taughtWith))
        case "metadata.globalCriteria" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.globalCriteria))
        case "metadata.competences" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.competences))
        case "metadata.po.optional" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.po.optional))
        case "metadata.po.mandatory" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.po.mandatory))
        case "metadata.prerequisites.required" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.prerequisites.required))
        case "metadata.prerequisites.recommended" =>
          go(
            GenLens[ModuleProtocol].apply(_.metadata.prerequisites.recommended)
          )
        case "metadata.assessmentMethods.optional" =>
          go(
            GenLens[ModuleProtocol].apply(_.metadata.assessmentMethods.optional)
          )
        case "metadata.assessmentMethods.mandatory" =>
          go(
            GenLens[ModuleProtocol].apply(
              _.metadata.assessmentMethods.mandatory
            )
          )
        case "metadata.lecturers" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.lecturers))
        case "metadata.moduleManagement" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.moduleManagement))
        case "metadata.moduleRelation" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.moduleRelation))
        case "metadata.participants" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.participants))
        case "metadata.location" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.location))
        case "metadata.status" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.status))
        case "metadata.workload.projectWork" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.workload.projectWork))
        case "metadata.workload.projectSupervision" =>
          go(
            GenLens[ModuleProtocol].apply(
              _.metadata.workload.projectSupervision
            )
          )
        case "metadata.workload.exercise" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.workload.exercise))
        case "metadata.workload.practical" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.workload.practical))
        case "metadata.workload.seminar" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.workload.seminar))
        case "metadata.workload.lecture" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.workload.lecture))
        case "metadata.season" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.season))
        case "metadata.duration" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.duration))
        case "metadata.language" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.language))
        case "metadata.ects" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.ects))
        case "metadata.moduleType" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.moduleType))
        case "metadata.abbrev" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.abbrev))
        case "metadata.title" =>
          go(GenLens[ModuleProtocol].apply(_.metadata.title))
        case "id" | "metadata.workload.total" | "metadata.workload.selfStudy" =>
          (existing, existingUpdatedKeys)
        case other =>
          logger.error(s"unsupported key: $other")
          (existing, existingUpdatedKeys)
      }
    }

  private def log[A](
      field: String,
      lensExisting: A,
      lensNewProtocol: A,
      id: String
  ): Unit =
    logger.info(
      s"""updating module
         |=====================================
         |module: $id
         |property: $field
         |from:
         |$lensExisting
         |to:
         |$lensNewProtocol
         |=====================================""".stripMargin
    )
}
