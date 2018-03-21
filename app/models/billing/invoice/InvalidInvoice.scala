package models.billing.invoice


case class InvalidInvoice(message: String) extends Exception(message)
