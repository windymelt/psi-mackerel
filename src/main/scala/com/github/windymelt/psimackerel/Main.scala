package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource}
import org.http4s.Uri
import org.http4s.curl.CurlApp
import org.http4s.client.Client

object Main extends cats.effect.IOApp with CurlApp:
  def run(args: List[String]) =
    val uri = Uri.fromString("https://www.3qe.us").toOption.get
    given client: Client[IO] = curlClient
    for
      _ <- IO.println("calling")
      score <- PSI().fetchPsiScore(uri)
      _ <- IO.println(s"ok: $score")
    yield cats.effect.ExitCode(0)

