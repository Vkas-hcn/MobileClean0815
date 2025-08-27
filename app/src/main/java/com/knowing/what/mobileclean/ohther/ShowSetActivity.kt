package com.knowing.what.mobileclean.ohther



import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ActivityShowSetBinding


class ShowSetActivity : AppCompatActivity() {

    private val binding by lazy { ActivityShowSetBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ss)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        binding.apply {
            imgBack.setOnClickListener {
                finish()
            }
            atvShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${this@ShowSetActivity.packageName}")
                try {
                    startActivity(Intent.createChooser(intent, "Share via"))
                } catch (ex: Exception) {
                    // Handle error
                }
            }
            atvPlo.setOnClickListener {
                val intent = Intent(Intent .ACTION_VIEW)
                //TODO
                intent.data = "https://sites.google.com/view/easy-cleanmmm/home".toUri()
                startActivity(intent)
            }
        }

    }

}