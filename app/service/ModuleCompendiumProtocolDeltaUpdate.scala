package service

import database.ModuleCompendiumOutput
import models.ModuleCompendiumProtocol
import monocle.Lens
import monocle.macros.GenLens

object ModuleCompendiumProtocolDeltaUpdate {

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

  def deltaUpdate(
      existing: ModuleCompendiumProtocol,
      newP: ModuleCompendiumProtocol,
      origin: Option[ModuleCompendiumOutput],
      existingUpdatedKeys: Set[String]
  ): (ModuleCompendiumProtocol, Set[String]) =
    allFields[ModuleCompendiumProtocol].foldLeft(
      (existing, existingUpdatedKeys)
    ) { case ((existing, existingUpdatedKeys), field) =>
      def go[A](
          lens: Lens[ModuleCompendiumProtocol, A],
          lens2: Lens[ModuleCompendiumOutput, A],
          property: String
      ) =
        if (lens.get(existing) != lens.get(newP)) {
          println(s"update of $property: ${lens.get(existing)} => ${lens.get(newP)}")
          val newAcc = lens.replace(lens.get(newP)).apply(existing)
          if (origin.exists(mco => lens2.get(mco) == lens.get(newP))) {
            (newAcc, existingUpdatedKeys - property)
          } else {
            val newKeys = existingUpdatedKeys + property
            (newAcc, newKeys)
          }
        } else {
          (existing, existingUpdatedKeys)
        }

      field match {
        case "enContent.particularities" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.particularities
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.particularities
            ),
            field
          )
        case "enContent.recommendedReading" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.recommendedReading
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.recommendedReading
            ),
            field
          )
        case "enContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.teachingAndLearningMethods
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.teachingAndLearningMethods
            ),
            field
          )
        case "enContent.content" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.content
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.content
            ),
            field
          )
        case "enContent.learningOutcome" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.learningOutcome
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.learningOutcome
            ),
            field
          )
        case "deContent.particularities" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.particularities
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.particularities
            ),
            field
          )
        case "deContent.recommendedReading" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.recommendedReading
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.recommendedReading
            ),
            field
          )
        case "deContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.teachingAndLearningMethods
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.teachingAndLearningMethods
            ),
            field
          )
        case "deContent.content" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.content
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.content
            ),
            field
          )
        case "deContent.learningOutcome" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.learningOutcome
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.learningOutcome
            ),
            field
          )
        case "metadata.taughtWith" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.taughtWith
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.taughtWith
            ),
            field
          )
        case "metadata.globalCriteria" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.globalCriteria
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.globalCriteria
            ),
            field
          )
        case "metadata.competences" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.competences
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.competences
            ),
            field
          )
        case "metadata.po.optional.recommendedSemester" |
            "metadata.po.optional.partOfCatalog" |
            "metadata.po.optional.instanceOf" |
            "metadata.po.optional.specialization" | "metadata.po.optional.po" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.po.optional
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.po.optional
            ),
            "metadata.po.optional"
          )
        case "metadata.po.mandatory.recommendedSemesterPartTime" |
            "metadata.po.mandatory.recommendedSemester" |
            "metadata.po.mandatory.specialization" |
            "metadata.po.mandatory.po" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.po.mandatory
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.po.mandatory
            ),
            "metadata.po.mandatory"
          )
        case "metadata.prerequisites.required.pos" |
            "metadata.prerequisites.required.modules" |
            "metadata.prerequisites.required.text" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.prerequisites.required
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.prerequisites.required
            ),
            "metadata.prerequisites.required"
          )
        case "metadata.prerequisites.recommended.pos" |
            "metadata.prerequisites.recommended.modules" |
            "metadata.prerequisites.recommended.text" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.prerequisites.recommended
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.prerequisites.recommended
            ),
            "metadata.prerequisites.recommended"
          )
        case "metadata.assessmentMethods.optional.precondition" |
            "metadata.assessmentMethods.optional.percentage" |
            "metadata.assessmentMethods.optional.method" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.assessmentMethods.optional
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.assessmentMethods.optional
            ),
            "metadata.assessmentMethods.optional"
          )
        case "metadata.assessmentMethods.mandatory.precondition" |
            "metadata.assessmentMethods.mandatory.percentage" |
            "metadata.assessmentMethods.mandatory.method" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.assessmentMethods.mandatory
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.assessmentMethods.mandatory
            ),
            "metadata.assessmentMethods.mandatory"
          )
        case "metadata.lecturers" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.lecturers),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.lecturers),
            field
          )
        case "metadata.moduleManagement" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.moduleManagement
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.moduleManagement
            ),
            field
          )
        case "metadata.moduleRelation" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.moduleRelation
            ),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.moduleRelation),
            field
          )
        case "metadata.participants.max" | "metadata.participants.min" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.participants
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.participants
            ),
            "metadata.participants"
          )
        case "metadata.location" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.location),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.location),
            field
          )
        case "metadata.status" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.status),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.status),
            field
          )
        case "metadata.workload.projectWork" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.projectWork
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.projectWork
            ),
            "metadata.workload"
          )
        case "metadata.workload.projectSupervision" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.projectSupervision
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.projectSupervision
            ),
            "metadata.workload"
          )
        case "metadata.workload.exercise" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.exercise
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.exercise
            ),
            "metadata.workload"
          )
        case "metadata.workload.practical" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.practical
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.practical
            ),
            "metadata.workload"
          )
        case "metadata.workload.seminar" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.seminar
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.seminar
            ),
            "metadata.workload"
          )
        case "metadata.workload.lecture" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.lecture
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.lecture
            ),
            "metadata.workload"
          )
        case "metadata.season" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.season),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.season),
            field
          )
        case "metadata.duration" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.duration),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.duration),
            field
          )
        case "metadata.language" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.language),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.language),
            field
          )
        case "metadata.ects" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.ects),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.ects),
            field
          )
        case "metadata.moduleType" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.moduleType),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.moduleType),
            field
          )
        case "metadata.abbrev" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.abbrev),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.abbrev),
            field
          )
        case "metadata.title" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.title),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.title),
            field
          )
        case other =>
          println(s"unsupported key: $other")
          (existing, existingUpdatedKeys)
      }
    }
}
