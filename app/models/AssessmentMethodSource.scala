package models

enum AssessmentMethodSource(val id: String) {
  case Unknown            extends AssessmentMethodSource("unknown")
  case RPO                extends AssessmentMethodSource("rpo")
  case PO(poId: FullPoId) extends AssessmentMethodSource(poId.id)
}
