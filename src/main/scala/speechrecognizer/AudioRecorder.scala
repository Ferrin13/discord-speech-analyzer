package speechrecognizer

import net.dv8tion.jda.core.audio.{AudioReceiveHandler, CombinedAudio, UserAudio}

import scala.collection.mutable

class AudioRecorder extends AudioReceiveHandler {
  private final val DEFAULT_VOLUME = 1.0
  private final val BYTES_PER_SECOND = 192000
  private final val BYTES_PER_PACKET = 3840
  private final val SECONDS_PER_SECTION = 10
  private final val PACKETS_PER_SECOND = 50

  var fileWritten = false
  val userPacketIndexMap: mutable.HashMap[String, Int]  = new mutable.HashMap()
  val userAudioSectionMap: mutable.HashMap[String, Array[Byte]] = new mutable.HashMap()

  override def canReceiveCombined: Boolean = false

  override def canReceiveUser: Boolean = true

  override def handleCombinedAudio(combinedAudio: CombinedAudio): Unit = Unit

  override def handleUserAudio(userAudio: UserAudio): Unit = {
    val pcmPacket = userAudio.getAudioData(DEFAULT_VOLUME)
    val userId = userAudio.getUser.getId
    val currentPacketIndex = userPacketIndexMap.getOrElseUpdate(userId, 0)
    val audioSection = userAudioSectionMap.getOrElseUpdate(userId, new Array[Byte](BYTES_PER_SECOND * SECONDS_PER_SECTION))

    System.arraycopy(pcmPacket, 0, audioSection, currentPacketIndex * BYTES_PER_PACKET, BYTES_PER_PACKET)
    userPacketIndexMap.update(userId, currentPacketIndex + 1)

    if(currentPacketIndex + 1 == PACKETS_PER_SECOND * SECONDS_PER_SECTION) {
      AudioFileWriter.writeWavFile(userAudio.getUser.getId, audioSection, ServerListener.currentChanelName, ServerListener.recordingId)
      userPacketIndexMap.update(userId, 0)
    }
  }
}
