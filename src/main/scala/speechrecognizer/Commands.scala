package speechrecognizer

import net.dv8tion.jda.core.entities.{MessageChannel, VoiceChannel}
import net.dv8tion.jda.core.requests.restaction.MessageAction

import scala.collection.mutable

object Commands {
  def combine(args: List[String],
              voiceChannels: mutable.Buffer[VoiceChannel],
              messageChanel: MessageChannel,
              defaultChannelName: String
  ): MessageAction = {
    val channels = args.find(_.toLowerCase == "all").map{_ =>
      Some(voiceChannels.filter(vc => FileWriter.getChannelDirectoryNames.contains(vc.getName)).toList)
    }.getOrElse({
      val chanelName = args.find(_.toLowerCase != "all").getOrElse(defaultChannelName)
      voiceChannels.find(_.getName == chanelName).map(List(_))
    })
    val message = channels.map { channels =>
      messageChanel.sendMessage(s"Combining last audio recordings in ${channels.map(_.getName).mkString(", ")}\n").queue()
      channels.map{ channel =>
        val channelsUserIds = FileWriter.getUserIdsInChannel(channel.getName)
        val userDurations = ServerListener.userIds.filter(channelsUserIds.contains(_)).map { userId =>
          (ServerListener.userNameIdMap.get(userId), FileWriter.combineUserLastRecording(channel.getName, userId))
        }
        val messageBody = userDurations.map{ ud =>
          val userName = ud._1.getOrElse("Unknown User")
          ud._2 match {
            case Right(duration) => s"\tCombined $userName's audio in $duration milliseconds"
            case Left(s) => s"\tSkipping $userName because $s"
          }
        }.mkString("\n")

        val totalDuration = userDurations.map(_._2).map {
          case Right(duration) => duration
          case _ => 0
        }.sum

        s"Combined user audio for ${channel.getName} in $totalDuration ms: \n$messageBody"
      }.reduce((s1, s2) => s"$s1\n\n$s2")
    }
    messageChanel.sendMessage(message.getOrElse("Error finding channels"))
  }
}
