package com.dev.james.workmanagerlesson

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.work.*
import coil.load
import coil.transform.CircleCropTransformation

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()
            )
            .build()

        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()
        val workManager = WorkManager.getInstance(applicationContext)

        val imageLayout = findViewById<ImageView>(R.id.downloadedImageView)
        val downloadButton = findViewById<Button>(R.id.buttonDownloadImage)
        val downloadStatus = findViewById<TextView>(R.id.downloadStatus)


        workManager.getWorkInfosForUniqueWorkLiveData("download")
            .observe(this , Observer{

                //get the results from download worker and filter worker
                val downloadInfo = it.find { wrkInfo ->
                    wrkInfo.id == downloadRequest.id
                }

                val filterInfo = it.find { wrkInfo ->
                    wrkInfo.id == colorFilterRequest.id
                }

                //load image into imageview using coil
                val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)?.toUri()
                val filterUri = filterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)?.toUri()

                filterUri ?: downloadUri

                downloadUri?.let { uri ->
                    imageLayout.load(downloadUri){
                        crossfade(true)
                        placeholder(R.drawable.ic_baseline_person_24)
                        transformations(CircleCropTransformation())
                    }
                }

                filterUri?.let {
                    imageLayout.load(filterUri){
                        crossfade(true)
                        placeholder(R.drawable.ic_baseline_person_24)
                        transformations(CircleCropTransformation())
                    }
                }


                downloadButton.isEnabled = downloadInfo?.state != WorkInfo.State.RUNNING

                when(downloadInfo?.state){
                    WorkInfo.State.RUNNING -> downloadStatus.text = "downloading..."
                    WorkInfo.State.SUCCEEDED -> downloadStatus.text = "download succeeded"
                    WorkInfo.State.FAILED -> downloadStatus.text = "download failed"
                    WorkInfo.State.CANCELLED -> downloadStatus.text = "download cancelled"
                    WorkInfo.State.ENQUEUED -> downloadStatus.text = "download enqueued"
                    WorkInfo.State.BLOCKED -> downloadStatus.text = "download blocked "

                }
                when(filterInfo?.state){
                    WorkInfo.State.RUNNING -> downloadStatus.text = "applying filter..."
                    WorkInfo.State.SUCCEEDED -> downloadStatus.text = "filter succeeded"
                    WorkInfo.State.FAILED -> downloadStatus.text = "filter failed"
                    WorkInfo.State.CANCELLED -> downloadStatus.text = "filter cancelled"
                    WorkInfo.State.ENQUEUED -> downloadStatus.text = "filter enqueued"
                    WorkInfo.State.BLOCKED -> downloadStatus.text = "filter blocked "

                }

            })


        downloadButton.setOnClickListener {
            workManager.beginUniqueWork(
                "download",
                ExistingWorkPolicy.KEEP,
                downloadRequest
            ).then(colorFilterRequest)
                .enqueue()
        }


    }
}