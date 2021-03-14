/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc

import akka.actor.ClassicActorSystemProvider
import akka.stream.alpakka.googlecloud.pubsub.grpc.impl.GrpcCredentials
import com.typesafe.config.Config
import io.grpc.CallCredentials

/**
 * Connection settings used to establish Pub/Sub connection.
 */
final class PubSubSettings private (
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val rootCa: Option[String],
    val callCredentials: Option[CallCredentials]
) {

  /**
   * Endpoint hostname where the gRPC connection is made.
   */
  def withHost(host: String): PubSubSettings = copy(host = host)

  /**
   * Endpoint port where the gRPC connection is made.
   */
  def withPort(port: Int): PubSubSettings = copy(port = port)

  /**
   * A filename on the classpath which contains the root certificate authority
   * that is going to be used to verify certificate presented by the gRPC endpoint.
   */
  def withRootCa(rootCa: String): PubSubSettings =
    copy(rootCa = Some(rootCa))

  /**
   * Credentials that are going to be used for gRPC call authorization.
   */
  def withCallCredentials(callCredentials: CallCredentials): PubSubSettings =
    copy(callCredentials = Some(callCredentials))

  private def copy(host: String = host,
                   port: Int = port,
                   useTls: Boolean = useTls,
                   rootCa: Option[String] = rootCa,
                   callCredentials: Option[CallCredentials] = callCredentials) =
    new PubSubSettings(host, port, useTls, rootCa, callCredentials)
}

object PubSubSettings {

  /**
   * Create settings for unsecure (no tls), unauthenticated (no root ca)
   * and unauthorized (no call credentials) endpoint.
   */
  def apply(host: String, port: Int): PubSubSettings =
    new PubSubSettings(host, port, false, None, None)

  /**
   * Create settings from config instance.
   */
  def apply(config: Config): PubSubSettings =
    new PubSubSettings(
      config.getString("host"),
      config.getInt("port"),
      config.getBoolean("use-tls"),
      Some(config.getString("rootCa")).filter(_ != "none"),
      config.getString("callCredentials") match {
        case "google-application-default" => Some(GrpcCredentials.applicationDefault())
        case _ => None
      }
    )

  /**
   * Create settings from the new actor API's ActorSystem config.
   */
  def apply(system: ClassicActorSystemProvider): PubSubSettings = apply(system.classicSystem)

  /**
   * Create settings from a classic ActorSystem's config.
   */
  def apply(system: akka.actor.ActorSystem): PubSubSettings =
    PubSubSettings(system.settings.config.getConfig("alpakka.google.cloud.pubsub.grpc"))

  /**
   * Java API
   *
   * Create settings for unsecure (no tls), unauthenticated (no root ca)
   * and unauthorized (no call credentials) endpoint.
   */
  def create(host: String, port: Int): PubSubSettings =
    PubSubSettings(host, port)

  /**
   * Java API
   *
   * Create settings from config instance.
   */
  def create(config: Config): PubSubSettings =
    PubSubSettings(config)

  /**
   * Java API
   *
   * Create settings from ActorSystem's config.
   */
  def create(system: ClassicActorSystemProvider): PubSubSettings = PubSubSettings(system.classicSystem)

  /**
   * Java API
   *
   * Create settings from a classic ActorSystem's config.
   */
  def create(system: akka.actor.ActorSystem): PubSubSettings =
    PubSubSettings(system)
}
