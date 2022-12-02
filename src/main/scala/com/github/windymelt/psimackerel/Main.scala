package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource, ExitCode}
import org.http4s.Uri
import org.http4s.curl.CurlApp
import org.http4s.client.Client
import com.monovore.decline._
import com.monovore.decline.effect._
import cats.implicits._

object Main
    extends CommandIOApp(
      name = "psi-mackerel",
      header = "Post Google Page Speed Insights score to Mackerel",
      version = "0.1.0"
    )
    with CurlApp:

  override def main: Opts[IO[ExitCode]] =
    CLIParameters.config map { config =>
      val epoch = java.time.Instant.now()
      val uri = Uri.fromString("https://www.3qe.us").toOption.get
      given client: Client[IO] = curlClient
      for
        score <- Util.backgroundIndicator("Fetching PSI score...") use { _ =>
          PSI().fetchPsiScore(uri, config.psiKey)
        }
        _ <- config.mackerelKey match
          case Some(mackerelKey) =>
            import MackerelClient.given
            import io.circe.Encoder.AsArray.importedAsArrayEncoder
            given mc: MackerelClient = MackerelClient(mackerelKey)
            for
              _ <- Util
                .backgroundIndicator("Defining Graph definition...")
                .use { _ => defineMackerelGraphDefinition(uri) }
              _ <- Util.backgroundIndicator("Posting service metrics...").use {
                _ =>
                  val safeUrl =
                    uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
                  mc.postServiceMetrics(
                    "WWW",
                    Seq(
                      MackerelClient.ServiceMetric(
                        s"custom.pagespeed.$safeUrl",
                        epoch,
                        score.get * 100
                      )
                    )
                  ) // TODO
              }
            yield ()
          case None => IO.unit
        _ <- IO.println("")
      yield cats.effect.ExitCode(0)
    }

  private def defineMackerelGraphDefinition(uri: Uri)(using
      mc: MackerelClient
  ): IO[Unit] =
    val safeUrl = uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
    mc.defineGraph(
      Seq(
        MackerelClient.GraphDefinition(
          name = "custom.pagespeed",
          displayName = uri.toString.some,
          unit = "percentage".some,
          metrics = Seq(
            MackerelClient.Metric(
              name = s"custom.pagespeed.$safeUrl",
              displayName = uri.toString.some,
              isStacked = false
            )
          )
        )
      )
    )
