package parsing.core

import models.core.AbbrevLabelDescLike

case class AbbrevLabelDescImpl(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends AbbrevLabelDescLike
