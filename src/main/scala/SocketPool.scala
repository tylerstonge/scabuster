package dr0pkick.pentest.scabuster

import java.net.{InetAddress, Socket}
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}


class SocketPool(val host: String, val port: Int, val size: Int) {
  SocketPool.queue = new LinkedBlockingQueue[Socket](size)

  def openSockets(): Unit = {
    println("[+] starting sockets")
    1 to size foreach { x => {
        val socket = new Socket(InetAddress.getByName(host), port)
        socket.setSoTimeout(1000)
        SocketPool.queue.add(socket)
      }
    }
  }

  def closeSockets(): Unit = {
    println("[+] closing sockets")
    SocketPool.queue.forEach(x => {
      x.close()
    })
  }

  def get(): Socket = {
    var socket = SocketPool.queue.take()
    if (!socket.isConnected) {
      if (Scabuster.verbose) println("found broken socket, reconnecting")
      socket.close()
      socket = new Socket(InetAddress.getByName(host), port)
    }
    socket
  }

  def put(s: Socket): Unit = {
    SocketPool.queue.add(s)
  }
}

object SocketPool {
  private var queue: BlockingQueue[Socket] = _
}
