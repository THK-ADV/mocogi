package service

import akka.actor.{Actor, ActorRef, Props}
import database.ModuleCompendiumOutput
import database.repo.{
  AssessmentMethodRepository,
  LanguageRepository,
  ModuleCompendiumListRepository,
  ModuleCompendiumRepository,
  ModuleTypeRepository,
  PORepository,
  PersonRepository,
  SeasonRepository
}
import models.core._
import models.{ModuleCompendiumList, POShort, Semester}
import ops.LoggerOps
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCompendiumLatexPrinter
import service.ModuleCompendiumLatexActor.GenerateLatexFiles

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import javax.inject.Singleton
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
final class ModuleCompendiumLatexActor(actor: ActorRef) {
  def generateLatexFiles(semester: Semester): Unit =
    actor ! GenerateLatexFiles(semester)
}

object ModuleCompendiumLatexActor {
  private case class GenerateLatexFiles(semester: Semester) extends AnyVal

  case class Config(
      texBinPath: String,
      compileScriptPath: String,
      clearScriptPath: String,
      tmpFolderPath: String,
      publicFolderName: String,
      assetsFolderName: String
  )

  def props(
      printer: ModuleCompendiumLatexPrinter,
      moduleCompendiumRepository: ModuleCompendiumRepository,
      moduleCompendiumListRepository: ModuleCompendiumListRepository,
      poRepository: PORepository,
      moduleTypeRepository: ModuleTypeRepository,
      languageRepository: LanguageRepository,
      seasonRepository: SeasonRepository,
      personRepository: PersonRepository,
      assessmentMethodRepository: AssessmentMethodRepository,
      config: Config,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      printer,
      moduleCompendiumRepository,
      moduleCompendiumListRepository,
      poRepository,
      moduleTypeRepository,
      languageRepository,
      seasonRepository,
      personRepository,
      assessmentMethodRepository,
      config,
      ctx
    )
  )

  private final class Impl(
      private val printer: ModuleCompendiumLatexPrinter,
      private val moduleCompendiumRepository: ModuleCompendiumRepository,
      private val moduleCompendiumListRepository: ModuleCompendiumListRepository,
      private val poRepository: PORepository,
      private val moduleTypeRepository: ModuleTypeRepository,
      private val languageRepository: LanguageRepository,
      private val seasonRepository: SeasonRepository,
      private val personRepository: PersonRepository,
      private val assessmentMethodRepository: AssessmentMethodRepository,
      private val config: Config,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def tmpFolder = Paths.get(config.tmpFolderPath)

    // TODO commit .tex file
    private def create(semester: Semester) =
      for {
        pos <- poRepository.allValidShort()
        poIds = pos.map(_.abbrev)
        mcs <- moduleCompendiumRepository.allFromPos(poIds)
        mts <- moduleTypeRepository.all()
        lang <- languageRepository.all()
        seasons <- seasonRepository.all()
        people <- personRepository.all()
        ams <- assessmentMethodRepository.all()
        res <- {
          val res =
            measure(
              "all prints", {
                pos.par
                  .flatMap(po =>
                    Seq(
                      po -> PrintingLanguage.German,
                      po -> PrintingLanguage.English
                    )
                  )
                  .filter(_._1.abbrev.startsWith("inf_inf"))
                  .map { case (po, pLang) =>
                    // TODO expand to optional if "partOfCatalog" is set
                    // TODO write lines while printing
                    // TODO make pdf movement and clearing part of the compile script
                    val content = print(
                      pLang,
                      po,
                      semester,
                      mcs.filter(_.metadata.po.mandatory.exists { a =>
                        a.po == po.abbrev && a.specialization
                          .zip(po.specialization)
                          .fold(true)(a => a._1 == a._2.abbrev)
                      }),
                      mts,
                      lang,
                      seasons,
                      people,
                      ams,
                      pos
                    )
                    val filename =
                      s"${pLang.id}_${semester.id}_${po.fullAbbrev}"
                    createTexFile(filename, content) match {
                      case Left(err) =>
                        Left(None, err)
                      case Right(texFile) =>
                        (for {
                          _ <- compile(texFile)
                          pdf <- getPdf(texFile)
                          res <- movePdf(pdf, filename)
                        } yield res) match {
                          case Left(err) =>
                            markFileAsBroken(texFile) match {
                              case Left(err) =>
                                Left(Some(texFile), err)
                              case Right(texFile) =>
                                Left(Some(texFile), err)
                            }
                          case Right(pdfFile) =>
                            Right(
                              (
                                po.fullAbbrev,
                                po.abbrev,
                                po.version,
                                po.studyProgram.abbrev,
                                semester.id,
                                pdfFile.getFileName,
                                pLang,
                                po.specialization.map(_.abbrev)
                              )
                            )
                        }
                    }
                  }
                  .seq
              }
            )
          clear(tmpFolder)
          res.collect { case Left((file, _)) => logger.error(s"$file") }
          moduleCompendiumListRepository.createOrUpdateMany(
            toModuleCompendiumList(res.collect { case Right(r) => r })
          )
        }
      } yield res

    private def toModuleCompendiumList(
        xs: Seq[
          (
              String,
              String,
              Int,
              String,
              String,
              Path,
              PrintingLanguage,
              Option[String]
          )
        ]
    ): Seq[ModuleCompendiumList.DB] =
      xs.groupBy(_._1)
        .map { case (_, xs) =>
          val x = xs.head
          val de = xs.find(_._7 == PrintingLanguage.German).get._6
          val en = xs.find(_._7 == PrintingLanguage.English).get._6
          ModuleCompendiumList(
            x._1,
            x._2,
            x._3,
            x._8,
            x._4,
            x._5,
            s"${config.assetsFolderName}/${de.toString}",
            s"${config.assetsFolderName}/${en.toString}",
            LocalDateTime.now()
          )
        }
        .toSeq

    private def print(
        pLang: PrintingLanguage,
        po: POShort,
        semester: Semester,
        entries: Seq[ModuleCompendiumOutput],
        moduleTypes: Seq[ModuleType],
        languages: Seq[Language],
        seasons: Seq[Season],
        people: Seq[Person],
        assessmentMethods: Seq[AssessmentMethod],
        poShorts: Seq[POShort]
    ) =
      printer.print(
        po,
        semester,
        entries.sortBy(_.metadata.title),
        moduleTypes,
        languages,
        seasons,
        people,
        assessmentMethods,
        poShorts
      )(pLang)

    private def markFileAsBroken(file: Path): Either[String, Path] =
      try {
        logger.error(s"mark file as broken $file")
        Right(
          Files.move(
            file,
            file.resolveSibling(s"BROKEN_${file.getFileName}"),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    private def movePdf(file: Path, newFilename: String): Either[String, Path] =
      try {
        logger.debug(s"moving pdf $newFilename")
        val dest = Paths.get(config.publicFolderName, s"$newFilename.pdf")
        Right(Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING))
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    private def getPdf(file: Path): Either[String, Path] =
      try {
        val pdf = file.resolveSibling(
          file.getFileName.toString.replace(".tex", ".pdf")
        )
        Right(pdf)
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    private def createTexFile(
        name: String,
        content: StringBuilder
    ): Either[String, Path] =
      try {
        logger.debug("creating tex file")
        val file = tmpFolder.resolve(s"$name.tex")
        Files.deleteIfExists(file)
        val path = Files.createFile(file)
        Files.writeString(path, content)
        Right(path)
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    private def asString(b: ListBuffer[String]): String =
      if (b.isEmpty) ""
      else b.mkString("\n========\n\t- ", "\n\t- ", "\n========")

    private def clear(path: Path): Unit = {
      val process = Process(
        command = Seq(
          "/bin/sh",
          config.clearScriptPath,
          config.texBinPath
        ),
        cwd = path.toAbsolutePath.toFile
      )
      val sdtOut = ListBuffer.empty[String]
      val sdtErr = ListBuffer.empty[String]
      val pLogger = ProcessLogger(sdtOut += _, sdtErr += _)
      logger.debug(s"clearing ${path.getFileName}")
      val res = process ! pLogger
      logger.debug(s"res is $res")
      if (res == 0) logger.debug(asString(sdtOut.appendedAll(sdtErr)))
      else logger.error(asString(sdtOut.appendedAll(sdtErr)))
    }

    private def compile(
        file: Path
    ): Either[String, String] = {
      val process = Process(
        command = Seq(
          "/bin/sh",
          config.compileScriptPath,
          config.texBinPath,
          file.getFileName.toString
        ),
        cwd = file.getParent.toAbsolutePath.toFile
      )

      val sdtOut = ListBuffer.empty[String]
      val sdtErr = ListBuffer.empty[String]
      val pLogger = ProcessLogger(sdtOut += _, sdtErr += _)
      try {
        logger.debug(s"compiling ${file.getFileName}")
        val res = process ! pLogger
        logger.debug(s"res is $res")
        Either.cond(
          res == 0,
          asString(sdtOut.appendedAll(sdtErr)),
          asString(sdtOut.appendedAll(sdtErr))
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }
    }

    override def receive: Receive = { case GenerateLatexFiles(semester) =>
      create(semester) onComplete {
        case Success(value) =>
          logSuccess(s"created ${value.size} module compendium list entries")
        case Failure(e) => logFailure(e)
      }
    }
  }
}
