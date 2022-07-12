package git.subscriber

import akka.actor.{Actor, Props}
import database.table.ResponsibilityType.Coordinator
import database.table.{
  AssessmentMethodMetadataDbEntry,
  MetadataDbEntry,
  ResponsibilityDbEntry
}
import git.GitFilePath
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parsing.types.Metadata
import play.api.Logging
import service.MetadataService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MetadataDatabaseActor {
  def props(metadataService: MetadataService, ctx: ExecutionContext) = Props(
    new MetadataDatabaseActor(metadataService, ctx)
  )
}

private final class MetadataDatabaseActor(
    metadataService: MetadataService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = { case OnUpdate(changes, _) =>
    changes.added.foreach { case (path, mc) =>
      createOrUpdate(mc.metadata, path)
    }
    changes.modified.foreach { case (path, mc) =>
      createOrUpdate(mc.metadata, path)
    }
    changes.removed.foreach { path =>
      delete(path)
    }
  }

  private def createOrUpdate(metadata: Metadata, gitPath: GitFilePath): Unit =
    metadataService.createOrUpdate(metadata, gitPath) onComplete {
      case Success((a, b, c)) => logSuccess(a, b, c)
      case Failure(e)         => logError(metadata, gitPath, e)
    }

  private def delete(path: GitFilePath): Unit =
    metadataService.delete(path) onComplete {
      case Success(_) =>
        logger.info(
          s"""successfully deleted metadata
             |  - git path: ${path.value}""".stripMargin
        )
      case Failure(t) =>
        logger.error(
          s"""failed to delete metadata
             |  - git path: ${path.value}
             |  - message: ${t.getMessage}
             |  - trace: ${t.getStackTrace.mkString(
              "\n           "
            )}""".stripMargin
        )
    }

  private def logSuccess(
      metadata: MetadataDbEntry,
      responsibilities: List[ResponsibilityDbEntry],
      assessmentMethods: List[AssessmentMethodMetadataDbEntry]
  ): Unit = {
    def fmtList[A](xs: List[A])(f: List[A] => String): String =
      if (xs.isEmpty) "None"
      else f(xs)

    def responsibilitiesFmt(): String =
      fmtList(responsibilities) { responsibilities =>
        val (cord, lec) = responsibilities.partitionMap(e =>
          if (e.kind == Coordinator) Left(e.person) else Right(e.person)
        )
        s"""
           |    - coordinator: ${fmtList(cord)(_.mkString(","))}
           |    - lecturer: ${fmtList(lec)(_.mkString(","))}""".stripMargin
      }

    def assessmentMethodsFmt(): String =
      fmtList(assessmentMethods) { assessmentMethods =>
        assessmentMethods
          .foldLeft("") { case (acc, a) =>
            val p = a.percentage.fold("")(d => s" ($d %)")
            acc + s"${a.assessmentMethod}$p, "
          }
          .dropRight(2)
      }

    logger.info(
      s"""successfully created or updated metadata
         |  - id: ${metadata.id}
         |  - git path: ${metadata.gitPath}
         |  - title: ${metadata.title}
         |  - abbrev: ${metadata.abbrev}
         |  - module type: ${metadata.moduleType}
         |  - children: ${metadata.children}
         |  - parent: ${metadata.parent}
         |  - credits: ${metadata.credits}
         |  - language: ${metadata.language}
         |  - duration: ${metadata.duration}
         |  - recommended semester: ${metadata.recommendedSemester}
         |  - season: ${metadata.season}
         |  - workload total: ${metadata.workloadTotal}
         |  - workload lecture: ${metadata.workloadLecture}
         |  - workload seminar: ${metadata.workloadSeminar}
         |  - workload practical: ${metadata.workloadPractical}
         |  - workload exercise: ${metadata.workloadExercise}
         |  - workload self study: ${metadata.workloadSelfStudy}
         |  - recommended prerequisites: ${metadata.recommendedPrerequisites}
         |  - required prerequisites: ${metadata.requiredPrerequisites}
         |  - status: ${metadata.status}
         |  - location: ${metadata.location}
         |  - po: ${metadata.po}
         |  - responsibilities: ${responsibilitiesFmt()}
         |   -assessment methods: ${assessmentMethodsFmt()}""".stripMargin
    )
  }

  private def logError(
      metadata: Metadata,
      gitPath: GitFilePath,
      t: Throwable
  ): Unit =
    logger.error(
      s"""failed to create or update metadata
         |  - id: ${metadata.id}
         |  - git path: ${gitPath.value}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
