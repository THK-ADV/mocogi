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
import ops.Measure
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCompendiumLatexPrinter
import providers.ConfigReader
import service.ModuleCompendiumLatexActor.GenerateLatexFiles

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import javax.inject.Singleton
import scala.annotation.unused
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.control.NonFatal

@Singleton
final class ModuleCompendiumLatexActor(actor: ActorRef) {
  def generateLatexFiles(semester: Semester): Unit =
    actor ! GenerateLatexFiles(semester)
}

object ModuleCompendiumLatexActor {
  private case class GenerateLatexFiles(semester: Semester) extends AnyVal

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
      config: ConfigReader,
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
      private val config: ConfigReader,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with Measure {

    private val compileScript = s"${sys.env("PWD")}/compile_latex.sh"

    private val clearScript = s"${sys.env("PWD")}/clear_latex.sh"

    private def tmpFolder = Paths.get("tmp")

    private def publicFolderName = "public"

    private def assetsFolderName = "assets"

    // TODO commit .tex file
    // TODO resolve ids in printer
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
          val (failure, success) = {
            measure(
              "all prints", {
                pos
                  .flatMap(po =>
                    Seq(
                      po -> PrintingLanguage.German,
                      po -> PrintingLanguage.English
                    )
                  )
                  // .filter(_._1.abbrev.startsWith("inf_inf2")) // TODO DEBUG ONLY
                  .partitionMap { case (po, pLang) =>
                    logger.info(Thread.currentThread().getName)
                    // TODO expand to optional if "partOfCatalog" is set
                    // TODO paralyze printing
                    // TODO write lines while printing
                    // TODO make pdf movement and clearing part of the compile script
                    val content = print(
                      pLang,
                      po,
                      semester,
                      mcs.filter(
                        _.metadata.po.mandatory.exists(_.po == po.abbrev)
                      ),
                      mts,
                      lang,
                      seasons,
                      people,
                      ams,
                      pos
                    )
                    val filename = s"${pLang.id}_${semester.id}_${po.abbrev}"
                    val (res, msg) = measure(
                      "create tex file",
                      createTexFile(filename, content)
                    ) match {
                      case Left(err) =>
                        (None, err)
                      case Right(texFile) =>
                        val res = for {
                          _ <- measure("compile tex file", compile(texFile))
                          pdf <- measure("get pdf", getPdf(texFile))
                          res <- measure("move pdf", movePdf(pdf, filename))
                        } yield res
                        res match {
                          case Left(err) =>
                            (Some(texFile, false), err)
                          case Right(pdfFile) =>
                            (Some(pdfFile, true), "???")
                        }
                    }
                    res match {
                      case Some((pdfFile, compiled)) if compiled =>
                        Right(
                          (
                            po.abbrev,
                            po.version,
                            po.studyProgram.abbrev,
                            semester.id,
                            s"$assetsFolderName/${pdfFile.getFileName.toString}",
                            pLang
                          )
                        )
                      case Some((texFile, compiled)) if !compiled =>
                        Left(Some(texFile), msg)
                      case _ =>
                        Left(None, msg)
                    }
                  }
              }
            )
          }
          measure("clearing all tex files", clear(tmpFolder))
          failure.foreach(res => logger.error(s"${res._1}, ${res._2}"))
          measure(
            "creating db entries",
            moduleCompendiumListRepository.createOrUpdateMany(
              toModuleCompendiumList(success)
            )
          )
        }
      } yield res

    private def toModuleCompendiumList(
        xs: Seq[(String, Int, String, String, String, PrintingLanguage)]
    ): Seq[ModuleCompendiumList.DB] =
      xs.groupBy(_._1)
        .map { case (_, xs) =>
          val x = xs.head
          val de = xs.find(_._6 == PrintingLanguage.German).get._5
          val en = xs.find(_._6 == PrintingLanguage.English).get._5
          ModuleCompendiumList(
            x._1,
            x._2,
            x._3,
            x._4,
            de,
            en,
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

    @unused
    private def movePdf(file: Path, newFilename: String): Either[String, Path] =
      try {
        logger.debug(s"moving pdf $newFilename")
        val dest = Paths.get(publicFolderName, s"$newFilename.pdf")
        Right(Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING))
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    @unused
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
          clearScript,
          config.textBin
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
          compileScript,
          config.textBin,
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
      create(semester)
    }
  }
}
