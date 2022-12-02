package com.github.windymelt.psimackerel

import cats.effect.IO
import io.circe.Codec
import com.github.windymelt.psimackerel.MackerelClient.ServiceMetric
import io.circe.Encoder
import io.circe.Json
import com.github.windymelt.psimackerel.MackerelClient.GraphDefinition

class MackerelClient()(using client: org.http4s.client.Client[IO]):
  import org.http4s._
  import org.http4s.Method.POST
  import MackerelClient._
  import io.circe.Encoder
  import MackerelClient.given

  val jsonType = org.http4s.MediaType.application.json
  private def postReq(reqUri: Uri, json: io.circe.Json): IO [Unit] =
    import org.http4s.client.dsl.io._
    import org.http4s.headers._
    import org.http4s.circe._

    val req = POST(
    reqUri,
    `Content-Type`(jsonType),
    ).withEntity(json).withHeaders("X-Api-Key" -> "")
    client.successful(req) *> IO.unit

  def defineGraph(defs: Seq[GraphDefinition])(using GraphDefinitionEncoder: Encoder[GraphDefinition]): IO[Unit] =
    import org.http4s.implicits.uri
    import io.circe.syntax._
    postReq(uri"https://mackerelio.com/api/v0/graph-defs/create", defs.asJson)

  def postServiceMetrics(serviceName: String, metrics: Seq[ServiceMetric])(using ServiceMetricsEncoder: Encoder[ServiceMetric]): IO[Unit] =
    import org.http4s.implicits.uri
    import io.circe.syntax._
    postReq(uri"https://mackerelio.com/api/v0/services/" / serviceName / "tsdb", metrics.asJson)

object MackerelClient:
  import io.circe.generic.semiauto._
  import io.circe._
  import com.github.nscala_time.time.Imports._
  case class GraphDefinition(name: String, displayName: Option[String], unit: Option[String], metrics: Seq[Metric])
  case class Metric(name: String, displayName: Option[String], isStacked: Boolean)
  case class ServiceMetric(name: String, time: DateTime, value: Double)
  given EncoderForDateTime: Encoder[DateTime] = dt => Json.fromDouble(dt.getMillis() / 1000).get
