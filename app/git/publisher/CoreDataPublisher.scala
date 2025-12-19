package git.publisher

import javax.inject.Inject

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import database.view.ModuleViewRepository
import database.view.StudyProgramViewRepository
import git.GitFile
import git.GitFileContent
import models.core.*
import monocle.macros.GenLens
import monocle.Lens
import ops.toFuture
import org.apache.pekko.actor.Actor
import play.api.Logging
import service.core.*

final class CoreDataPublisher @Inject() (
    private val locationService: LocationService,
    private val languageService: LanguageService,
    private val statusService: StatusService,
    private val moduleTypeService: ModuleTypeService,
    private val seasonService: SeasonService,
    private val identityService: IdentityService,
    private val poService: POService,
    private val degreeService: DegreeService,
    private val studyProgramService: StudyProgramService,
    private val specializationService: SpecializationService,
    private val studyProgramViewRepository: StudyProgramViewRepository,
    private val moduleViewRepository: ModuleViewRepository,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  import CoreDataPublisher.*

  override def receive = {
    case Handle(coreFiles) =>
      val order   = topologicalSort(coreFiles)
      val updates = order.foldLeft(Future.unit) {
        case (acc, (filename, _, content)) =>
          acc.flatMap(_ => createOrUpdate(filename, content))
      }
      val res = for {
        _ <- updates
        _ <- studyProgramViewRepository.refreshView()
        _ <- moduleViewRepository.refreshView()
      } yield ()
      res.onComplete {
        case Success(_) => logger.info("finished!")
        case Failure(t) => logFailure(t)
      }
  }

  private def topologicalSort(
      coreFiles: List[(GitFile.CoreFile, GitFileContent)]
  ): Seq[(String, GitFile.CoreFile, GitFileContent)] = {
    val deps     = coreFileDependencies
    val vertices = coreFiles.map(_._1.path.fileName)
    val edges    = vertices.flatMap { f =>
      deps.get(f) match {
        case Some(value) =>
          val self = vertices.indexOf(f)
          value.collect {
            case d if vertices.contains(d) => (self, vertices.indexOf(d))
          }
        case None => Nil
      }
    }
    val sorted = new Graph(vertices, edges).topoSort()
    sorted.map { filename =>
      val (coreFile, content) =
        coreFiles.find(_._1.path.fileName == filename).get
      (filename, coreFile, content)
    }
  }

  private def createOrUpdate(
      filename: String,
      content: GitFileContent
  ): Future[Unit] = {
    def go[A](
        ids: => Future[Seq[String]],
        yamlService: YamlService[A],
        getId: Lens[A, String],
        deleteMany: Seq[String] => Future[Int],
    ) =
      for {
        parser       <- yamlService.parser
        parsedValues <- parser.parse(content.value)._1.toFuture
        existing     <- ids
        (toCreate, toUpdate, toDelete) = split[A](existing, parsedValues, getId)
        _ <- yamlService.createOrUpdateMany(toCreate.appendedAll(toUpdate))
        _ <- deleteMany(toDelete)
      } yield logSuccess(filename, toCreate, toUpdate, toDelete)

    filename match {
      case Filenames.location =>
        go(
          locationService.repo.allIds(),
          locationService,
          GenLens[ModuleLocation](_.id),
          locationService.repo.deleteMany,
        )
      case Filenames.lang =>
        go(
          languageService.repo.allIds(),
          languageService,
          GenLens[ModuleLanguage](_.id),
          languageService.repo.deleteMany,
        )
      case Filenames.status =>
        go(
          statusService.repo.allIds(),
          statusService,
          GenLens[ModuleStatus](_.id),
          statusService.repo.deleteMany,
        )
      case Filenames.module_type =>
        go(
          moduleTypeService.repo.allIds(),
          moduleTypeService,
          GenLens[ModuleType](_.id),
          moduleTypeService.repo.deleteMany,
        )
      case Filenames.season =>
        go(
          seasonService.repo.allIds(),
          seasonService,
          GenLens[Season](_.id),
          seasonService.repo.deleteMany,
        )
      case Filenames.person =>
        go(
          identityService.repo.allIds(),
          identityService,
          Identity.idLens,
          identityService.repo.deleteMany,
        )
      case Filenames.po =>
        go(
          poService.repo.allIds(),
          poService,
          GenLens[PO](_.id),
          poService.repo.deleteMany,
        )
      case Filenames.grade =>
        go(
          degreeService.repo.allIds(),
          degreeService,
          GenLens[Degree](_.id),
          degreeService.repo.deleteMany,
        )
      case Filenames.program =>
        go(
          studyProgramService.allIds(),
          studyProgramService,
          GenLens[StudyProgram](_.id),
          studyProgramService.deleteMany,
        )
      case Filenames.specialization =>
        go(
          specializationService.repo.allIds(),
          specializationService,
          GenLens[Specialization](_.id),
          specializationService.repo.deleteMany,
        )
      case _ =>
        logUnknownFile(filename)
        Future.unit
    }
  }

  private def logSuccess[A](
      filename: String,
      toCreate: Seq[A],
      toUpdate: Seq[A],
      toDelete: Seq[String]
  ): Unit = {
    if (toCreate.nonEmpty) {
      logger.info(
        s"successfully created ${toCreate.size} $filename files"
      )
    }
    if (toUpdate.nonEmpty) {
      logger.info(
        s"successfully updated ${toUpdate.size} $filename files"
      )
    }
    if (toDelete.nonEmpty) {
      logger.info(
        s"successfully deleted ${toDelete.size} $filename files"
      )
    }
  }

  private def logFailure(error: Throwable): Unit =
    logger.error(s"""failed to create or update core data file
                    |  - message: ${error.getMessage}
                    |  - trace: ${error.getStackTrace.mkString(
                     "\n           "
                   )}""".stripMargin)

  private def logUnknownFile(filename: String): Unit =
    logger.info(s"no handler found for file $filename")
}

