package com.example.bulksnitester

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var inputStaticIp: EditText
    private lateinit var inputUuid: EditText
    private lateinit var inputBulkDomains: EditText
    private lateinit var textResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputStaticIp = findViewById(R.id.inputStaticIp)
        inputUuid = findViewById(R.id.inputUuid)
        inputBulkDomains = findViewById(R.id.inputBulkDomains)
        textResults = findViewById(R.id.textResults)

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val domains = inputBulkDomains.text.toString().split("\n").filter { it.isNotBlank() }
            if (domains.isNotEmpty()) {
                textResults.text = "Testing ${domains.size} domains...\n"
                // Testing loop placeholder
                CoroutineScope(Dispatchers.Main).launch {
                    for (domain in domains) {
                        textResults.append("Checking: ${domain.trim()}\n")
                        delay(500) // Simulate processing time for now
                    }
                    textResults.append("Done!")
                }
            }
        }
    }
}
