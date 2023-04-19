package com.github.windymelt.psimackerel

import cats.effect.IO
import io.circe.Json

enum Strategy:
  case Desktop, Mobile

class PSI():
  /** Fetches Page Speed Insight Score.
    *
    * @param targetUri
    * @param apiKey
    * @param client
    * @return
    *   Score [0, 1]
    */
  def fetchPsiScore(
      targetUri: org.http4s.Uri,
      apiKey: Option[String] = None,
      strategy: Strategy = Strategy.Desktop,
  )(using
      client: org.http4s.client.Client[IO],
  ): IO[Option[Double]] =
    import io.circe.syntax._
    import org.http4s.circe._
    import cats.syntax.applicative.catsSyntaxApplicativeId
    import io.circe.Encoder.encodeString
    val strategyString = strategy match
      case Strategy.Desktop => "desktop"
      case Strategy.Mobile  => "mobile"

    val url = apiKey match
      case Some(key) =>
        s"https://www.googleapis.com/pagespeedonline/v5/runPagespeed?key=$key&url=$targetUri&strategy=$strategyString"
      case None =>
        s"https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=$targetUri"

    val jIo: IO[Json] = client.expect[Json](url)
    for {
      j <- jIo
      scoreJson <- (for {
        res <- j \\ "lighthouseResult"
        cat <- res \\ "categories"
        perf <- cat \\ "performance"
      } yield perf \\ "score").flatten.head.pure[IO]
      score <- scoreJson.asNumber.map(_.toDouble).pure[IO]
    } yield score
