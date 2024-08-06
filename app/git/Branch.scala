package git

case class Branch(value: String) extends AnyVal {
  override def toString = value

  def isMainBranch(implicit gitConfig: GitConfig): Boolean =
    value == gitConfig.mainBranch.value
}
