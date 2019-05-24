package at.chaosfield.packupdate

object Main {
  def main(args: Array[String]) = {
    if (args.contains("--server")) {
      ServerMain.main(args.filter(a => a != "--server"))
    } else {
      ClientMain.main(args)
    }
  }
}
