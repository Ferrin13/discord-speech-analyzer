package speechrecognizer

import net.dv8tion.jda.core.{AccountType, JDABuilder}

object Main {
  def main(args: Array[String]): Unit = {
    val jdaBuilder = new JDABuilder(AccountType.BOT)
    jdaBuilder.setToken(Settings.BOT_TOKEN)
    jdaBuilder.addEventListener(new ServerListener())
    jdaBuilder.build()
  }
}
