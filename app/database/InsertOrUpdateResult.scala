package database

sealed trait InsertOrUpdateResult

object InsertOrUpdateResult {
  case object Insert extends InsertOrUpdateResult
  case object Update extends InsertOrUpdateResult
}
