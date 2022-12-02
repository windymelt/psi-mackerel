package com.github.windymelt.psimackerel

import cats.effect.{ResourceIO, IO, OutcomeIO}
import scala.concurrent.duration._
import cats.syntax.applicative._
import scala.language.postfixOps

object Util:
  def backgroundIndicator: ResourceIO[IO[OutcomeIO[Unit]]] = indicator.background
  private def indicator: IO[Unit] =
    IO.sleep(100 milliseconds) *> (IO.print("\r|") *> IO.sleep(100 milliseconds) *> IO.print("\r/") *> IO.sleep(100 milliseconds) *> IO.print("\r-") *> IO.sleep(100 milliseconds) *> IO.print("\r\\") *> IO.sleep(100 milliseconds)).foreverM

