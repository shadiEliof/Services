package tooka.io.services

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import tooka.io.utils.Converter
import tooka.io.utils.showToast


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 250
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("tooka.io.services.downloadUpdater")
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnStartService.setOnClickListener {
            if (!checkPermission()) {
                requestPermission()
            } else {
                downloadStart()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    fun downloadStart() {
        val downloadPath = "/sdcard/fastDl"
        //val downloadPath = this.getExternalFilesDir("fastDl")!!.absolutePath
        val intent = Intent(this@MainActivity, MyService::class.java)
        intent.putExtra(
            "downloadLink",
            "https://hw16.cdn.asset.aparat.com/aparat-video/be1844ca1fec7583ed80677419d78bcb19660635-480p__90599.mp4"
        )
        intent.putExtra("downloadPath", downloadPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this@MainActivity)
            .setMessage(message)
            .setPositiveButton("agree", okListener)
            .setNegativeButton("cancel", null)
            .create()
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty()) {
                val writeExternalStorageAccepted =
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (writeExternalStorageAccepted) {


                    showToast("Allow Access external Storage")

                    downloadStart()
                } else {

                    showToast("Deny Access external Storage")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                        showMessageOKCancel("you should access external storage",
                            DialogInterface.OnClickListener { dialog, which ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(
                                        arrayOf(WRITE_EXTERNAL_STORAGE),
                                        PERMISSION_REQUEST_CODE
                                    )
                                }
                            })
                        return
                    }
                }

            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1!!.action == "tooka.io.services.downloadUpdater") {

                val state = p1.getIntExtra("STATE", 0)

                if (state == MyService.ON_PREPARE) {
                    val length = p1.getIntExtra("lengthFile", 0)
                    val convert = Converter.humanReadableByteCountBin(length.toLong())
                    txtLength.text = convert.toString()
                    txtPercent.text="download prepare"
                } else if (state == MyService.ON_PROGRESS) {
                    val percent = p1.getIntExtra("percent", 0)
                   // txtPercent.text = percent.toString().plus("%")
                    txtPercent.text = "$percent %"
                    pBar.progress = percent
                } else if(state== MyService.ON_SUCCESS){
                   txtPercent.text="download complete"
                }else if (state==MyService.ON_FAIL){
                    txtPercent.text="download fail"
                    pBar.progress=0
                }


            }

        }

    }
}

