package dr0pkick.pentest.scabuster

import java.io.PrintStream
import java.net.Socket

import scala.io.BufferedSource
import scala.util.matching.Regex

class Requester(val dir: String) extends Runnable {

  override def run(): Unit = {
    var success: Boolean = false
    var attempts = 0
    var socket: Socket = Requester.pool.get()
    while (!success) {
      try {
        lazy val in = new BufferedSource(socket.getInputStream).getLines()
        val out = new PrintStream(socket.getOutputStream)

        out.print(Requester.request.format(Requester.method, dir, Requester.host))
        out.flush()

        val Requester.regex(responseCode) = in.next()
        if (Requester.codes.contains(responseCode)) {
          println("[+] FOUND /%s (STAT: %s)".format(dir, responseCode))
        }

        Requester.pool.put(socket)
        success = true
      } catch {
        case e: Throwable => {
          if (Scabuster.verbose) println("[!] error, retrying /%s".format(dir))
          if (attempts >= Requester.attempts) {
            println("[!] error, skipping /%s".format(dir))
            success = true
          } else {
            attempts += 1
            socket = new Socket(socket.getInetAddress, socket.getPort)
          }
        }
      }
    }
  }
}

object Requester {
  private val regex: Regex = """^(?:HTTP|http)\/\d\.\d\W(\d{3}).*$""".r
  private val request: String = "%s /%s HTTP/1.1\r\nHost: %s\r\n\r\n"
  private val attempts: Int = 3
  var host: String = _
  var method: String = _
  var codes: Array[String] = _
  var pool: SocketPool = _

  def configure(host: String, method: String, codes: Array[String], pool: SocketPool): Unit = {
    // TODO make this threadsafe, or just get ride of it. is it an advantage?
    Requester.host = host
    Requester.method = method
    Requester.codes = codes
    Requester.pool = pool
  }
}
