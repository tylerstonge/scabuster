package dr0pkick.pentest.scabuster

import java.io.File
import java.util.concurrent.{ExecutorService, TimeUnit}

import scala.io.Source

object Scabuster {
  var verbose: Boolean = false
  private var threadPool: ExecutorService = _
  private var socketPool: SocketPool = _
  private var url: String = _
  private var list: String = _
  private var requestMethod: String = "HEAD"
  private var port: Int = 80
  private var threads: Int = 2
  private var sockets: Int = 2
  private var codes: Array[String] = Array("200", "204", "301", "302", "307")

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      print_banner()
      print_help()
      System.exit(0)
    }
    // parse all arguments
    args.foreach {
      parse_arguments
    }
    sanity_check()
    // finish configuring
    this.threadPool = java.util.concurrent.Executors.newFixedThreadPool(this.threads)
    this.socketPool = new SocketPool(this.url, this.port, this.sockets)
    // start the real program
    print_banner()
    execute()
  }

  def execute(): Unit = {
    socketPool.openSockets()
    val file = Source.fromFile(this.list)
    Requester.configure(this.url, this.requestMethod, this.codes, this.socketPool)
    for (line <- file.getLines()) {
      this.threadPool.submit(new Requester(line))
    }
    this.threadPool.shutdown()
    this.threadPool.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS)
    file.close()
    socketPool.closeSockets()
  }

  def parse_arguments(cmd: String): Unit = {
    val split = cmd.split("=")
    val option = split(0)
    option match {
      case "+url" => this.url = split(1);
      case "+list" => this.list = split(1);
      case "+method" => this.requestMethod = split(1);
      case "+port" => this.port = split(1).toInt;
      case "+threads" => this.threads = split(1).toInt;
      case "+sockets" => this.sockets = split(1).toInt;
      case "+v" | "+verbose" => this.verbose = true;
      case "+codes" => this.codes = split(1).split(",")
    }
  }

  def sanity_check(): Unit = {
    if (this.url.isEmpty) {
      error("[!] url was not specified, exiting...")
      System.exit(1)
    }
    if (this.list.startsWith("~" + File.separator)) {
      this.list = System.getProperty("user.home").concat(this.list.substring(1))
    }
    if (this.list.isEmpty) {
      error("[!] wordlist was not specified, exiting...")
      System.exit(1)
    }
  }

  def error(msg: String): Unit = {
    println(msg)
    System.exit(1)
  }

  def print_banner(): Unit = {
    println("  _____ _____ _____ _____ _____ _____ _____ _____ _____  ")
    println(" |   __|     |  _  | __  |  |  |   __|_   _|   __| __  | ")
    println(" |__   |   --|     | __ -|  |  |__   | | | |   __|    -| ")
    println(" |_____|_____|__|__|_____|_____|_____| |_| |_____|__|__| ")
    println("  ----- R A Z V R A T INC ----------------------------- ")
  }

  def print_help(): Unit = {
    println("Usage: scala scabuster.jar [options]")
    println("\t+url=URL\t\t\tthe url to scan")
    println("\t+list=LIST\t\t\tthe wordlist to use")
    println("\t+method=METHOD\t\t\thttp request method to use")
    println("\t+port=PORT\t\t\tport number of server")
    println("\t+threads=THREADS\t\tnumber of threads")
    println("\t+sockets=SOCKETS\t\tnumber of sockets")
    println("\t+codes=CODE(,CODE...)\t\tresponse codes to report")
    println("\t+v, +verbose\t\t\tchange verbosity")
  }
}
