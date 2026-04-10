package controllers

import security.ClientErrorResponse

/** Mix in and provide [[clientErrors]] (e.g. `val clientErrors: ClientErrorResponse` in the controller). */
trait UsesClientErrors {
  protected def clientErrors: ClientErrorResponse
}
