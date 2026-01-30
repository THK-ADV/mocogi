package database.repo.schedule

import models.MetadataProtocol
import models.schedule.CourseType
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{JsArray, JsBoolean, JsValue, Json}
import service.ModuleService
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ScheduleEntryRepository @Inject() (
    moduleService: ModuleService,                               // TODO: remove after bootstrapping
    moduleTeachingUnitRepository: ModuleTeachingUnitRepository, // TODO: remove after bootstrapping
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  def scheduleEntriesByRange(from: Timestamp, to: Timestamp) = {
    val query = sql"select schedule.get_schedule_entries($from, $to)".as[String].head
    db.run(query)
  }

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

    def courseQuery(xs: Vector[(UUID, String)]) = {
      val entries = mutable.Map[UUID, (UUID, CourseType)]()
      var sb      = new StringBuilder("insert into schedule.course values ")
      for ((module, courseType) <- xs) {
        val id = UUID.randomUUID()
        val ct = resolveCourseType(courseType)
        assume(entries.put(id, (module, ct)).isEmpty)
        sb.append(s"('$id','$module','${ct.id}'),")
      }
      sb = sb.dropRight(1)
      sb.append(";")
      (entries.toMap, sb.toString())
    }

    def scheduleQuery(
        xs: Vector[(UUID, String, Int, Int, String)],
        rooms: Map[UUID, String],
        courses: Map[UUID, (UUID, CourseType)],
        modules: Seq[(Option[UUID], MetadataProtocol)]
    ) = {
      var sb  = new StringBuilder("insert into schedule.schedule_entry values ")
      val ref = LocalDateTime.of(2025, 10, 5, 7, 0, 0)
      val df  = DateTimeFormatter.ISO_LOCAL_DATE_TIME

      def add(course: UUID, room: UUID, start: LocalDateTime, end: LocalDateTime, id: UUID, props: String) =
        sb.append(s"('$id','$course','${start.format(df)}','${end.format(df)}','$room','$props'::jsonb),")

      for ((m, xs) <- xs.filter(a => modules.exists(_._1.get == a._1)).groupBy(_._1)) {
        for ((courseType, xs) <- xs.groupBy(_._5)) {
          val course = courses.filter(a => a._2._1 == m && a._2._2.id == resolveCourseType(courseType).id)
          assume(course.size == 1, course.toString())
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

              println(s"$roomAbbrev $slots")
              val room = rooms.filter(_._2 == roomAbbrev)
              assume(room.size == 1, room.toString())
              val day = ref.plusDays(weekday)

              val courseId = course.head._1
              val roomId   = room.head._1
              val id       = UUID.randomUUID()
              val module   = modules.filter(_._1.get == m)
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
              val props = Json.obj("po" -> JsArray.apply(pos.toList)).toString
              for (slot <- slots) {
                if slot.size > 1 then {
                  val start = day.plusHours(slot.min)
                  val end   = day.plusHours(slot.max + 1)
                  add(courseId, roomId, start, end, id, props)
                } else {
                  val time  = slot.head
                  val start = day.plusHours(time)
                  val end   = start.plusHours(1)
                  add(courseId, roomId, start, end, id, props)
                }
              }
            }
          }
        }
      }
      sb = sb.dropRight(1)
      sb.append(";")
      sb.toString()
    }

    val res                     = json.map(parse(_).get)
    val (rooms, roomsQuery)     = roomQuery(res.map(_._2).distinct)
    val (courses, coursesQuery) = courseQuery(res.map(a => (a._1, a._5)).distinct)
    for {
      modules <- moduleService.allMetadata()
      q = scheduleQuery(res, rooms, courses, modules)
      _ <- db.run(
        DBIO.seq(
          sqlu"delete from schedule.schedule_entry",
          sqlu"delete from schedule.room",
          sqlu"delete from schedule.course",
          sqlu"#$roomsQuery",
          sqlu"#$coursesQuery",
          sqlu"#$q",
        )
      )
    } yield ()
  }
}
