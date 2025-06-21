package controllers

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleDraftRepository
import git.GitFileContent
import models.*
import models.core.ExamPhases
import models.core.ExamPhases.ExamPhase
import models.core.Identity
import models.core.Identity.Person
import parsing.types.ModuleContent
import parsing.YamlParser
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.routing.Router
import play.api.Logging
import service.core.IdentityService
import service.MetadataPipeline
import service.ModuleUpdatePermissionService
import service.Print

@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    router: Provider[Router],
    identityService: IdentityService,
    implicit val ctx: ExecutionContext,
    pipeline: MetadataPipeline,
    moduleDraftRepository: ModuleDraftRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
) extends AbstractController(cc)
    with Logging {

  private def parseSemester(s: String): List[Int] =
    s.trim.split(',').map(_.toInt).toList

  private def parsePeople(s: String, identities: Seq[Identity]): List[String] = {
    val s0 = s.trim
    if s0.isEmpty then Nil
    else
      s0
        .split(',')
        .map { s =>
          val split    = s.trim.split(' ')
          val lastname = split.last
          val people = identities.filter {
            case p: Identity.Person  => p.lastname == lastname
            case _: Identity.Group   => false
            case _: Identity.Unknown => false
          }
          people.size match {
            case 0 => throw Exception(s"expected to find one person, but found 0 for $s")
            case 1 => people.head.id
            case _ =>
              val people1 = people.filter {
                case p: Identity.Person  => p.firstname == split.head
                case _: Identity.Group   => false
                case _: Identity.Unknown => false
              }
              assume(people1.size == 1, s"expected one person, but found $people1 in $s")
              people1.head.id
          }
        }
        .toList
  }

//  def index = Action.async { (_: Request[AnyContent]) =>
//    for
//      identities <- identityService.all()
//      modules = parseModules(identities)
//      res <- Future.sequence(modules.map(module => pipeline.printParseValidate(module, default, module.id.get)))
//    yield {
//      val (success, failure) = res.partition(_.isRight)
//      if failure.nonEmpty then {
//        failure.foreach(f => logger.error("failed to parse module", f.left.get))
//      } else
//        success.foreach { e =>
//          val (module, print) = e.right.get
//          logger.info(module.metadata.title)
//          assume(YamlParser.validateModuleYaml(GitFileContent(print.value)).isEmpty)
//          Files.writeString(
//            Paths
//              .get("/Users/alex/Developer/modulhandbuecher_test/modules/ing_5")
//              .resolve(module.metadata.id.toString + ".md"),
//            print.value
//          )
//        }
//      NoContent
//    }
//  }
//  def index = Action.async { (_: Request[AnyContent]) =>
//    for res <- validate()
//    yield {
//      res match {
//        case Left(value)  => value.foreach(e => logger.error(e.metadata.toString, e))
//        case Right(value) => logger.info("ok")
//      }
//      NoContent
//    }
//  }

  def index = Action { (_: Request[AnyContent]) =>
    NoContent
  }

//  def index = Action.async { (_: Request[AnyContent]) =>
//    moduleForUserRegressionTest()
//  }

  private def parseModules(identities: Seq[Identity]) = {
    val content = Files.readString(Paths.get("tmp/ing_wiw_5.csv"))
    logger.info(s"parsed ${content.length}")
    content.linesIterator
      .drop(1)
      .map { line =>
        val arr = line.split(';')
        logger.info(s"parsing ${arr.toList}")
        assume(arr.length == 6)
        val semester   = parseSemester(arr(0))
        val title      = arr(1).trim
        val ects       = arr(2).trim.toDouble
        val management = NonEmptyList.fromListUnsafe(parsePeople(arr(3), identities))
        val lecturer = {
          val lecturer = parsePeople(arr(4), identities)
          if lecturer.isEmpty then management else NonEmptyList.fromListUnsafe(lecturer)
        }
        val examiner =
          management.size match {
            case 1 => Examiner(management.head, Identity.NN.id)
            case 2 => Examiner(management.head, management.last)
            case _ => Examiner(management.head, management.toList(1))
          }
        val id = UUID.randomUUID()
        ModuleProtocol(
          Some(id),
          MetadataProtocol(
            title,
            "TBA",
            "module",
            ects,
            "de",
            1,
            "ws_ss",
            ModuleWorkload(0, 0, 0, 0, 0, 0),
            "active",
            "gm",
            None,
            None,
            management,
            lecturer,
            ModuleAssessmentMethodsProtocol(Nil, Nil),
            examiner,
            NonEmptyList.one(ExamPhase.none.id),
            ModulePrerequisitesProtocol(None, None),
            ModulePOProtocol(
              List(
                ModulePOMandatoryProtocol("ing_gme5", None, semester),
                ModulePOMandatoryProtocol("ing_een5", None, semester),
                ModulePOMandatoryProtocol("ing_wiw5", None, semester)
              ),
              Nil
            ),
            Nil,
            Nil,
            Nil
          ),
          ModuleContent("", "", "", "", ""),
          ModuleContent("", "", "", "", ""),
        )
      }
      .toList
  }

  private def validate() = {
    val path = Paths.get("/Users/alex/Developer/modulhandbuecher_test/modules")

    val files = Vector(
      "03124901-80e5-4b58-b33a-8a4945464856.md",
      "03dd0c90-3f1b-4717-aeda-a9110a0dbe0e.md",
      "0cce025b-7733-49cf-a021-59a45ae7f053.md",
      "0d474f8b-9f6b-4889-bf86-194197126875.md",
      "0e794425-7420-4637-8cb5-4c3f6c02a4fc.md",
      "10e57e81-b6b9-439c-82de-a756826965d6.md",
      "1470f35b-da45-4516-a3fc-aeb1ca774e25.md",
      "1ae999ce-1746-413f-b25f-d86ddb27f183.md",
      "1d20d94b-4482-4c51-8981-b980a5e6c1cb.md",
      "1eb9f516-e50d-47ae-9886-ceac66cc5ec8.md",
      "232b604c-1ac6-4a51-a984-7cdeac1c04f0.md",
      "2882ae87-18f1-4465-8b60-f5d79e2ad145.md",
      "2a1cdc33-a1ae-4228-888c-625e0db378d5.md",
      "2b1a22bf-9513-4090-a0ec-5ec39d8a4a7d.md",
      "2c71529c-fd66-43f0-9fbc-266e240b92cf.md",
      "2d3e4424-55a6-4c1d-bfab-c7acc198f9ff.md",
      "301458ca-2d04-41af-9561-1a356b4ae8d8.md",
      "32a3de2e-d42f-46b5-bf5e-38651e7cccc3.md",
      "375678c7-d6ab-444e-9318-e5f82e331ceb.md",
      "3f28e778-e437-4672-a907-be679b44c9bb.md",
      "470b2e21-d501-47a1-b22d-c0c5979a23ec.md",
      "4865b839-7537-42a6-9d35-6cc0038cc39a.md",
      "4b8a9ece-7813-4a16-be70-6dd8a9e3ba71.md",
      "4dff7eb6-53d4-436a-a168-27ec8d6d344b.md",
      "52b1716b-8589-456b-b59d-aff83dee42f0.md",
      "56d6b3a5-64b1-4d72-8ccf-d21c3763e915.md",
      "5bef13af-4d1e-4930-bff2-ddfdbc922075.md",
      "62672c5a-3081-4ae4-a83d-772f8e027018.md",
      "6334d51e-93a4-4d42-a9d0-217d92bae15a.md",
      "64ca9ced-be8f-4623-bd06-eecaefa8df8e.md",
      "67ccea01-461a-4e8f-af13-93e51b47a4a5.md",
      "7030aa4c-09f5-4c76-88d5-e6f56a9cf0c0.md",
      "783ffeec-a3c3-45aa-8c26-b248b210076d.md",
      "7d25d072-c600-44ec-9c1f-c8bd2d4e7e8d.md",
      "8bcf53d9-9a9b-44d4-b9ff-554aa9803d52.md",
      "940a9475-ca89-44c6-98fa-6fd38d8b1355.md",
      "9959aec5-298e-40ab-a050-294912e39278.md",
      "9a24e3bb-4b09-4344-801d-30c4434391d2.md",
      "a21c5986-8dc2-4fa8-a7ea-04bb744372e3.md",
      "a2afae50-40fa-433f-839c-df417492b6cc.md",
      "aef6c2c8-cd23-4a7e-b3cc-ac78a4214cd3.md",
      "b06c96d7-2fb7-444a-b210-592a68e0a44b.md",
      "b0f751db-d8a2-4f2e-a0da-b70ec6c775f1.md",
      "b2901a77-7cf7-486d-b9e7-f6de37169534.md",
      "b4a30fc6-55fa-4380-86ae-5cf4dcbd9bbf.md",
      "b891924c-71a6-4dfb-8aea-eb5638a490f4.md",
      "ba206037-11e0-4061-a636-981c91614fd8.md",
      "be387724-dc31-43e2-abbe-dddd1a33fee0.md",
      "bef08548-7198-4c50-a026-027b3370cd8f.md",
      "bf974cdf-0021-400b-b689-c5ca24a24e59.md",
      "cbac732d-b3bb-4e1f-820a-f66e690a06b6.md",
      "cfeb062b-6d1e-4052-8f10-92dc41b188f5.md",
      "d01f1419-788f-45ea-88c9-d76b7147fd2a.md",
      "d111a080-1ed6-4b65-be1a-41c98c5872da.md",
      "d708c6ae-7691-44dc-ae0d-3c9fdae32a67.md",
      "dcb62363-2ba4-4ef6-a12d-f8f3d588ffc1.md",
      "dd5f5b31-11ab-4213-8269-f970905140b0.md",
      "eb803a9f-7424-4907-b812-f68815dec6f5.md",
      "efc8c06f-9a15-423d-ac08-61f405d43cab.md",
      "f0dca524-34b1-4aaa-b7bd-04192e29628d.md",
      "f4863d88-57d7-4fe4-b665-7060753cf33f.md",
      "f5a2e8d3-6667-4566-8f9d-bf452e44a65b.md",
      "fd9d760b-924c-4e77-9d37-edf0cc28061f.md",
    )
    val content = files.map { a =>
      val content = Files.readString(path.resolve(a))
      assume(YamlParser.validateModuleYaml(GitFileContent(content)).isEmpty)
      Print(content)
    }
    pipeline.parseValidateMany(content)
  }

  def moduleForUserRegressionTest() =
    for
      people <- identityService
        .all()
        .map(_.collect { case p: Identity.Person if p.campusId.isDefined => CampusId(p.campusId.get) })
      oldImpl <- Future.sequence(people.map(cid => moduleUpdatePermissionService.allForCampusId(cid).map(cid -> _)))
      newImpl <- Future.sequence(people.map(cid => moduleDraftRepository.allForCampusId(cid).map(cid -> _)))
    yield {
      assume(oldImpl.size == newImpl.size)
      oldImpl.zip(newImpl).foreach {
        case (lhs, rhs) =>
          assume(lhs._1 == rhs._1)
          val js = Json.parse(rhs._2).validate[JsArray].get.value
          assume(lhs._2.size == js.size)
          lhs._2.sortBy(_._1._1.id).zip(js.sortBy(_.\("module").\("id").validate[UUID].get)).foreach {
            case (lhs, rhs) =>
              assume(lhs._1._1.title == rhs.\("module").\("title").validate[String].get)
              assume(lhs._1._1.abbrev == rhs.\("module").\("abbreviation").validate[String].get)
              assume(lhs._1._2.get == rhs.\("ects").validate[Double].get)
              assume(lhs._2.isInherited == rhs.\("isPrivilegedForModule").validate[Boolean].get)
              assume(lhs._3.state().id == rhs.\("moduleDraftState").validate[String].get)
              lhs._3.zip(rhs.\("moduleDraft").validate[JsValue].asOpt).foreach {
                case (lhs, rhs) =>
                  assume(lhs.module == rhs.\("id").validate[UUID].get)
                  assume(lhs.moduleTitle == rhs.\("title").validate[String].get)
                  assume(lhs.moduleAbbrev == rhs.\("abbreviation").validate[String].get)
              }

          }
      }
      logger.info("ok")
      NoContent
    }
}
