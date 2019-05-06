/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.influxdb.InfluxDB
import org.influxdb.dto.{Point, Query, QueryResult}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import docs.javadsl.TestConstants.DATABASE_NAME
import akka.{Done, NotUsed}
import akka.stream.alpakka.influxdb.{InfluxDBSettings, InfluxDBWriteMessage}
import akka.stream.alpakka.influxdb.scaladsl.{InfluxDBSink, InfluxDBSource}
import akka.testkit.TestKit
import docs.javadsl.Cpu
import docs.javadsl.TestUtils._
import akka.stream.scaladsl.Sink

import scala.collection.JavaConverters._

class InfluxDBSpec extends WordSpec with MustMatchers with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  implicit var influxDB: InfluxDB = _

  override protected def beforeAll(): Unit =
    influxDB = setupConnection()

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit =
    populateDatabase(influxDB)

  override def afterEach() =
    cleanDatabase(influxDB)

  "support typed source" in assertAllStagesStopped {
    val query = new Query("SELECT*FROM cpu", DATABASE_NAME);
    val measurements = InfluxDBSource.typed(classOf[Cpu], InfluxDBSettings(), influxDB, query).runWith(Sink.seq)

    measurements.futureValue.map(_.getHostname) mustBe List("local_1", "local_2")
  }

  "InfluxDBFlow" should {

    "consume and publish measurements using typed" in assertAllStagesStopped {
      val query = new Query("SELECT*FROM cpu", DATABASE_NAME);

      val f1 = InfluxDBSource
        .typed(classOf[Cpu], InfluxDBSettings(), influxDB, query)
        .map { cpu: Cpu =>
          {
            val clonedCpu = cpu.cloneAt(cpu.getTime.plusSeconds(60000))
            InfluxDBWriteMessage(clonedCpu)
          }
        }
        .runWith(InfluxDBSink.typed(classOf[Cpu], InfluxDBSettings()))

      f1.futureValue mustBe Done

      val f2 = InfluxDBSource.typed(classOf[Cpu], InfluxDBSettings(), influxDB, query).runWith(Sink.seq)

      f2.futureValue.length mustBe 4
    }

    "consume and publish measurements" in assertAllStagesStopped {
      val query = new Query("SELECT*FROM cpu", DATABASE_NAME);

      val f1 = InfluxDBSource(influxDB, query)
        .map { queryReqult =>
          resultToPoints(queryReqult)
        }
        .mapConcat(identity)
        .runWith(InfluxDBSink(InfluxDBSettings()))

      f1.futureValue mustBe Done

      val f2 = InfluxDBSource.typed(classOf[Cpu], InfluxDBSettings(), influxDB, query).runWith(Sink.seq)

      f2.futureValue.length mustBe 4
    }

    def resultToPoints(queryResult: QueryResult): List[InfluxDBWriteMessage[Point, NotUsed]] = {
      val points = for {
        results <- queryResult.getResults.asScala
        serie <- results.getSeries.asScala
        values <- serie.getValues.asScala
      } yield
        (
          InfluxDBWriteMessage(resultToPoint(serie, values))
        )
      points.toList
    }

  }

}