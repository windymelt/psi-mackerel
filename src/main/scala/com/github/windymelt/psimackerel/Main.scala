package com.github.windymelt.psimackerel

import cats.effect.{IOApp, IO, ResourceIO, Resource, ExitCode}
import cats.effect.std.Console
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
      given client: Client[IO] = curlClient
      for
        uris <- extractUris(config.targetUris)
        fetchedCount <- cats.effect.Ref.of[IO, Int](0)
        scores <- Util.backgroundIndicatorWithCount("Fetching PSI score...", fetchedCount, uris.size) use { _ =>
          uris.map(uri => (PSI().fetchPsiScore(uri, config.psiKey) <* fetchedCount.update(_ + 1)).map(uri -> _)).parSequence // but scala native doesn't support multithreading yet
        }
        _ <- Console[IO].errorln("")
        _ <- Console[IO].errorln("All PSI scores are fetched")
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
                        config.mackerelService.get,
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
          case None => scores.map { s => IO.println(s"${s._2.map(_ * 100).getOrElse('?')}	${s._1}") }.sequence
        _ <- Console[IO].errorln("")
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

  private def extractUris(genericUris: NonEmptyList[java.net.URI] | java.nio.file.Path): IO[NonEmptyList[Uri]] =
    import fs2.io.file.{Files, Path}
    import fs2.text
    genericUris match
      case nel: NonEmptyList[java.net.URI] => nel.map(uri => Uri.fromString(uri.toString).right.get).pure
      case path: java.nio.file.Path =>
        val lisIo = Files[IO].readUtf8Lines(Path.fromNioPath(path)).filterNot(_.isBlank).map{ s => Uri.fromString(s).right.get }.compile.toList
        lisIo.map(lis => NonEmptyList.fromList(lis).get) // TODO: avoid get
