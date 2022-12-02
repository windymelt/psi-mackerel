package com.github.windymelt.psimackerel

import cats.effect.IO
import io.circe.Json

class PSI():
  def fetchPsiScore(targetUri: org.http4s.Uri)(using client: org.http4s.client.Client[IO]): IO[Option[Double]] =
    import io.circe.syntax._
    import org.http4s.circe._
    import cats.syntax.applicative.catsSyntaxApplicativeId
    import io.circe.Encoder.encodeString
    val jIo: IO[Json] = client.expect[Json](s"https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=$targetUri")
    for {
      j <- jIo
      scoreJson <- (for {
        res <- j \\ "lighthouseResult"
        cat <- res \\ "categories"
        perf <- cat \\ "performance"
      } yield perf \\ "score").flatten.head.pure[IO]
      score <- scoreJson.asNumber.map(_.toDouble).pure[IO]
    } yield score
