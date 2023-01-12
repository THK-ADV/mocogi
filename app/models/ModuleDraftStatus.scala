package models

sealed trait ModuleDraftStatus {
  override def toString = this match {
    case ModuleDraftStatus.Added    => "added"
    case ModuleDraftStatus.Modified => "modified"
  }
}

object ModuleDraftStatus {
  case object Added extends ModuleDraftStatus
  case object Modified extends ModuleDraftStatus

  def apply(string: String): ModuleDraftStatus =
    string.toLowerCase match {
      case "added"    => Added
      case "modified" => Modified
    }
}
