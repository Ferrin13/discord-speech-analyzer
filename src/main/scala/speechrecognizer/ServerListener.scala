package speechrecognizer

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import scala.collection.JavaConverters._

class ServerListener extends ListenerAdapter {

  override def onMessageReceived(event: MessageReceivedEvent) {
    val guild = event.getGuild
    val voiceChannels = guild.getVoiceChannels.asScala
    val authorizedVoiceChannel = voiceChannels.find(_.getMembers.asScala.map(_.getUser.getId).contains(Settings.AUTHORIZED_USER_ID))
    val audioManager = guild.getAudioManager

    if(event.getAuthor.getId == Settings.AUTHORIZED_USER_ID) {
      getCommand(event.getMessage.getContentRaw) match {
        case ("!start", _) =>
          ServerListener.recordingId = System.currentTimeMillis().toString
          audioManager.setSendingHandler(new SilenceAudioSendHandler()) //Solves Discord Bug
          audioManager.setReceivingHandler(new AudioRecorder())
          if(authorizedVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Starting audio recording").queue()
            ServerListener.currentChanelName = authorizedVoiceChannel.get.getName
            audioManager.openAudioConnection(authorizedVoiceChannel.get)
          }

        case ("!stop", _) =>
          val currentVoiceChannel = audioManager.getConnectedChannel
          audioManager.closeAudioConnection()
          event.getChannel.sendMessage("Stopping audio recording").queue()
          Commands.combine(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)
          Commands.transcribe(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!combine", args) =>
          Commands.combine(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!transcribe", args) =>
          Commands.transcribe(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case ("!analyze", args) =>
          Commands.analyze(args, voiceChannels, event.getChannel, ServerListener.currentChanelName)

        case _ => Unit
      }
    }
  }

  def getCommand(commandText: String): (String, List[String]) = {
    val tokens = commandText.split(' ')
    (tokens(0), tokens.slice(1, tokens.length).toList)
  }
}


object ServerListener {
  var recordingId: String = "-1"
  var currentChanelName: String = "CS436Testing"
}
