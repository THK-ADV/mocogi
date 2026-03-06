package database.repo.schedule

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import service.ModuleService
import scala.collection.mutable
import play.api.libs.json.JsValue
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import models.schedule.CourseType
import models.schedule.ScheduleEntry
import models.MetadataProtocol
import java.util.UUID
import scala.collection.mutable.ListBuffer

@Singleton
final class ScheduleEntryBootstrapRepository @Inject() (
    moduleService: ModuleService,                               
    moduleTeachingUnitRepository: ModuleTeachingUnitRepository, 
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*



  // TODO: This is only used to bootstrap module teaching units associations
  def bootstrapModuleTeachingUnit() =
    for {
      modules <- moduleService.allMetadata()
      entries = modules.map {
        case (id, m) =>
          val pos = mutable.Set[String]()
          m.po.mandatory.foreach(po => pos.add(po.po))
          m.po.optional.foreach(po => pos.add(po.po))
          (id.get, pos.toList)
      }
      _ <- moduleTeachingUnitRepository.recreate(entries)
    } yield ()

  // TODO: This is only used to bootstrap schedule entries
  def createFromJson(json: Vector[JsValue]): Future[Unit] = {
    def parse(js: JsValue) =
      for {
        module     <- js.\("uuid").validate[UUID]
        room       <- js.\("raum_kz").validate[String]
        weekday    <- js.\("wochentag").validate[Int]
        time       <- js.\("zeit").validate[Int]
        courseType <- js.\("veranstaltungstyp").validate[String]
      } yield (module, room, weekday, time, courseType)

    def roomQuery(xs: Vector[String]) = {
      val entries = mutable.Map[UUID, String]()
      var sb      = new StringBuilder("insert into schedule.room values ")
      for (x <- xs) {
        val id = UUID.randomUUID()
        assume(entries.put(id, x).isEmpty)
        sb.append(s"('$id','','$x','',0),")
      }
      sb = sb.dropRight(1)
      sb.append(";")
      (entries.toMap, sb.toString())
    }

    def resolveCourseType(str: String): CourseType =
      str match {
        case "V"   => CourseType.Lecture
        case "P"   => CourseType.Lab
        case "UE"  => CourseType.Exercise
        case "S"   => CourseType.Seminar
        case "T"   => CourseType.Tutorial
        case other => throw new Exception(s"unknown course type $other")
      }

//    def courseQuery(xs: Vector[(UUID, String)]) = {
//      val entries = mutable.Map[UUID, (UUID, CourseType)]()
//      var sb      = new StringBuilder("insert into schedule.course values ")
//      for ((module, courseType) <- xs) {
//        val id = UUID.randomUUID()
//        val ct = resolveCourseType(courseType)
//        assume(entries.put(id, (module, ct)).isEmpty)
//        sb.append(s"('$id','$module','${ct.id}'),")
//      }
//      sb = sb.dropRight(1)
//      sb.append(";")
//      (entries.toMap, sb.toString())
//    }

    def scheduleQuery(
        xs: Vector[(UUID, String, Int, Int, String)],
        rooms: Map[UUID, String],
        modules: Seq[(Option[UUID], MetadataProtocol)]
    ) = {
      var sb      = new StringBuilder("insert into schedule.schedule_entry values ")
      val entries = new ListBuffer[ScheduleEntry.DB]()
      val ref     = LocalDateTime.of(2026, 4, 19, 7, 0, 0)
      val df      = DateTimeFormatter.ISO_LOCAL_DATE_TIME

      def add(p: ScheduleEntry.DB) =
        sb.append(
          s"('${p.id}','${p.module}','${p.courseType.id}','${p.start.format(df)}','${p.end.format(df)}',ARRAY[${p.rooms.map(a => s"'$a'").mkString(",")}]::uuid[],'${p.props}'::jsonb),"
        )

      def create(
          module: UUID,
          course: CourseType,
          room: UUID,
          start: LocalDateTime,
          end: LocalDateTime,
          id: UUID,
          props: String
      ): ScheduleEntry.DB = ScheduleEntry(id, module, course, List(room), start, end, Json.parse(props))

      def merge(xs: List[ScheduleEntry.DB]) = {
        // same room
//        for (((m, _, c, s, e), xs) <- xs.groupBy(a => (a.module, a.props, a.course, a.start, a.end))) {
//          if xs.size > 1 then {
//            val module = modules.find(_._1.get == m).get._2
//            println(xs.map(a => (module.title, c, s, e)).mkString(","))
//          }
//        }
        xs
      }

      for ((m, xs) <- xs.filter(a => modules.exists(_._1.get == a._1)).groupBy(_._1)) {
        for ((courseType, xs) <- xs.groupBy(_._5)) {
          val course = resolveCourseType(courseType)
          for ((weekday, xs) <- xs.groupBy(_._3)) {
            for ((roomAbbrev, xs) <- xs.groupBy(_._2)) {
              val times = xs.distinctBy(a => (a._2, a._4)).map(_._4).sorted
              val slots = new ListBuffer[ListBuffer[Int]]()
              slots.append(new ListBuffer[Int]())
              slots.head.append(times.head)

              for (time <- times.drop(1)) {
                if (time - 1) == slots.last.last then {
                  slots.last.append(time)
                } else {
                  slots.append(new ListBuffer[Int]())
                  slots.last.append(time)
                }
              }

              // println(s"$roomAbbrev $slots")
              val room = rooms.filter(_._2 == roomAbbrev)
              assume(room.size == 1, room.toString())
              val day = ref.plusDays(weekday)

              val roomId = room.head._1
              val id     = UUID.randomUUID()
              val module = modules.filter(_._1.get == m)
              assume(module.size == 1, s"$m $module")
              val pos = ListBuffer[JsValue]()
              for (a <- module.head._2.po.mandatory) {
                pos.append(
                  Json.obj(
                    "po"                  -> a.po,
                    "specialization"      -> a.specialization,
                    "recommendedSemester" -> a.recommendedSemester,
                    "mandatory"           -> JsBoolean(true)
                  )
                )
              }
              for (a <- module.head._2.po.optional) {
                pos.append(
                  Json.obj(
                    "po"                  -> a.po,
                    "specialization"      -> a.specialization,
                    "recommendedSemester" -> a.recommendedSemester,
                    "mandatory"           -> JsBoolean(false)
                  )
                )
              }
              val props    = Json.obj("po" -> JsArray.apply(pos.toList)).toString
              val moduleId = module.head._1.get
              for (slot <- slots) {
                if slot.size > 1 then {
                  val start = day.plusHours(slot.min)
                  val end   = day.plusHours(slot.max + 1)
                  entries += create(moduleId, course, roomId, start, end, id, props)
                } else {
                  val time  = slot.head
                  val start = day.plusHours(time)
                  val end   = start.plusHours(1)
                  entries += create(moduleId, course, roomId, start, end, id, props)
                }
              }
            }
          }
        }
      }

      for (x <- merge(entries.toList)) {
        add(x)
      }

      sb = sb.dropRight(1)
      sb.append(";")
      sb.toString()
    }

    val res                 = json.map(parse(_).get)
    val (rooms, roomsQuery) = roomQuery(res.map(_._2).distinct)
    for {
      modules <- moduleService.allMetadata()
      q = scheduleQuery(res, rooms, modules)
      _ <- db.run(
        DBIO.seq(
          sqlu"delete from schedule.schedule_entry",
          sqlu"delete from schedule.room",
          sqlu"#$roomsQuery",
          sqlu"#$q",
        )
      )
    } yield ()
  }
}