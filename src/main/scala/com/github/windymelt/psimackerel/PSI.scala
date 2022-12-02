package com.github.windymelt.psimackerel

import cats.effect.IO
import io.circe.Json

class PSI(apiUri: org.http4s.Uri):
  def fetchPsiScore(targetUri: org.http4s.Uri): IO[Option[Double]] =
    import io.circe.syntax._
    import org.http4s.circe._
    import cats.syntax.applicative.catsSyntaxApplicativeId
    PSI.client.use { c =>
      import io.circe.Encoder.encodeString
      val jIo: IO[Json] = c.expect[Json](s"https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=$targetUri")
      for {
        j <- jIo
        scoreJson <- (for {
          res <- j \\ "lighthouseResult"
          cat <- res \\ "categories"
          perf <- cat \\ "performance"
        } yield perf \\ "score").flatten.head.pure[IO]
        score <- scoreJson.asNumber.map(_.toDouble).pure[IO]
      } yield score
    }


object PSI:
  import org.http4s.ember.client._
  import org.http4s.client._
  val client = EmberClientBuilder.default[IO].build
