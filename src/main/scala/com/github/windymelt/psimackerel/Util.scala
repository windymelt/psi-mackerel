package com.github.windymelt.psimackerel

import cats.effect.{ResourceIO, IO, OutcomeIO}
import scala.concurrent.duration._
import cats.syntax.applicative._
import scala.language.postfixOps

object Util:
  def backgroundIndicator(msg: String): ResourceIO[IO[OutcomeIO[Unit]]] =
    indicator(msg).background
  private def indicator(m: String): IO[Unit] = (
    for
      _ <- IO.print(s"\r| $m")
      _ <- IO.sleep(100 milliseconds)
      _ <- IO.print(s"\r/ $m")
      _ <- IO.sleep(100 milliseconds)
      _ <- IO.print(s"\r- $m")
      _ <- IO.sleep(100 milliseconds)
      _ <- IO.print(s"\r\\ $m")
    yield IO.sleep(100 milliseconds)
  ).foreverM
