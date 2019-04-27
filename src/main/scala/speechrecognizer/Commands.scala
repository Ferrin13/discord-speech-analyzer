package speechrecognizer

import net.dv8tion.jda.core.entities.{MessageChannel, VoiceChannel}
import net.dv8tion.jda.core.requests.restaction.MessageAction

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object Commands {
  implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  def combine(args: List[String],
              voiceChannels: mutable.Buffer[VoiceChannel],
              messageChanel: MessageChannel,
              defaultChannelName: String
  ): Unit = {
    val channels = getChannelsFromArgs(args, voiceChannels, defaultChannelName)
    val message = channels.map { channels =>
      messageChanel.sendMessage(s"Combining last audio recordings in ${channels.map(_.getName).mkString(", ")}\n").queue()
      channels.map{ channel =>
        val channelsUserIds = Utils.getUserIdsInChannelFolder(channel.getName)
        val userDurations = ServerListener.userIds.filter(channelsUserIds.contains(_)).map { userId =>
          (ServerListener.userNameIdMap.get(userId), AudioFileWriter.combineUserLastRecording(channel.getName, userId))
        }
        val messageBody = userDurations.map{ ud =>
          val userName = ud._1.getOrElse("Unknown User")
          ud._2 match {
            case Right(duration) => s"\tCombined $userName's audio in $duration milliseconds"
            case Left(s) => s"\tSkipping $userName because ${s.toLowerCase}"
          }
        }.mkString("\n")

        val totalDuration = userDurations.map(_._2).map {
          case Right(duration) => duration
          case _ => 0
        }.sum

        s"Combined user audio for ${channel.getName} in $totalDuration ms: \n$messageBody"
      }.reduce((s1, s2) => s"$s1\n\n$s2")
    }
    messageChanel.sendMessage(message.getOrElse("Error finding channels")).queue()
  }

  def transcribe(args: List[String],
              voiceChannels: mutable.Buffer[VoiceChannel],
              messageChanel: MessageChannel,
              defaultChannelName: String
  ): Unit = {
    val channelsOption = getChannelsFromArgs(args, voiceChannels, defaultChannelName)
    val messageFutureOption = channelsOption.map { channels =>
      messageChanel.sendMessage(s"Transcribing last audio recordings in ${channels.map(_.getName).mkString(", ")}\n").queue()
      Future.sequence( channels.map { channel =>
        val startTime = System.currentTimeMillis()
        val channelsUserIds = Utils.getUserIdsInChannelFolder(channel.getName)
        val userDurations = ServerListener.userIds.filter(channelsUserIds.contains(_)).map { userId =>
          (ServerListener.userNameIdMap.get(userId), VoiceRecognizer.transcribeUserLastRecording(channel.getName, userId))
        }
        val messageBodyFuture = Future.sequence(userDurations.map{ ud =>
          val userName = ud._1.getOrElse("Unknown User")
          ud._2.map {
            case Right(duration) => s"\tUploaded and transcribed $userName's audio in ${Utils.msAsSeconds(duration)} "
            case Left(s) => s"\tSkipping $userName because ${s.toLowerCase}"
          }
        }).map(_.mkString("\n"))

        messageBodyFuture.map{ messageBody =>
          val totalDuration = System.currentTimeMillis() - startTime
          s"Transcribed user audio for ${channel.getName} in ${Utils.msAsSeconds(totalDuration)}: \n$messageBody"
        }
      }).map(_.reduce((s1, s2) => s"$s1\n\n$s2"))
    }
    val message = Await.result(messageFutureOption.getOrElse(Future.successful("Error finding channels")), Duration.Inf)
    messageChanel.sendMessage(message).queue()
  }

  private def getChannelsFromArgs(args: List[String],
                                  voiceChannels: mutable.Buffer[VoiceChannel],
                                  defaultChannelName: String
  ): Option[List[VoiceChannel]] = {
    args.find(_.toLowerCase == "all").map{_ =>
      Some(voiceChannels.filter(vc => Utils.getChannelDirectoryNames.contains(vc.getName)).toList)
    }.getOrElse({
      val chanelName = args.find(_.toLowerCase != "all").getOrElse(defaultChannelName)
      voiceChannels.find(_.getName == chanelName).map(List(_))
    })
  }
}
