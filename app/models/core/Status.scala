package models.core

case class Status(abbrev: String, deLabel: String, enLabel: String)
    extends AbbrevLabelLike
