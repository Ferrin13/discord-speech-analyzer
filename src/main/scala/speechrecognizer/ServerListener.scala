package speechrecognizer

import com.sedmelluq.discord.lavaplayer.player.{AudioLoadResultHandler, AudioPlayer, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack}
import net.dv8tion.jda.core.entities.{MessageChannel, VoiceChannel}
import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceJoinEvent, GuildVoiceUpdateEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import collection.JavaConverters._

class ServerListener extends ListenerAdapter {
  type FailureString = String //Makes failure strings more clear


  override def onMessageReceived(event: MessageReceivedEvent) {
    val guild = event.getGuild
    val user = event.getAuthor
    val voiceChannels = guild.getVoiceChannels.asScala
    val ferrinVoiceChannel = voiceChannels.find(_.getMembers.asScala.map(_.getUser.getId).contains(ServerListener.FERRIN_USER_ID))
    val audioManager = guild.getAudioManager
    val voiceRecognizer = new VoiceRecognizer

    println(s"Message from ${event.getAuthor.getName}: ${event.getMessage.getContentDisplay}")
    println(s"Voice Channel: ${ferrinVoiceChannel.map(_.getName)}")

    if(event.getAuthor.getId == ServerListener.FERRIN_USER_ID) {
      getCommand(event.getMessage.getContentRaw) match {
        case ("!start", _) =>
          ServerListener.recordingId = System.currentTimeMillis().toString
          audioManager.setSendingHandler(new SilenceAudioSendHandler()) //Solves Discord Bug
          audioManager.setReceivingHandler(new AudioRecorder())
          if(ferrinVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Starting audio recording").queue()
            ServerListener.currentChanelName = ferrinVoiceChannel.get.getName
            audioManager.openAudioConnection(ferrinVoiceChannel.get)
          }

        case ("!stop", _) =>
          val currentVoiceChannel = audioManager.getConnectedChannel
          audioManager.closeAudioConnection()
          event.getChannel.sendMessage("Stopping audio recording").queue()
          Commands.combine(List(currentVoiceChannel.getName), voiceChannels, event.getChannel, ServerListener.currentChanelName).queue()

        //USAGE: !combine [channel] [all]
        case ("!combine", args) =>
          Commands.combine(args, voiceChannels, event.getChannel, ServerListener.currentChanelName).queue()
//          val channels = args.find(_.toLowerCase == "all").map(_ => Some(voiceChannels.toList)).getOrElse({
//            val chanelName = args.find(_.toLowerCase != "all").getOrElse(ServerListener.currentChanelName)
//            voiceChannels.find(_.getName == chanelName).map(List(_))
//          })
//          channels.map{ channels =>
//            event.getChannel.sendMessage(s"Combining last audio recordings in ${channels.map(_.getName).mkString(" ")}").queue()
//            channels.foreach { channel =>
//              val channelsUserIds = FileWriter.getUserIdsInChannel(channel.getName)
//              val userDurations = ServerListener.userIds.filter(channelsUserIds.contains(_)).map { userId =>
//                (ServerListener.userNameIdMap.get(userId), FileWriter.combineUserLastRecording(channel.getName, userId))
//              }
//              val messageBody = userDurations.map(ud =>  s"\t${ud._1.getOrElse("Unknown User")}'s audio in ${ud._2} milliseconds").mkString("\n")
//              event.getChannel.sendMessage(s"Combined user audio for ${channel.getName} in ${userDurations.map(_._2).sum} ms: \n$messageBody").queue()
//            }
//          }.getOrElse(event.getChannel.sendMessage("Error finding channels"))

        case ("!play", _) =>
          audioManager.setSendingHandler(new DemoAudioSender(getPlayer()))
          if(ferrinVoiceChannel.isEmpty) {
            event.getChannel.sendMessage("Target userId not in a voice channel").queue()
          } else {
            event.getChannel.sendMessage("Playing audio").queue()
            audioManager.openAudioConnection(ferrinVoiceChannel.get)
          }

        case ("!transcribe", _) =>
          event.getChannel.sendMessage("Currently Disabled")
        //        event.getChannel.sendMessage("Beginning to transcribe audio").queue()
        //        voiceRecognizer.transcribeFile("RecordedAudio;123203630193967104_audio_1554578124244.wav")
        //        event.getChannel.sendMessage("Audio transcription finished").queue()

        case _ => println("Unknown command")
      }
    }
  }


  override def onGuildVoiceJoin(event: GuildVoiceJoinEvent): Unit = {
    println(s"${event.getMember.getUser.getName} joined: ${event.getChannelJoined.getName}")
  }

  override def onGuildVoiceUpdate(event: GuildVoiceUpdateEvent): Unit = {
//    println(s"Guild Voice Updated: ${event.get}")

  }

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

  def getCommand(commandText: String): (String, List[String]) = {
    val tokens = commandText.split(' ')
    (tokens(0), tokens.slice(1, tokens.length).toList)
  }
}


object ServerListener {
  val FERRIN_USER_ID = "123203630193967104"
  val FIIRCE_USER_ID = "358670104424087554"
  val RR_USER_ID = "366695216511713281"
  val NOBLE_USER_ID = "276407390394515456"
  val STORM_USER_ID = "302528107603165185"
  val SLEEPY_USER_ID = "222210116655513610"

  val userIds = List(FERRIN_USER_ID, FIIRCE_USER_ID, RR_USER_ID, NOBLE_USER_ID, STORM_USER_ID, SLEEPY_USER_ID)
  val userNameIdMap: Map[String, String] = Map(
    FERRIN_USER_ID -> "Ferrin",
    RR_USER_ID -> "RandomRedneck",
    FIIRCE_USER_ID -> "Fiirce",
    NOBLE_USER_ID -> "Noble",
    STORM_USER_ID -> "Storm",
    SLEEPY_USER_ID -> "Sleepy"
  )

  var recordingId: String = "-1"
  var currentChanelName: String = "CS436Testing"
}
