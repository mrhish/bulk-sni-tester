package com.example.bulksnitester

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import libv2ray.Libv2ray

class MainActivity : AppCompatActivity() {

    private lateinit var inputPasteConfig: EditText
    private lateinit var inputAddress: EditText
    private lateinit var inputPort: EditText
    private lateinit var inputUuid: EditText
    private lateinit var inputBulkDomains: EditText
    private lateinit var spinnerProtocol: Spinner
    private lateinit var textResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputPasteConfig = findViewById(R.id.inputPasteConfig)
        inputAddress = findViewById(R.id.inputAddress)
        inputPort = findViewById(R.id.inputPort)
        inputUuid = findViewById(R.id.inputUuid)
        inputBulkDomains = findViewById(R.id.inputBulkDomains)
        spinnerProtocol = findViewById(R.id.spinnerProtocol)
        textResults = findViewById(R.id.textResults)

        // Parse VLESS/VMess link
        findViewById<Button>(R.id.btnParseConfig).setOnClickListener {
            val config = inputPasteConfig.text.toString().trim()
            if (config.startsWith("vless://")) {
                parseVless(config)
            } else {
                Toast.makeText(this, "Please paste a valid vless:// link", Toast.LENGTH_SHORT).show()
            }
        }

        // Start Real Delay Test via Xray-Core
        findViewById<Button>(R.id.btnStartTest).setOnClickListener {
            val domains = inputBulkDomains.text.toString().split("\n").filter { it.isNotBlank() }
            if (domains.isEmpty()) return@setOnClickListener

            val uuid = inputUuid.text.toString()
            val port = inputPort.text.toString().toIntOrNull() ?: 443

            textResults.text = "Testing ${domains.size} domains via Xray Core...\n\n"
            
            CoroutineScope(Dispatchers.IO).launch {
                for (domain in domains) {
                    val cleanDomain = domain.trim()
                    
                    withContext(Dispatchers.Main) {
                        textResults.append("Probing: $cleanDomain... ")
                    }

                    // Generate a basic Xray Outbound JSON config for testing
                    val configJson = buildBasicXrayConfig(cleanDomain, port, uuid, cleanDomain)
                    
                    // Call the native Xray engine to measure the real delay to Google
                    try {
                        val pingMs = Libv2ray.measureOutboundDelay(
                            configJson, 
                            "https://connectivitycheck.gstatic.com/generate_204"
                        )
                        
                        withContext(Dispatchers.Main) {
                            if (pingMs > 0) {
                                textResults.append("✅ ${pingMs}ms\n")
                            } else {
                                textResults.append("❌ Failed\n")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            textResults.append("❌ Error\n")
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    textResults.append("\nDone!")
                }
            }
        }
    }

    private fun parseVless(config: String) {
        try {
            val withoutPrefix = config.removePrefix("vless://")
            val uuidAndRest = withoutPrefix.split("@")
            inputUuid.setText(uuidAndRest[0])
            
            val ipAndRest = uuidAndRest[1].split(":")
            inputAddress.setText(ipAndRest[0])
            
            val portAndParams = ipAndRest[1].split("?")
            inputPort.setText(portAndParams[0].split("#")[0])
            spinnerProtocol.setSelection(0)
            
            Toast.makeText(this, "Parsed successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to parse link", Toast.LENGTH_SHORT).show()
        }
    }

    // Creates the JSON string required by Xray-core to open a connection
    private fun buildBasicXrayConfig(ip: String, port: Int, uuid: String, sni: String): String {
        return """
        {
            "outbounds": [{
                "protocol": "vless",
                "settings": {
                    "vnext": [{
                        "address": "$ip",
                        "port": $port,
                        "users": [{"id": "$uuid", "encryption": "none"}]
                    }]
                },
                "streamSettings": {
                    "network": "tcp",
                    "security": "tls",
                    "tlsSettings": {
                        "serverName": "$sni",
                        "allowInsecure": true
                    }
                }
            }]
        }
        """.trimIndent()
    }
}
