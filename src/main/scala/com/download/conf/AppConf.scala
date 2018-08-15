package com.download.conf

import com.typesafe.config.{Config, ConfigFactory}
import ProviderProtocolType.ProtocolType
import com.download.model.DestinationModel

import scala.collection.JavaConverters._

case class ProviderConfig(id: String,
                          host: String,
                          port: Int,
                          protocol: ProtocolType,
                          interval: Int,
                          maxConcurrentConnections: Int,
                          basePath: String,
                          allowedExt: List[String],
                          username: String,
                          password: String)

case class AppConf(providers: List[ProviderConfig], destination: DestinationModel)

object AppConf {
  val DownloadManagerActorSystemName = "download-manager"
  def apply(): AppConf = apply(ConfigFactory.load.getConfig("app-conf"))

  def apply(config: Config): AppConf = {
    new AppConf(
      config.getConfigList("providers").asScala.map(provider =>
        ProviderConfig(
          provider.getString("uid").trim,
          provider.getString("host").trim,
          provider.getInt("port"),
          ProviderProtocolType.withNameWithDefault(provider.getString("protocol")),
          provider.getInt("interval"),
          provider.getInt("max-concurrent-connections"),
          provider.getString("base-path").trim,
          provider.getString("allowed-ext").split(",").toList.map(_.trim),
          provider.getString("username").trim,
          provider.getString("password").trim
        )).toList,
      DestinationModel(config.getString("final-destination"), config.getString("tmp-destination"))
    )
  }

  def isAllowedExt(source: String, allowedExtensions: List[String]): Boolean = {
    if (allowedExtensions.head == "*") return true
    allowedExtensions.exists(endsWith => source.endsWith(endsWith))
  }
}