object CoreDataPublisher {

  case class Handle(coreFiles: List[(GitFile.CoreFile, GitFileContent)]) extends AnyVal

  private object Filenames {
    val location       = "location"
    val lang           = "lang"
    val status         = "status"
    val module_type    = "module_type"
    val season         = "season"
    val person         = "person"
    val po             = "po"
    val grade          = "grade"
    val program        = "program"
    val specialization = "specialization"
  }

  private class Graph[A](vertices: List[A], edges: List[(Int, Int)]) {
    private val numVertices = vertices.size
    private val adjacency   =
      Array.fill(numVertices)(Array.fill(numVertices)(false))

    edges.foreach { case (a, b) => adjacency(a)(b) = true }

    private def hasDependency(r: Int, todo: ListBuffer[Int]): Boolean =
      todo.exists(c => adjacency(r)(c))

    def topoSort(): List[A] = {
      val result = ListBuffer.empty[A]
      val todo   = ListBuffer.tabulate(numVertices)(identity)
      while (todo.nonEmpty) {
        val indexToRemove = todo.indexWhere(r => !hasDependency(r, todo))
        if (indexToRemove != -1) {
          val removedVertex = todo.remove(indexToRemove)
          result += vertices(removedVertex)
        } else {
          throw new Exception("Graph has cycles")
        }
      }
      result.toList
    }
  }

  private def coreFileDependencies = Map(
    Filenames.program        -> List(Filenames.grade, Filenames.person),
    Filenames.po             -> List(Filenames.program),
    Filenames.specialization -> List(Filenames.po),
  )

  def toCreate(e: Seq[String], v: Seq[String]) =
    v.diff(e)

  def toDelete(e: Seq[String], v: List[String]) =
    e.diff(v)

  def toUpdate(e: Seq[String], v: List[String]) =
    e.intersect(v)

  def split[A](
      allIds: Seq[String],
      locations: List[A],
      id: Lens[A, String]
  ): (Seq[A], Seq[A], Seq[String]) = {
    val vIds     = locations.map(id.get)
    val toCreate =
      this
        .toCreate(allIds, vIds)
        .map(i => locations.find(a => id.get(a) == i).get)
    val toUpdate =
      this
        .toUpdate(allIds, vIds)
        .map(i => locations.find(a => id.get(a) == i).get)
    val toDelete = this.toDelete(allIds, vIds)
    (toCreate, toUpdate, toDelete)
  }
}
