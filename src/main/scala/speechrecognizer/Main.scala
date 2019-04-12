package speechrecognizer

import net.dv8tion.jda.core.{AccountType, JDABuilder}

object Main {
  val BOT_TOKEN = "NTYwMTcxNDM3NDUzNzM3OTg4.D3wEwg.liG6j9j49YQs3IqhR0PUUOOb0O4"

  def main(args: Array[String]): Unit = {
    print("Initial Output")
    val jdaBuilder = new JDABuilder(AccountType.BOT)
    jdaBuilder.setToken(BOT_TOKEN)
    jdaBuilder.addEventListener(new ServerListener())
    jdaBuilder.build()
  }
}
