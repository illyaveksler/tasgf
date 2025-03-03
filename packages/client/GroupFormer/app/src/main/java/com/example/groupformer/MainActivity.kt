package com.example.groupformer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val profButton: Button = findViewById(R.id.ProfButton)
        val stuButton: Button = findViewById(R.id.StuButton)

        profButton.setOnClickListener {
            val intent = Intent(this, ProfessorActivity::class.java)
            startActivity(intent)
        }

        stuButton.setOnClickListener {
            val intent = Intent(this, StudentActivity::class.java)
            startActivity(intent)
        }
    }
}
