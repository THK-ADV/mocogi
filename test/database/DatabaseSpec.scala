//package database
//
//import helper.FakeApplication
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//
//class DatabaseSpec extends AnyWordSpec with GuiceOneAppPerSuite with FakeApplication {
//
//  "A Database Spec" should {
//    "return each module for a user" in {
//      import database.table.campusIdColumnType
//
//      val campusIds: Future[Seq[CampusId]] =
//        db.run(
//          TableQuery[IdentityTable]
//            .filter(_.isPerson)
//            .map(_.campusId)
//            .result
//            .map(_.collect { case Some(s) => CampusId(s) })
//        )
//
//      val allModules: Future[Seq[ModuleCore]] = {
//        val q1: Future[Seq[ModuleCore]] =
//          db.run(TableQuery[ModuleTable].map(a => (a.id, a.title, a.abbrev)).result.map(_.map(ModuleCore.apply)))
//        val q2: Future[Seq[ModuleCore]] = db.run(
//          TableQuery[ModuleDraftTable]
//            .map(a => (a.module, a.moduleTitle, a.moduleAbbrev))
//            .result
//            .map(_.map(ModuleCore.apply))
//        )
//        val q3: Future[Seq[ModuleCore]] = db.run(
//          TableQuery[CreatedModuleTable]
//            .map(a => (a.module, a.moduleTitle, a.moduleAbbrev))
//            .result
//            .map(_.map(ModuleCore.apply))
//        )
//        for {
//          live <- q1
//          drafts <- q2
//          created <- q3
//        } yield {
//          val xs = ListBuffer[ModuleCore](live *)
//          created.foreach { x =>
//            if xs.exists(_.id == x.id) then {
//              logger.error(s"module $x exists twice")
//            } else {
//              xs += x
//            }
//          }
//          drafts.foreach { x =>
//            xs += x
//          }
//          xs.toList
//        }
//      }
//
//      def moduleUpdatePermissions(ids: Seq[CampusId]): Future[Map[CampusId, Seq[UUID]]] =
//        db.run(TableQuery[ModuleUpdatePermissionTable].filter(_.campusId.inSet(ids)).result)
//          .map(_.groupBy(_._2).map(a => (a._1, a._2.map(_._1))))
//
//      def parseResult(s: String, cid: CampusId) = {
//        try {
//          Json.parse(s).validate[JsArray].get.value.map(_.\("module").\("id").validate[UUID].get)
//        } catch {
//          case NonFatal(e) =>
//            logger.error(s"[$cid] unable to parse $s")
//            throw e
//        }
//      }
//
//      def getModulesForUser(map: Map[CampusId, Seq[UUID]], allModules: Seq[ModuleCore]) = Future.sequence(map.map {
//        case (cid, modules) =>
//          val query = sql"select get_modules_for_user(${cid.value}::text)".as[String].head
//          val res: Future[String] = db.run(query)
//          res.map(r =>
//            (
//              cid,
//              modules.map { id =>
//                val res = allModules.find(_.id == id)
//                assert(res.isDefined, s"[$cid]: module $id not found")
//                res.get
//              },
//              parseResult(r, cid)
//            )
//          )
//      })
//
//      for {
//        allModules <- allModules
//        ids <- campusIds
//        perms <- moduleUpdatePermissions(ids)
//        usersModules <- getModulesForUser(perms, allModules)
//      } yield {
//        usersModules.foreach {
//          case (cid, modules, perms) =>
//            modules.foreach { module =>
//              if !perms.contains(module.id) then {
//                logger.error(s"[${cid.value}] - $module")
//              }
//            }
//        }
//        logger.info("ok")
//        NoContent
//      }
//    }
//  }
//}
