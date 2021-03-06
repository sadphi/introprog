// kod till facit
// run in terminal with:       scala sunserver.fibserver.memcach.start
package sunserver.fibserver.memcach

import scala.util.Try
import java.io.OutputStream
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.{
  Headers, HttpExchange, HttpHandler, HttpServer
}

object Html:
  def minimalWebPage(body: String, title: String = "Min Sörver") = 
    s"""<!DOCTYPE html>
       |<html>
       |<head><meta charset="UTF-8">
       |<title>$title</title>
       |</head> 
       |<body>
       |$body
       |</body>
       |</html>
       """.stripMargin

object start:
  def main(args: Array[String]): Unit =
    val server = HttpServer.create(new InetSocketAddress(8080), 0)
    server.createContext("/", new FibHandler)
    server.setExecutor(Executors.newCachedThreadPool)
    server.start
    println("Surfa till servern på http://localhost:8080/akrug" )

object fib:
  import java.util.concurrent.ConcurrentHashMap
  val memcache = new ConcurrentHashMap[BigInt, BigInt]
  
  def apply(n: BigInt): BigInt = 
    if memcache.containsKey(n) then 
      println("CACHE HIT!!! FOUND: " + n)
      memcache.get(n)
    else
      println("cache miss :( could not find: " + n)
      val f = fastFib(n)
      memcache.put(n, f)
      f
  
  private def fastFib(n: BigInt): BigInt =
    if n < 0 then 0 else 
    if n == 1 || n == 2 then 1  
    else apply(n - 1) + apply(n -2)

class FibHandler extends HttpHandler:
  def handle(exchange: HttpExchange ): Unit =
    val requestMethod: String = exchange.getRequestMethod();
    if requestMethod.equalsIgnoreCase("GET") then
      val uri = exchange.getRequestURI.toString
      println(s"GET URI='$uri'")
      val response = if uri.toLowerCase.startsWith("/fib") then Try[String] {
        val n = uri.drop(5).toInt
        s"fib($n) == ${fib(n)}"
      }.getOrElse("SKRIV ETT TAL") else "FATTAR NOLL"
      val htmlResponse = 
        Html.minimalWebPage(response, "Fibonacci")
      println("RESPONSE:\n" + htmlResponse)
      val responseHeaders: Headers = exchange.getResponseHeaders
      responseHeaders.set("Content-Type", "text/html")
      exchange.sendResponseHeaders(200, 0)
      val responseBody: OutputStream  = exchange.getResponseBody
      responseBody.write(htmlResponse.getBytes)
      responseBody.close
