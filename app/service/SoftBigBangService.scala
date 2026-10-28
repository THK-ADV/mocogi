package service

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import git.*
import git.api.GitApiException
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitFileService
import git.api.GitMergeRequestService
import git.api.GitRepositoryService
import models.core.Identity
import models.ModuleCore
import models.Semester
import service.pipeline.MetadataPipeline
import service.pipeline.Print

/**
 * Publishes selected modules from preview to main ("soft big bang", SBB).
 *
 * Every module is edited on its own branch, which is merged into `preview` after review. `preview` therefore collects
 * changes of different modules that are not necessarily meant to be published together. Only `main` is imported into
 * the database and thus constitutes the published modules. Merging `preview` as a whole would publish everything at
 * once (a "big bang"). Instead, a soft big bang publishes only a selected subset: the files of the selected modules
 * are copied from a fixed preview revision onto a new `sbb` branch based on main, and a merge request labelled `sbb`
 * is opened. The merge event handler checks this merge request again and merges it, or closes it and deletes its
 * branch.
 *
 * The backend keeps no state: the existence of the `sbb` branch marks a pending soft big bang, and the client passes
 * the compared revisions of main and preview back to detect intermediate changes. Only files of the modules folder are
 * published; module companions are ignored as they are going to be removed.
 */
