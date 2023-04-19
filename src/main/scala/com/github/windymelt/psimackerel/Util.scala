package com.github.windymelt.psimackerel

import cats.effect.IO
import cats.effect.OutcomeIO
import cats.effect.Ref
import cats.effect.ResourceIO
import cats.effect.std.Console
import cats.syntax.applicative._

import scala.concurrent.duration._
import scala.language.postfixOps

object Util:
  def backgroundIndicator(msg: String): ResourceIO[IO[OutcomeIO[Unit]]] =
    indicator(msg).background
  def backgroundIndicatorWithCount(
      msg: String,
      current: Ref[IO, Int],
      all: Int
  ): ResourceIO[IO[OutcomeIO[Unit]]] =
    indicatorWithCount(msg, current, all).background

  private def indicator(m: String): IO[Unit] = (
    for
      _ <- Console[IO].error(s"\r| $m")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r/ $m")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r- $m")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r\\ $m")
    yield IO.sleep(1 second)
  ).foreverM

  private def indicatorWithCount(
      m: String,
      current: Ref[IO, Int],
      all: Int
  ): IO[Unit] = (
    for
      c <- current.get
      _ <- Console[IO].error(s"\r| $m [$c / $all]")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r/ $m [$c / $all]")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r- $m [$c / $all]")
      _ <- IO.sleep(1 second)
      _ <- Console[IO].error(s"\r\\ $m [$c / $all]")
    yield IO.sleep(1 second)
  ).foreverM
