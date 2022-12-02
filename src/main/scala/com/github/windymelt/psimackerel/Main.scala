package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource, ExitCode}
import org.http4s.Uri
import org.http4s.curl.CurlApp
import org.http4s.client.Client
import com.monovore.decline._
import com.monovore.decline.effect._
import cats.implicits._

case class Config(psiKey: Option[String], mackerelKey: Option[String])

object Main extends CommandIOApp(name = "psi-mackerel", header = "Post Google Page Speed Insights score to Mackerel", version = "0.1.0")
    with CurlApp:
  val apiKeyForPsi = Opts.option[String]("psi-api-key", metavar = "API_KEY_FOR_PSI", help = "Optional: Set PSI API key (can get yours at https://developers.google.com/speed/docs/insights/v5/get-started). API may return 429 Too Many Requests error unless you give no API key.").orNone
  val apiKeyForMackerel = Opts.option[String]("mackerel-api-key", short = "k", metavar = "API_KEY_FOR_MACKEREL", help = "Set Mackerel API key").orNone
  val config = (apiKeyForPsi, apiKeyForMackerel).mapN(Config.apply)

  override def main: Opts[IO[ExitCode]] =
    config map { config =>
      val epoch = java.time.Instant.now()
      val uri = Uri.fromString("https://www.3qe.us").toOption.get
      given client: Client[IO] = curlClient
      for
        score <- Util.backgroundIndicator("Fetching PSI score...") use { _ => PSI().fetchPsiScore(uri, config.psiKey) }
        _ <- IO.println(s"ok: $score")
        _ <- config.mackerelKey match
          case Some(mackerelKey) =>
            import MackerelClient.given
            import io.circe.Encoder.AsArray.importedAsArrayEncoder
            given mc: MackerelClient = MackerelClient(mackerelKey)
            for
              _ <- Util.backgroundIndicator("Defining Graph definition...").use { _ => defineMackerelGraphDefinition(uri) }
              _ <- Util.backgroundIndicator("Posting service metrics...").use { _ =>
                val safeUrl = uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
                mc.postServiceMetrics("WWW", Seq(MackerelClient.ServiceMetric(s"custom.pagespeed.$safeUrl", epoch, score.get * 100))) // TODO
              }
            yield ()
          case None => IO.unit
      yield cats.effect.ExitCode(0)
    }

  private def defineMackerelGraphDefinition(uri: Uri)(using mc: MackerelClient): IO[Unit] =
    val safeUrl = uri.toString.replaceAll("""[^a-zA-Z0-9_\-]""", "-")
    mc.defineGraph(Seq(
      MackerelClient.GraphDefinition(
        name = "custom.pagespeed", displayName = uri.toString.some, unit = "percentage".some,
        metrics = Seq(MackerelClient.Metric(name = s"custom.pagespeed.$safeUrl", displayName = uri.toString.some, isStacked = false))
      )
    ))
