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

trait ModulePreview {

  protected def diffApiService: GitDiffApiService
  protected def downloadService: GitFileDownloadService
  protected implicit def ctx: ExecutionContext

  private implicit def config: GitConfig = diffApiService.config

  def changedActiveModulesFromPreview(po: String, liveModules: Seq[UUID]): Future[Seq[ModuleProtocol]] =
    diffApiService
      .compare(config.mainBranch, config.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.map(d => downloadService.downloadModuleFromPreviewBranch(d.path.moduleId.get))
        Future.sequence(downloads.seq)
      }
      .map(_.collect {
        case Some(m) if ModuleStatus.isActive(m.metadata.status) && m.metadata.po.hasPORelation(po) => m
      }.toSeq)

  def changedActiveModulesFromPreviewWithLastModified(
      po: String,
      liveModules: Seq[UUID]
  ): Future[Seq[(ModuleProtocol, Option[LocalDateTime])]] =
    diffApiService
      .compare(config.mainBranch, config.draftBranch)
      .flatMap { diffs =>
        val downloads =
          diffs.par.map(d => downloadService.downloadModuleFromPreviewBranchWithLastModified(d.path.moduleId.get))
        Future.sequence(downloads.seq)
      }
      .map(_.collect {
        case Some((m, lastModified)) if ModuleStatus.isActive(m.metadata.status) && m.metadata.po.hasPORelation(po) =>
          (m, lastModified)
      }.toSeq)

  @targetName("mergeModulesWithLastModified")
  def mergeModules(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])]
  ): Seq[(ModuleProtocol, Option[LocalDateTime])] = {
    val builder = ListBuffer[(ModuleProtocol, Option[LocalDateTime])](changedModules*)
    liveModules.foreach {
      case (liveModule, lastModified) =>
        if !builder.exists(_._1.id.get == liveModule.id.get) then {
          builder.append((liveModule, Some(lastModified)))
        }
    }
    assume(
      builder.size == builder.distinctBy(_._1.id.get).size,
      s"""expected modules to be unique
         |liveModules: ${liveModules.map(_._1.id.get)}
         |changedModules: ${changedModules.map(_._1.id.get)}
         |builder: ${builder.map(_._1.id.get)}""".stripMargin
    )
    builder.toList
  }

  def mergeModules(
      liveModules: Seq[ModuleProtocol],
      changedModules: Seq[ModuleProtocol]
  ): Seq[ModuleProtocol] = {
    val builder = ListBuffer[ModuleProtocol](changedModules*)
    liveModules.foreach { liveModule =>
      if !builder.exists(_.id.get == liveModule.id.get) then {
        builder.append(liveModule)
      }
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
