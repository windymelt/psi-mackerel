package com.github.windymelt.psimackerel

import cats.effect.IO
import io.circe.Codec
import com.github.windymelt.psimackerel.MackerelClient.ServiceMetric
import io.circe.Encoder
import io.circe.Json
import com.github.windymelt.psimackerel.MackerelClient.GraphDefinition

class MackerelClient(apiKey: String)(using
    client: org.http4s.client.Client[IO]
):
  import org.http4s._
  import org.http4s.Method.POST
  import MackerelClient._
  import io.circe.Encoder
  import MackerelClient.given

  val jsonType = org.http4s.MediaType.application.json
  private def postReq(reqUri: Uri, json: io.circe.Json): IO[Unit] =
    import org.http4s.client.dsl.io._
    import org.http4s.headers._
    import org.http4s.circe._

    val req = POST(
      reqUri,
      `Content-Type`(jsonType),
      Header("X-Api-Key", apiKey)
    ).withEntity(
      json
    ) // Don't .toString(): it modifies Content-Type into text/plain

    client.expectOr[SuccessfulResponse](req) { res =>
      for msg <- res.body.compile.toVector
          .map(_.toArray)
          .map(arr => String(arr))
      yield new Exception(msg)
    }(jsonOf[IO, SuccessfulResponse]) *> IO.unit

  def defineGraph(defs: Seq[GraphDefinition])(using
      GraphDefinitionEncoder: Encoder[GraphDefinition]
  ): IO[Unit] =
    import org.http4s.implicits.uri
    import io.circe.syntax._
    postReq(
      uri"https://api.mackerelio.com/api/v0/graph-defs/create",
      defs.asJson
    )

  def postServiceMetrics(serviceName: String, metrics: Seq[ServiceMetric])(using
      ServiceMetricsEncoder: Encoder[ServiceMetric]
  ): IO[Unit] =
    import org.http4s.implicits.uri
    import io.circe.syntax._
    postReq(
      uri"https://api.mackerelio.com/api/v0/services/" / serviceName / "tsdb",
      metrics.asJson
    )

object MackerelClient:
  import io.circe.generic.semiauto._
  import io.circe._
  import java.time.Instant
  case class GraphDefinition(
      name: String,
      displayName: Option[String],
      unit: Option[String],
      metrics: Seq[Metric]
  )
  case class Metric(
      name: String,
      displayName: Option[String],
      isStacked: Boolean
  )
  case class ServiceMetric(name: String, time: Instant, value: Double)
  case class SuccessfulResponse(success: true)
  // Encoder
  given EncoderForDateTime: Encoder[Instant] = ins =>
    Json.fromDouble(ins.getEpochSecond()).get
  given EncoderForServiceMetric: Encoder[ServiceMetric] =
    deriveEncoder[ServiceMetric]
  given EncoderForMetric: Encoder[Metric] = deriveEncoder[Metric]
  given EncoderForGraphDefinition: Encoder[GraphDefinition] =
    deriveEncoder[GraphDefinition]
  // Decoder
  given DecoderForSuccessfulResponse: Decoder[SuccessfulResponse] =
    deriveDecoder[SuccessfulResponse]
