package database

enum Schema(val name: String) {
  case Core extends Schema("core")
  case Modules extends Schema("modules")
  case Schedule extends Schema("schedule")

  override def toString = this.name
}
