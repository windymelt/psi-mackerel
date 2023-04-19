package com.github.windymelt.psimackerel

import cats.data.NonEmptyList
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._
import fansi.Color

import java.net.URI
import java.nio.file.Path

object CLIParameters:
  case class Config(
      psiKey: Option[String],
      mackerelKey: Option[String],
      mackerelService: Option[String],
      targetUris: NonEmptyList[URI] | Path,
      userAgent: Option[String]
  )

  // Main object以外の場所でvalにすると壊れる!!のでここだけdefとしている
  def config =
    (
      apiKeyForPsi,
      apiKeyForMackerel,
      mackerelServiceName,
      genericTargetUris,
      userAgent
    )
      .mapN(Config.apply)

  val apiKeyForPsi = Opts
    .option[String](
      "psi-api-key",
      metavar = "API_KEY",
      help =
        "Optional: Set PSI API key (can get yours at https://developers.google.com/speed/docs/insights/v5/get-started). API may return 429 Too Many Requests error unless you give no API key."
    )
    .orNone

  val apiKeyForMackerel = Opts
    .option[String](
      "mackerel-api-key",
      short = "k",
      metavar = "API_KEY",
      help = "Set Mackerel API key."
    )
    .orNone

  val mackerelServiceName = Opts
    .option[String](
      "service",
      short = "s",
      metavar = "service_name",
      help = "Set Mackerel service name."
    )
    .orNone

  def genericTargetUris: Opts[NonEmptyList[URI] | Path] =
    targetUriFile orElse targetUris

  val targetUris = Opts
    .arguments[URI]("target uri")
    .validate(
      errorString(
        "URI should contain scheme (HTTP / HTTPS). (Try prepending https://)"
      )
    )(nel => nel.forall(u => Seq("http", "https") contains (u.getScheme())))

  val targetUriFile = Opts.option[Path](
    "target-list",
    "Target URI list: one URI per line. Precedes by-argument URI list.",
    "f",
    "target list file"
  )

  val userAgent =
    Opts
      .option[String]("user-agent", "User Agent for request", "A", "agent")
      .orNone

private def errorString(s: String): String = fansi.Color.Red(s).toString
