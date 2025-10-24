package service

import java.time.LocalDateTime
import java.util.UUID

import scala.annotation.targetName
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.core.ModuleStatus
import models.ModuleProtocol
import play.api.Logging

trait ModulePreview { self: Logging =>

  protected def diffApiService: GitDiffApiService
  protected def downloadService: GitFileDownloadService
  protected implicit def ctx: ExecutionContext

  private implicit def config: GitConfig = diffApiService.config

  def mergeWithChangedModulesFromPreview(po: String, liveModules: Seq[ModuleProtocol]): Future[Seq[ModuleProtocol]] =
    diffApiService
      .compare(config.mainBranch, config.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.map(d => downloadService.downloadModuleFromPreviewBranch(d.path.moduleId.get))
        Future.sequence(downloads.seq)
      }
      .map { preview =>
        val modules         = new ListBuffer[ModuleProtocol]()
        val inactiveModules = new ListBuffer[UUID]()
        preview.foreach {
          case Some(m) if m.metadata.po.hasPORelation(po) =>
            if liveModules.exists(_.id.get == m.id.get) && !ModuleStatus.isActive(m.metadata.status) then {
              // Ignore modules switch went inactive from live to preview
              inactiveModules += m.id.get
            } else if ModuleStatus.isActive(m.metadata.status) then {
              // only consider active modules
              modules += m
            }
          case _ =>
        }
        val activeLiveModules = liveModules.filterNot(m => inactiveModules.contains(m.id.get))
        mergeModules(activeLiveModules, modules.toSeq)
      }

  def changedActiveModulesFromPreviewWithLastModified(
      po: String,
      liveModules: Seq[(ModuleProtocol, LocalDateTime)]
  ): Future[(Seq[(ModuleProtocol, LocalDateTime)], Seq[(ModuleProtocol, Option[LocalDateTime])])] =
    diffApiService
      .compare(config.mainBranch, config.draftBranch)
      .flatMap { diffs =>
        val downloads =
          diffs.par.map(d => downloadService.downloadModuleFromPreviewBranchWithLastModified(d.path.moduleId.get))
        Future.sequence(downloads.seq)
      }
      .map { preview =>
        val modules         = new ListBuffer[(ModuleProtocol, Option[LocalDateTime])]()
        val inactiveModules = new ListBuffer[UUID]()
        preview.foreach {
          case Some((m, ld)) if m.metadata.po.hasPORelation(po) =>
            if liveModules.exists(_._1.id.get == m.id.get) && !ModuleStatus.isActive(m.metadata.status) then {
              // Ignore modules switch went inactive from live to preview
              inactiveModules += m.id.get
            } else if ModuleStatus.isActive(m.metadata.status) then {
              // only consider active modules
              modules += (m -> ld)
            }
          case _ =>
        }
        val activeLiveModules = liveModules.filterNot(m => inactiveModules.contains(m._1.id.get))
        val previewModules    = modules.toSeq
        (activeLiveModules, previewModules)
      }

  @targetName("mergeModulesWithLastModified")
  def mergeModules(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])],
      bannedGenericModules: List[UUID]
  ): Seq[(ModuleProtocol, Option[LocalDateTime])] = {
    val builder = ListBuffer[(ModuleProtocol, Option[LocalDateTime])](changedModules*)
    liveModules.foreach {
      case (liveModule, lastModified) =>
        if !builder.exists(_._1.id.get == liveModule.id.get) then {
          builder.append((liveModule, Some(lastModified)))
        }
    }
    val nonActiveModules = builder.filterNot(m => ModuleStatus.isActive(m._1.metadata.status))
    if nonActiveModules.nonEmpty then {
      logger.error(
        s"expected active modules, but found: ${nonActiveModules.map(a => (a._1.id.get, a._1.metadata.title))}"
      )
    }
    assume(
      builder.size == builder.distinctBy(_._1.id.get).size,
      s"""expected modules to be unique
         |liveModules: ${liveModules.map(_._1.id.get)}
         |changedModules: ${changedModules.map(_._1.id.get)}
         |builder: ${builder.map(_._1.id.get)}""".stripMargin
    )
    builder.toList.filterNot((m, _) => bannedGenericModules.contains(m.id.get))
  }

  private def mergeModules(
      liveModules: Seq[ModuleProtocol],
      changedModules: Seq[ModuleProtocol]
  ): Seq[ModuleProtocol] = {
    val builder = ListBuffer[ModuleProtocol](changedModules*)
    liveModules.foreach { liveModule =>
      if !builder.exists(_.id.get == liveModule.id.get) then {
        builder.append(liveModule)
      }
    }
    val nonActiveModules = builder.filterNot(m => ModuleStatus.isActive(m.metadata.status))
    if nonActiveModules.nonEmpty then {
      logger.error(s"expected active modules, but found: ${nonActiveModules.map(a => (a.id.get, a.metadata.title))}")
    }
    assume(
      builder.size == builder.distinctBy(_.id.get).size,
      s"""expected modules to be unique
         |liveModules: ${liveModules.map(_.id.get)}
         |changedModules: ${changedModules.map(_.id.get)}
         |builder: ${builder.map(_.id.get)}""".stripMargin
    )
    builder.toList
  }
}
