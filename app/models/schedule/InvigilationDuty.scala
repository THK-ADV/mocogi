package models.schedule

import java.time.Instant
import java.util.UUID

import scala.collection.mutable

import play.api.libs.json.Json
import play.api.libs.json.Reads

/** Room of an exam appointment with its time, invigilators and required invigilators, `None` if unknown. */
case class InvigilationDuty(
    entry: UUID,
    room: UUID,
    start: Instant,
    end: Instant,
    required: Option[Int],
    invigilators: List[String]
)

/** Room of an exam appointment selected for the automatic assignment. */
case class InvigilationDutyRef(entry: UUID, room: UUID)

object InvigilationDutyRef {
  given Reads[InvigilationDutyRef] = Json.reads
}

object InvigilationDuty {

  /**
   * Fills the open known demand of the selected duties in the given order greedily with the candidates having the
   * fewest duties, ties broken by id. Candidates are never added to a duty overlapping one of theirs, which includes
   * the same room. Existing invigilators stay, so the result is a deterministic heuristic, not an optimal schedule.
   * Duties outside the selection only count towards load and overlaps.
   *
   * @return
   *   the duties that changed with their invigilators as sorted set
   */
  def assign(
      duties: Seq[InvigilationDuty],
      candidates: Seq[String],
      selected: Set[InvigilationDutyRef]
  ): List[InvigilationDuty] = {
    val load = mutable.Map.from(candidates.map(_ -> 0))
    val busy = mutable.Map.empty[String, List[InvigilationDuty]].withDefaultValue(Nil)

    def take(p: String, d: InvigilationDuty): Unit = {
      load(p) += 1
      busy(p) = d :: busy(p)
    }
    duties.foreach(d => d.invigilators.filter(load.contains).foreach(take(_, d)))
    duties.toList.flatMap { d =>
      val open =
        if selected(InvigilationDutyRef(d.entry, d.room)) then d.required.fold(0)(_ - d.invigilators.size) else 0
      val chosen =
        if open <= 0 then Nil
        else
          candidates
            .filter(p => busy(p).forall(o => !d.start.isBefore(o.end) || !d.end.isAfter(o.start)))
            .sortBy(p => (load(p), p))
            .take(open)
      chosen.foreach(take(_, d))
      Option.when(chosen.nonEmpty)(d.copy(invigilators = (d.invigilators ++ chosen).sorted))
    }
  }
}
