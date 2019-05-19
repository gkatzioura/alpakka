/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.influxdb.{InfluxDbSettings, InfluxDbWriteResult}
import akka.stream.alpakka.influxdb.scaladsl.InfluxDbSource
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import org.influxdb.{InfluxDB, InfluxDBException}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import docs.javadsl.TestUtils._
import org.influxdb.dto.Query

class InfluxDbSourceSpec
    extends WordSpec
    with MustMatchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures {

  final val DatabaseName = "InfluxDbSourceSpec"

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  implicit var influxDB: InfluxDB = _

  override protected def beforeAll(): Unit =
    influxDB = setupConnection(DatabaseName)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit =
    populateDatabase(influxDB, classOf[InfluxDbSourceCpu])

  override def afterEach() =
    cleanDatabase(influxDB, DatabaseName)

  "support source" in assertAllStagesStopped {
    // #run-typed
    val query = new Query("SELECT * FROM cpu", DatabaseName);

    val influxDBResult = InfluxDbSource(influxDB, query).runWith(Sink.seq)
    val resultToAssert = influxDBResult.futureValue.head

    val values = resultToAssert.getResults.get(0).getSeries().get(0).getValues

    values.size() mustBe 2
  }

  "exception on source" in assertAllStagesStopped {
    val query = new Query("SELECT man() FROM invalid", DatabaseName);

    val result = InfluxDbSource(influxDB, query) //.runWith(Sink.seq)
      .recover {
        case e: InfluxDBException => InfluxDbWriteResult(null, Some(e.getMessage))
      }
      .runWith(Sink.seq)
      .futureValue

    result mustBe Seq(InfluxDbWriteResult(null, Some("undefined function man()")))
  }

  "exception on typed source" in assertAllStagesStopped {
    val query = new Query("SELECT man() FROM invalid", DatabaseName);

    val result = InfluxDbSource
      .typed(classOf[InfluxDbSourceCpu], InfluxDbSettings.Default, influxDB, query) //.runWith(Sink.seq)
      .recover {
        case e: InfluxDBException => InfluxDbWriteResult(null, Some(e.getMessage))
      }
      .runWith(Sink.seq)
      .futureValue

    result mustBe Seq(InfluxDbWriteResult(null, Some("undefined function man()")))
  }

}
