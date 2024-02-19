package parsing.core

import models.core.IDLabelDesc

case class IDLabelDescImpl(
    id: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends IDLabelDesc
