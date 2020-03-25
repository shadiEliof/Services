package tooka.io.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.*
import java.net.URL

class MyService : Service() {
    var total: Long = 0
    var downloadLink = ""
    var downloadPath = ""

    companion object {
        const val ON_PREPARE = 100
        const val ON_PROGRESS = 101
        const val ON_SUCCESS = 102
        const val ON_FAIL = 103
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        downloadLink = intent!!.getStringExtra("downloadLink")
        downloadPath = intent!!.getStringExtra("downloadPath")
        val fileName =
            downloadLink.split("/").toTypedArray()[downloadLink.split("/").toTypedArray().size - 1]

        val thread = Thread(object : Runnable {
            override fun run() {
                var count: Int
                try {

                    val url = URL(downloadLink)
                    val connection = url.openConnection()
                    connection.connect()
                    val lengthOfFile = connection.contentLength

                    sendDownloadBroadcast(state = ON_PREPARE, lengthOfFile = lengthOfFile)


                    val input: InputStream = BufferedInputStream(url.openStream(), 8192)

                    val file = File(downloadPath)
                    if (!file.exists()) {
                        file.mkdirs()
                    }
                    val filePath = "$downloadPath/$fileName"
                    val output: OutputStream = FileOutputStream(filePath)
                    val data = ByteArray(1024)
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        val percent = (total * 100 / lengthOfFile).toInt()

                        sendDownloadBroadcast(ON_PROGRESS, percent)

                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()

                    sendDownloadBroadcast(ON_SUCCESS)

                } catch (e: Exception) {


                    sendDownloadBroadcast(ON_FAIL)
                }


            }
        })
        thread.start()


        return START_NOT_STICKY

    }

    private fun sendDownloadBroadcast(state: Int, percent: Int = 0, lengthOfFile: Int = 0) {
        val intent = Intent()
        intent.action = "tooka.io.services.downloadUpdater"
        intent.putExtra("STATE", state)
        intent.putExtra("percent", percent)
        intent.putExtra("lengthFile", lengthOfFile)
        sendBroadcast(intent)
    }
}