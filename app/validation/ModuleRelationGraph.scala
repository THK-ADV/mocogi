package validation

import java.util.UUID

import cats.data.NonEmptyList

/**
 * The parent to children relations of all known modules, indexed in both directions.
 */
final case class ModuleRelationGraph(childrenByParent: Map[UUID, Set[UUID]]) {

  // lazy so that folding [[updated]] over many modules only inverts the final graph
  private lazy val parentsByChild: Map[UUID, Set[UUID]] =
    childrenByParent.toSeq
      .flatMap((parent, children) => children.map(_ -> parent))
      .groupMap(_._1)(_._2)
      .view
      .mapValues(_.toSet)
      .toMap

  def parentsOf(module: UUID): Set[UUID] = parentsByChild.getOrElse(module, Set.empty)

  def isParent(module: UUID): Boolean = childrenByParent.contains(module)

  /**
   * Applies the relation a module declares about itself, replacing whatever the graph knows about it.
   * Returns the same graph if nothing changes, so that re-applying an already applied relation does
   * not invert the graph again.
   */
  def updated(module: UUID, relation: Option[NonEmptyList[UUID]]): ModuleRelationGraph = {
    val children = relation.map(_.toList.toSet)
    if childrenByParent.get(module) == children then this
    else
      ModuleRelationGraph(
        children.fold(childrenByParent.removed(module))(childrenByParent.updated(module, _))
      )
  }
}

object ModuleRelationGraph {
  val empty: ModuleRelationGraph = ModuleRelationGraph(Map.empty)
}