@Singleton
final class SoftBigBangService @Inject() (
    branchService: GitBranchService,
    repositoryService: GitRepositoryService,
    fileService: GitFileService,
    commitService: GitCommitService,
    mergeRequestService: GitMergeRequestService,
    moduleService: ModuleService,
    pipeline: MetadataPipeline,
    webConfig: GitWebConfig,
    implicit val repositoryConfig: GitRepositoryConfig,
    implicit val ctx: ExecutionContext
) {
  import SoftBigBangService.*

  /** Suggests the next part number from the five latest SBB merge requests of the current semester. */
  def title(): Future[String] = {
    val semester = Semester.of()
    val year     = semester.year % 100
    val term     = if semester.abbrev == "wise" then f"WiSe $year%02d/${(year + 1) % 100}%02d" else f"SoSe $year%02d"
    val prefix   = s"Soft Big Bang $term Teil "
    val pattern  = s"${prefix}([0-9]+)".r
    mergeRequestService.latestTitles(branch, prefix).map { titles =>
      val lastPart = titles.iterator.collect { case pattern(part) => part.toInt }.maxOption.getOrElse(0)
      s"$prefix${lastPart + 1}"
    }
  }

  /**
   * Returns the current revisions of main and preview and the modules whose files differ between them, each with its
   * database entry (empty if the module is unknown to the database), the kind of change and the diff. The revisions
   * identify the compared state, which [[create]] needs to detect changes made in the meantime.
   */
  def changes(): Future[(CommitId, CommitId, List[(UUID, Option[ModuleCore], ModuleChange, CompareDiff)])] =
    for {
      (main, preview) <- heads()
      diffs           <- moduleDiffs(main, preview)
      live            <- moduleService.allModuleCore()
      created         <- moduleService.allNewlyCreated()
    } yield {
      val modules = (created ++ live).map(m => m.id -> m).toMap
      (main, preview, diffs.map((id, diff) => (id, modules.get(id), changeOf(id, diff), diff)))
    }

  /**
   * Publishes the selected modules in the state of the compared revisions `main` and `preview`: copies their files
   * from `preview` onto the SBB branch, created from the current main, and opens the merge request.
   *
   * Nothing is written to GitLab unless
   *   - no other soft big bang is pending ([[PendingException]]),
   *   - the selected modules are unchanged in main and preview since the comparison ([[ModulesChangedException]]),
   *     so that exactly the reviewed changes get published,
   *   - every selected module still differs from main ([[IllegalArgumentException]]) and
   *   - the selected modules pass the publication check ([[InvalidModulesException]]), i.e. they only reference modules
   *     that are published or selected, since the merge into main would otherwise break the database import.
   *
   * If opening the MR fails, its branch is retained: GitLab may have created the MR before the response was lost.
   * @return
   *   the URL of the merge request
   */
  def create(
      main: CommitId,
      preview: CommitId,
      modules: Set[UUID],
      title: String,
      author: Identity.Person
  ): Future[String] =
    for {
      _     <- ensureNoPending()
      base  <- checkSelection(main, preview, modules)
      diffs <- moduleDiffs(base, preview)
      selected = diffs.filter((id, _) => modules.contains(id))
      _        = require(selected.map(_._1).toSet == modules, "Selection contains modules without changes")
      contents <- Future.traverse(selected) { (id, diff) =>
        val change = changeOf(id, diff)
        previewContent(id, change, preview).map((id, change, _))
      }
      deleted = contents.collect { case (id, ModuleChange.Deleted, _) => id }.toSet
      checked <- pipeline.publicationCheck(contents.flatMap(_._3).map(Print.apply), deleted)
      _ = checked.left.foreach(errs => throw InvalidModulesException(errs.map(_.getMessage).mkString("\n\n")))
      _       <- commitService.commitToNewBranch(branch, base, author, title, contents.map(commitAction))
      (mr, _) <- mergeRequestService
        .create(
          branch,
          repositoryConfig.mainBranch,
          title,
          selected.map((id, _) => s"- ${GitFilePath(id)}").mkString("\n"),
          needsApproval = false,
          labels = List(label)
        )
    } yield s"${webConfig.repoUrl}/-/merge_requests/${mr.value}"

  /** Current revisions of main and preview. */
  private def heads(): Future[(CommitId, CommitId)] =
    branchService.head(repositoryConfig.mainBranch).zip(branchService.head(repositoryConfig.previewBranch))

  /** Fails if the SBB branch exists, because its fixed name allows only one pending soft big bang. */
  private def ensureNoPending(): Future[Unit] =
    branchService.head(branch).transformWith {
      case Success(_)                                         => Future.failed(PendingException())
      case Failure(e: GitApiException) if e.statusCode == 404 => Future.unit
      case Failure(e)                                         => Future.failed(e)
    }

  /**
   * Fails if selected modules changed in main or preview since the comparison. Changes of other modules are allowed,
   * so concurrent work on preview does not block a publication.
   * @return
   *   the current main revision, which serves as the base of the SBB branch. It is pinned, so main changes after this
   *   check cannot alter what is published.
   */
  private def checkSelection(main: CommitId, preview: CommitId, selected: Set[UUID]): Future[CommitId] =
    for {
      (currentMain, currentPreview) <- heads()
      changed                       <- changedModules(main, currentMain, selected)
        .zip(changedModules(preview, currentPreview, selected))
      affected = changed._1 ++ changed._2
      _        = if affected.nonEmpty then throw ModulesChangedException(affected)
    } yield currentMain

  /** Selected modules whose files differ between both revisions. Compares only if the revisions differ. */
  private def changedModules(before: CommitId, now: CommitId, selected: Set[UUID]): Future[Set[UUID]] =
    if before == now then Future.successful(Set.empty)
    else
      repositoryService
        .compare(before, now)
        .map(_.iterator.flatMap(d => moduleIds(d)).filter(selected.contains).toSet)

  /**
   * Changed module files between main and preview, keyed by module ID. Module IDs never change, but git detects a
   * deleted and a similar added file as rename. Such a diff yields a change for both modules.
   */
  private def moduleDiffs(main: CommitId, preview: CommitId): Future[List[(UUID, CompareDiff)]] =
    repositoryService.compare(main, preview).map(_.flatMap(d => moduleIds(d).map(_ -> d)))

  private def moduleIds(diff: CompareDiff): List[UUID] =
    List(diff.oldPath, diff.newPath).distinct.flatMap(GitFilePath(_).moduleId)

  /**
   * Kind of change of the module file at its canonical path `modules/<id>.md`. A diff whose other side lies at a
   * different path, e.g. a detected rename, adds or deletes the module.
   */
  private def changeOf(id: UUID, diff: CompareDiff): ModuleChange = {
    val path = GitFilePath(id).value
    if diff.deletedFile || diff.newPath != path then ModuleChange.Deleted
    else if diff.newFile || diff.oldPath != path then ModuleChange.Added
    else ModuleChange.Modified
  }

  /** Content of the module file in preview, or none if the module is deleted there. */
  private def previewContent(id: UUID, change: ModuleChange, preview: CommitId): Future[Option[String]] =
    if change == ModuleChange.Deleted then Future.successful(None)
    else fileService.download(GitFilePath(id), preview).map(file => Some(file.get._1.value))

  /**
   * Commit action that brings the module file in main to the state of preview. Only the canonical module path is
   * touched, so renames never delete files outside the modules folder.
   */
  private def commitAction(id: UUID, change: ModuleChange, content: Option[String]): GitCommitAction = {
    val action = change match {
      case ModuleChange.Added    => GitCommitActionType.Create
      case ModuleChange.Modified => GitCommitActionType.Update
      case ModuleChange.Deleted  => GitCommitActionType.Delete
    }
    GitCommitAction(action, GitFilePath(id), content.getOrElse(""))
  }
}

object SoftBigBangService {
  val branch: Branch = Branch("sbb")
  val label: String  = "sbb"

  enum ModuleChange {
    case Added, Modified, Deleted
  }

  /** Another soft big bang is pending. */
  final class PendingException extends RuntimeException("A soft big bang is pending")

  /** Selected modules changed in main or preview since they were compared. */
  final class ModulesChangedException(val modules: Set[UUID])
      extends RuntimeException(s"Selected modules changed since the comparison: ${modules.mkString(", ")}")

  /** Selected modules fail the publication check. */
  final class InvalidModulesException(message: String) extends RuntimeException(message)
}
