import net.dv8tion.jda.client.events.call.voice.{CallVoiceJoinEvent, CallVoiceLeaveEvent}
import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceJoinEvent, GuildVoiceUpdateEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import com.sedmelluq.discord.lavaplayer.player.{AudioLoadResultHandler, AudioPlayer, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack}

class DemoListener extends ListenerAdapter {
  override def onMessageReceived(event: MessageReceivedEvent) {
    val guild = event.getGuild
    val voiceChannel = guild.getVoiceChannels.get(0)
    val audioManager = guild.getAudioManager

    println(s"Message from ${event.getAuthor.getName}: ${event.getMessage.getContentDisplay}")
    println(s"Voice Channel: ${voiceChannel.getName}")

    event.getMessage.getContentRaw match {
      case "!start" =>
        event.getChannel.sendMessage("Starting audio recording").queue()
        audioManager.setSendingHandler(new SilenceAudioSendHandler()) //Solves Discord Bug
        audioManager.setReceivingHandler(new DemoAudioListener())
        audioManager.openAudioConnection(voiceChannel)
      case "!stop" =>
        audioManager.closeAudioConnection()
        event.getChannel.sendMessage("Stopping audio recording").queue()
      case "!play" =>
        event.getChannel.sendMessage("Playing audio").queue()
        audioManager.setSendingHandler(new DemoAudioSender(getPlayer()))
        audioManager.openAudioConnection(voiceChannel)
      case _ => println("Unknown command")
    }
  }



  override def onGuildVoiceJoin(event: GuildVoiceJoinEvent): Unit = {
    println(s"${event.getMember.getUser.getName} joined: ${event.getChannelJoined.getName}")
  }

  override def onGuildVoiceUpdate(event: GuildVoiceUpdateEvent): Unit = {
//    println(s"Guild Voice Updated: ${event.get}")

  }
//  override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = {
//    println("Message Received")
//    val voiceChannel = event.getMember.getVoiceState.getChannel
//    println(s"Voice Channel: ${voiceChannel.getId}, Members: ${asScalaBuffer(voiceChannel.getMembers).map(_.getNickname)} ")
//  }

  def getPlayer(): AudioPlayer = {
    val playerManager = new DefaultAudioPlayerManager
    AudioSourceManagers.registerRemoteSources(playerManager)
    val player = playerManager.createPlayer()
    val trackScheduler = new DemoTrackScheduler(player)
    playerManager.loadItem("nkqVm5aiC28", new AudioLoadResultHandler() {

      def trackLoaded(track: AudioTrack) {
        trackScheduler.queue(track)
      }

      def playlistLoaded(playlist: AudioPlaylist) {
        playlist.getTracks.forEach(x => trackScheduler.queue(x))
      }

      def noMatches() {
        println("No matches found")
      }

      def loadFailed(throwable: FriendlyException) {
        println(s"Load failed ${throwable.getMessage}")
      }
    })
    player
  }

}
