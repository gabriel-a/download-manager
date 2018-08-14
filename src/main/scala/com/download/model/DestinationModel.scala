package com.download.model

import com.download.service.IOHelper

case class DestinationModel(finalDestination: String, tmpDestination: String)

object DestinationModel {
  def apply(finalDestination: String, tmpDestination: String) = {
    IOHelper.createDirIfNotExists(finalDestination)
    IOHelper.createDirIfNotExists(tmpDestination)
    new DestinationModel(finalDestination, tmpDestination)
  }

  def getProviderDestinations(folderName: String, destinationModel: DestinationModel): DestinationModel ={
    DestinationModel(s"${destinationModel.finalDestination}/${folderName}", s"${destinationModel.tmpDestination}/${folderName}")
  }
}
