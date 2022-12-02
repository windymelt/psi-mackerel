package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource, ExitCode}
import org.http4s.Uri
import org.http4s.curl.CurlApp
import org.http4s.client.Client
import com.monovore.decline._
import com.monovore.decline.effect._
import cats.implicits._
import cats.data.NonEmptyList

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
      val uris = config.targetUris.map(uri => Uri.fromString(uri.toString).right.get)
      given client: Client[IO] = curlClient
      for
        scores <- Util.backgroundIndicator("Fetching PSI score...") use { _ =>
          uris.map(uri => PSI().fetchPsiScore(uri, config.psiKey).map(uri -> _)).parSequence // but scala native doesn't support multithreading yet
        }
        _ <- config.mackerelKey match
          case Some(mackerelKey) =>
            import MackerelClient.given
            import io.circe.Encoder.AsArray.importedAsArrayEncoder
            given mc: MackerelClient = MackerelClient(mackerelKey)
            for
              _ <- Util
                .backgroundIndicator("Defining Graph definition...")
                .use { _ => defineMackerelGraphDefinition(uris)  }
              _ <- Util.backgroundIndicator("Posting service metrics...").use {
                _ =>
                scores.map {
                  case (uri, Some(score)) =>
                    val safeUrl =
                        uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
                    mc.postServiceMetrics(
                        "WWW",
                        Seq(
                            MackerelClient.ServiceMetric(
                                s"custom.pagespeed.$safeUrl",
                                epoch,
                                score * 100
                            )
                        )
                  )
                  case (uri, None) => IO.unit// nop
                }.parSequence
              }
            yield ()
          case None => IO.unit
        _ <- IO.println("")
      yield cats.effect.ExitCode(0)
    }

  private def defineMackerelGraphDefinition(uris: NonEmptyList[Uri])(using
      mc: MackerelClient
  ): IO[Unit] =
    val metrics = uris.map { uri =>
      val safeUrl = uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
      MackerelClient.Metric(
        name = s"custom.pagespeed.$safeUrl",
        displayName = uri.toString.some,
        isStacked = false
      )
    }.toList
    val graph = MackerelClient.GraphDefinition(
      name = "custom.pagespeed",
      unit = "percentage".some,
      metrics = metrics,
    )
    mc.defineGraph(graph.pure[Seq])
