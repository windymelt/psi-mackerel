package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource, ExitCode}
import org.http4s.Uri
import org.http4s.curl.CurlApp
import org.http4s.client.Client
import com.monovore.decline._
import com.monovore.decline.effect._

object Main extends CommandIOApp(name = "psi-mackerel", header = "Post Google Page Speed Insights score to Mackerel", version = "0.1.0")
    with CurlApp:
  val apiKeyForPsi = Opts.option[String]("psi-api-key", metavar = "API_KEY_FOR_PSI", help = "Optional: Set PSI API key (can get yours at https://developers.google.com/speed/docs/insights/v5/get-started). API may return 429 Too Many Requests error unless you give no API key.").orNone
  override def main: Opts[IO[ExitCode]] =
    apiKeyForPsi map { psiKey =>
      val uri = Uri.fromString("https://www.3qe.us").toOption.get
      given client: Client[IO] = curlClient
      for
        score <- Util.backgroundIndicator("Fetching PSI score...") use { _ => PSI().fetchPsiScore(uri, psiKey) }
        _ <- IO.println(s"ok: $score")
      yield cats.effect.ExitCode(0)
    }

