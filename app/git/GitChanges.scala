package git

case class GitChanges[A](
    added: List[A],
    modified: List[A],
    removed: List[A]
) {
  def map[B](f: A => B): GitChanges[B] =
    GitChanges(added.map(f), modified.map(f), removed.map(f))
}
