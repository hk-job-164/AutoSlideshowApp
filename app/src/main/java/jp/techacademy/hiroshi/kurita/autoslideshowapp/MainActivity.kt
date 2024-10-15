package jp.techacademy.hiroshi.kurita.autoslideshowapp

import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import jp.techacademy.hiroshi.kurita.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var timer: Timer? = null
    private var handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val PERMISSIONS_REQUEST_CODE = 100
    private var cursor: Cursor? = null

    // APIレベルによって許可が必要なパーミッションを切り替える
    private val readImagesPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (checkSelfPermission(readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            getContentsInfo()
        } else {
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }

        binding.prevButton.setOnClickListener {
            getPrevContentsInfo()
        }

        binding.nextButton.setOnClickListener {
            getNextContentsInfo()
        }

        binding.startPauseButton.setOnClickListener {
            if (timer == null) {
                startSlideshow()
                binding.startPauseButton.text = "停止"
                binding.prevButton.isEnabled = false
                binding.nextButton.isEnabled = false
                isRunning = true
            } else {
                stopSlideshow()
                binding.startPauseButton.text = "再生"
                binding.prevButton.isEnabled = true
                binding.nextButton.isEnabled = true
                isRunning = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cursor?.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                } else {
                    showAlert()
                }
        }
    }

    private fun getContentsInfo() {
        cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor!!.moveToFirst()) {
            setImageView()
        }
    }

    private fun getPrevContentsInfo() {
        if (cursor!!.moveToPrevious()) {
            setImageView()
        } else if (cursor!!.moveToLast()) {
            setImageView()
        }
    }

    private fun getNextContentsInfo() {
        if (cursor!!.moveToNext()) {
            setImageView()
        } else if (cursor!!.moveToFirst()) {
            setImageView()
        }
    }

    private fun setImageView() {
        cursor?.let { cursor ->
            val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor.getLong(fieldIndex)
            val imageUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            binding.imageView.setImageURI(imageUri)
        }
    }

    private fun startSlideshow() {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    getNextContentsInfo()
                }
            }
        }, 2000, 2000)
    }

    private fun stopSlideshow() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("権限を設定してください。")
        builder.setMessage("必要な権限がないためアプリを終了します。設定から必要な権限を付与してください。")
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }
}