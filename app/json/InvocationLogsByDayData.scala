package json

case class InvocationLogsByDayData(
                                   date: String,
                                   entries: Seq[InvocationLogEntryData]
                                 )
