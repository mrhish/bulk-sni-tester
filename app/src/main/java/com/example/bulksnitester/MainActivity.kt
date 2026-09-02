package com.example.bulksnitester

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private var inputPasteConfig: EditText? = null
    private var inputAddress: EditText? = null
    private var inputPort: EditText? = null
    private var inputUuid: EditText? = null
    private var inputBulkDomains: EditText? = null
    private var spinnerProtocol: Spinner? = null
    private var textResults: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            inputPasteConfig = findViewById(R.id.inputPasteConfig)
            inputAddress = findViewById(R.id.inputAddress)
            inputPort = findViewById(R.id.inputPort)
            inputUuid = findViewById(R.id.inputUuid)
            inputBulkDomains = findViewById(R.id.inputBulkDomains)
            spinnerProtocol = findViewById(R.id.spinnerProtocol)
            textResults = findViewById(R.id.textResults)

            findViewById<Button>(R.id.btnParseConfig)?.setOnClickListener {
                val config = inputPasteConfig?.text.toString().trim()
                if (config.startsWith("vless://") || config.startsWith("vmess://")) {
                    parseConfigLink(config)
                } else {
                    Toast.makeText(this, "Paste a valid vless:// or vmess:// link", Toast.LENGTH_SHORT).show()
                }
            }

            findViewById<Button>(R.id.btnStartTest)?.setOnClickListener {
                val domains = inputBulkDomains?.text.toString().split("\n").filter { it.isNotBlank() }
                if (domains.isEmpty()) {
                    Toast.makeText(this, "Enter at least one domain", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val targetPort = inputPort?.text.toString().toIntOrNull() ?: 443
                textResults?.text = "Executing bulk connectivity check (${domains.size} domains)...\n\n"

                CoroutineScope(Dispatchers.IO).launch {
                    for (domain in domains) {
                        val cleanHost = domain.trim()
                        withContext(Dispatchers.Main) {
                            textResults?.append("Testing $cleanHost... ")
                        }

                        val latency = measureSocketLatency(cleanHost, targetPort)

                        withContext(Dispatchers.Main) {
                            if (latency >= 0) {
                                textResults?.append("✅ Active (${latency}ms)\n")
                            } else {
                                textResults?.append("❌ Timeout / Dead\n")
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        textResults?.append("\nAll tests completed!")
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Init Error: ${e.localizedMessage}",coons = Toast.LENGTH_LONG).show()
        }
    }

    private fun parseConfigLink(config: String) {
        try {
            val prefix = if (config.startsWith("vless://")) "vless://" else "vmess://"
            val withoutPrefix = config.removePrefix(prefix)
            val uuidAndRest = withoutPrefix.split("@")
            if (uuidAndRest.size > 1) {
                inputUuid?.setText(uuidAndRest[0])
                val ipAndRest = uuidAndRest[1].split(":")
                inputAddress?.setText(ipAndRest[0])
                val portAndParams = ipAndRest[1].split("?")
                inputPort?.setText(portAndParams[0].split("#")[0])
            }
            Toast.makeText(this, "Parsed successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Parsing failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun measureSocketLatency(host: String, port: Int): Long {
        return measureTimeMillis {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2000)
                }
            } catch (e: Exception) {
                return -1L
            }
        }.let { time -> if (time >= 2000) -1L else time }
    }
}
