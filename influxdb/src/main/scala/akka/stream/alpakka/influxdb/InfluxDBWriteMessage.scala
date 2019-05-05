package akka.stream.alpakka.influxdb

import akka.NotUsed

object InfluxDBWriteMessage {
  // Apply method to use when not using passThrough
  def apply[T](point: T): InfluxDBWriteMessage[T, NotUsed] =
    InfluxDBWriteMessage(point, NotUsed)

  // Java-api - without passThrough
  def create[T](point: T): InfluxDBWriteMessage[T, NotUsed] =
    InfluxDBWriteMessage(point, NotUsed)

  // Java-api - with passThrough
  def create[T, C](point: T, passThrough: C) =
    InfluxDBWriteMessage(point, passThrough)
}

final case class InfluxDBWriteMessage[T, C](point: T, passThrough: C)

final case class InfluxDBWriteResult[T, C](writeMessage: InfluxDBWriteMessage[T, C], error: Option[String])
