package com.example.smartparking

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {



    private val AWS_ACCESS_KEY = "ESCRIBIR_ACCESS_KEY"
    private val AWS_SECRET_KEY = "ESCRIBIR_SECRET_KEY"
    private val AWS_SESSION_TOKEN = "ESCRIBIR_AQUÍ_SESSIONTOKEN"

    private val AWS_REGION = Regions.US_EAST_1
    private val NOMBRE_TABLA = "Estacionamiento"
    private val ID_PLAZA = "plaza_01"

    // Vistas
    private lateinit var tvEstado: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvUltimaActualizacion: TextView
    private lateinit var viewIndicadorColor: View
    private lateinit var btnActualizar: Button

    private var ddbClient: AmazonDynamoDBClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEstado = findViewById(R.id.tvEstado)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion)
        viewIndicadorColor = findViewById(R.id.viewIndicadorColor)
        btnActualizar = findViewById(R.id.btnActualizar)

        // Cargar almacenamiento temporal offline
        cargarDatosOffline()

        inicializarDynamoDB()

        btnActualizar.setOnClickListener {
            Toast.makeText(this, "Consultando DynamoDB...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                consultarEstadoDynamoDB()
            }
        }


        lifecycleScope.launch {
            while (isActive) {
                consultarEstadoDynamoDB()
                delay(2000)
            }
        }
    }

    private fun inicializarDynamoDB() {
        try {
            val credentials: AWSCredentials = if (AWS_SESSION_TOKEN.isNotBlank()) {
                BasicSessionCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_SESSION_TOKEN)
            } else {
                BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
            }

            ddbClient = AmazonDynamoDBClient(credentials).apply {
                setRegion(Region.getRegion(AWS_REGION))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error AWS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun consultarEstadoDynamoDB() {
        withContext(Dispatchers.IO) {
            try {
                val key = mapOf("id_plaza" to AttributeValue(ID_PLAZA))
                val request = GetItemRequest()
                    .withTableName(NOMBRE_TABLA)
                    .withKey(key)
                    .withConsistentRead(true)

                val result = ddbClient?.getItem(request)
                val item = result?.item

                withContext(Dispatchers.Main) {
                    val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    if (item != null) {
                        val estado = item["estado"]?.s 
                            ?: item["Estado"]?.s 
                            ?: "DESCONOCIDO"

                        val distancia = item["distancia"]?.n 
                            ?: item["distancia"]?.s 
                            ?: item["Distancia"]?.n 
                            ?: item["Distancia"]?.s 
                            ?: "--"

                        guardarDatosOffline(estado, distancia, horaActual)
                        actualizarUI(estado, distancia, horaActual)
                    } else {
                        tvEstado.text = "SIN REGISTRO"
                        tvDistancia.text = "No se encontró '$ID_PLAZA'"
                        tvUltimaActualizacion.text = "Último chequeo: $horaActual"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvUltimaActualizacion.text = "Sin red: mostrando datos locales"
                }
            }
        }
    }

    private fun guardarDatosOffline(estado: String, distancia: String, fecha: String) {
        val prefs = getSharedPreferences("parking_offline_cache", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("estado", estado)
            .putString("distancia", distancia)
            .putString("fecha", fecha)
            .apply()
    }

    private fun cargarDatosOffline() {
        val prefs = getSharedPreferences("parking_offline_cache", Context.MODE_PRIVATE)
        val estado = prefs.getString("estado", null)
        val distancia = prefs.getString("distancia", "--")
        val fecha = prefs.getString("fecha", "--")

        if (estado != null) {
            actualizarUI(estado, distancia ?: "--", "$fecha (Caché Offline)")
        }
    }

    private fun actualizarUI(estado: String, distancia: String, horaConsulta: String) {
        val estadoNormalizado = estado.trim().uppercase()
        tvEstado.text = estadoNormalizado
        tvDistancia.text = "Distancia: $distancia cm"
        tvUltimaActualizacion.text = "Actualizado: $horaConsulta"

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
        }

        if (estadoNormalizado == "OCUPADO") {
            tvEstado.setTextColor(Color.parseColor("#DC2626")) // Rojo
            shape.setColor(Color.parseColor("#DC2626"))
        } else {
            tvEstado.setTextColor(Color.parseColor("#16A34A")) // Verde
            shape.setColor(Color.parseColor("#16A34A"))
        }
        viewIndicadorColor.background = shape
    }
}
