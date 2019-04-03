import net.dv8tion.jda.core.audio.AudioSendHandler

//From https://github.com/wdavies973/VocalCord/blob/master/src/main/java/com/cpjd/speechGeneration/SilenceAudioSendHandler.java
object SilenceAudioSendHandler {
  private val silenceBytes = Array[Byte](0xF8.toByte, 0xFF.toByte, 0xFE.toByte)
}

class SilenceAudioSendHandler extends AudioSendHandler {
  private var _canProvide = true
  private val startTime = System.currentTimeMillis

  override def provide20MsAudio: Array[Byte] = { // send the silence only for 5 seconds
    if (((System.currentTimeMillis - startTime) / 1000) > 5) _canProvide = false
    println("Sending silence")
    SilenceAudioSendHandler.silenceBytes
  }

  override def canProvide: Boolean = _canProvide

  override def isOpus = true
}