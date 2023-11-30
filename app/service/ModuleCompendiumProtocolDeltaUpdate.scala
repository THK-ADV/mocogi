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

  val moduleCompendiumProtocolFields: Set[String] =
    allFields[ModuleCompendiumProtocol].foldLeft(Set.empty[String]) {
      case (acc, prop) =>
        val simplified = prop match {
          case "metadata.po.optional.recommendedSemester" |
              "metadata.po.optional.partOfCatalog" |
              "metadata.po.optional.instanceOf" |
              "metadata.po.optional.specialization" |
              "metadata.po.optional.po" =>
            "metadata.po.optional"
          case "metadata.po.mandatory.recommendedSemesterPartTime" |
              "metadata.po.mandatory.recommendedSemester" |
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

  def deltaUpdate(
      existing: ModuleCompendiumProtocol,
      newP: ModuleCompendiumProtocol,
      origin: Option[ModuleCompendiumOutput],
      existingUpdatedKeys: Set[String]
  ): (ModuleCompendiumProtocol, Set[String]) =
    moduleCompendiumProtocolFields.foldLeft(
      (existing, existingUpdatedKeys)
    ) { case ((existing, existingUpdatedKeys), field) =>
      def go[A](
          lens: Lens[ModuleCompendiumProtocol, A],
          lens2: Lens[ModuleCompendiumOutput, A]
      ) =
        if (lens.get(existing) != lens.get(newP)) {
          println(
            s"""update of $field
               |from:\t${lens.get(existing)}
               |to:\t${lens.get(newP)}""".stripMargin
          )
          val newAcc = lens.replace(lens.get(newP)).apply(existing)
          if (origin.exists(mco => lens2.get(mco) == lens.get(newP))) {
            (newAcc, existingUpdatedKeys - field)
          } else {
            val newKeys = existingUpdatedKeys + field
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
            )
          )
        case "enContent.recommendedReading" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.recommendedReading
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.recommendedReading
            )
          )
        case "enContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.teachingAndLearningMethods
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.teachingAndLearningMethods
            )
          )
        case "enContent.content" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.content
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.content
            )
          )
        case "enContent.learningOutcome" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.enContent.learningOutcome
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.enContent.learningOutcome
            )
          )
        case "deContent.particularities" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.particularities
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.particularities
            )
          )
        case "deContent.recommendedReading" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.recommendedReading
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.recommendedReading
            )
          )
        case "deContent.teachingAndLearningMethods" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.teachingAndLearningMethods
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.teachingAndLearningMethods
            )
          )
        case "deContent.content" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.content
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.content
            )
          )
        case "deContent.learningOutcome" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.deContent.learningOutcome
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.deContent.learningOutcome
            )
          )
        case "metadata.taughtWith" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.taughtWith
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.taughtWith
            )
          )
        case "metadata.globalCriteria" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.globalCriteria
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.globalCriteria
            )
          )
        case "metadata.competences" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.competences
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.competences
            )
          )
        case "metadata.po.optional" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.po.optional
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.po.optional
            )
          )
        case "metadata.po.mandatory" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.po.mandatory
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.po.mandatory
            )
          )
        case "metadata.prerequisites.required" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.prerequisites.required
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.prerequisites.required
            )
          )
        case "metadata.prerequisites.recommended" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.prerequisites.recommended
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.prerequisites.recommended
            )
          )
        case "metadata.assessmentMethods.optional" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.assessmentMethods.optional
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.assessmentMethods.optional
            )
          )
        case "metadata.assessmentMethods.mandatory" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.assessmentMethods.mandatory
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.assessmentMethods.mandatory
            )
          )
        case "metadata.lecturers" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.lecturers),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.lecturers)
          )
        case "metadata.moduleManagement" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.moduleManagement
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.moduleManagement
            )
          )
        case "metadata.moduleRelation" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.moduleRelation
            ),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.moduleRelation)
          )
        case "metadata.participants" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.participants
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.participants
            )
          )
        case "metadata.location" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.location),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.location)
          )
        case "metadata.status" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.status),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.status)
          )
        case "metadata.workload.projectWork" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.projectWork
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.projectWork
            )
          )
        case "metadata.workload.projectSupervision" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.projectSupervision
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.projectSupervision
            )
          )
        case "metadata.workload.exercise" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.exercise
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.exercise
            )
          )
        case "metadata.workload.practical" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.practical
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.practical
            )
          )
        case "metadata.workload.seminar" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.seminar
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.seminar
            )
          )
        case "metadata.workload.lecture" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(
              _.metadata.workload.lecture
            ),
            GenLens[ModuleCompendiumOutput].apply(
              _.metadata.workload.lecture
            )
          )
        case "metadata.season" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.season),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.season)
          )
        case "metadata.duration" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.duration),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.duration)
          )
        case "metadata.language" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.language),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.language)
          )
        case "metadata.ects" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.ects),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.ects)
          )
        case "metadata.moduleType" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.moduleType),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.moduleType)
          )
        case "metadata.abbrev" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.abbrev),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.abbrev)
          )
        case "metadata.title" =>
          go(
            GenLens[ModuleCompendiumProtocol].apply(_.metadata.title),
            GenLens[ModuleCompendiumOutput].apply(_.metadata.title)
          )
        case other =>
          println(s"unsupported key: $other")
          (existing, existingUpdatedKeys)
      }
    }
}
