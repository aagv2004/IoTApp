package com.example.iotapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONObject;
import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String THINGSPEAK_API_URL = "https://api.thingspeak.com/channels/2794908/fields/1.json?api_key=RKTMBAN559EUAS9B&results=1";
    private TextView temperaturaTextView, confirmationTextView;
    private ProgressBar progressBar;
    private Button botonSi, botonNo, botonLeer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        progressBar = findViewById(R.id.progressBar);
        botonSi = findViewById(R.id.botonSi);
        botonNo = findViewById(R.id.botonNo);
        botonLeer = findViewById(R.id.botonLeer);
        confirmationTextView = findViewById(R.id.confirmationTextView);

        // Acción del botón "Leer ThingSpeak"
        botonLeer.setOnClickListener(v -> obtenerTemperaturaDeThingSpeak());

        // Acción del botón "Sí"
        botonSi.setOnClickListener(v -> {
            activarAlerta(true);
            enviarComandoThingSpeak(1);
        });

        // Acción del botón "No"
        botonNo.setOnClickListener(v -> {
            activarAlerta(false);
            enviarComandoThingSpeak(0);
        });
    }

    private void obtenerTemperaturaDeThingSpeak() {
        progressBar.setVisibility(View.VISIBLE);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(THINGSPEAK_API_URL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Log.d("ThingSpeakResponse", "Response: " + response.toString());
                Float temperatura = parseTemperature(response);
                if (temperatura != null) {
                    temperaturaTextView.setText(String.format("Temperatura: %.1f°C", temperatura));
                    boolean isHighTemperature = temperatura > 20.0;
                    if (isHighTemperature) {
                        mostrarNotificacion("¡RIESGO DE TEMPERATURA ALTA!");
                        mostrarBotonesAccion(true);
                    } else {
                        mostrarBotonesAccion(false);
                    }
                } else {
                    temperaturaTextView.setText("Error al obtener la temperatura");
                    Log.e("TemperatureError", "No se pudo parsear la temperatura");
                    mostrarBotonesAccion(false);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
                temperaturaTextView.setText("Error al obtener la temperatura");
                Log.e("FetchTemperatureTask", "Error en la respuesta: " + statusCode);
                Log.e("FetchTemperatureTask", "Detalles del error: ", throwable);
                mostrarBotonesAccion(false);
            }
        });
    }

    private void mostrarBotonesAccion(boolean mostrar) {
        LinearLayout actionButtons = findViewById(R.id.actionButtons);
        if (mostrar) {
            actionButtons.setVisibility(View.VISIBLE);
            botonSi.setEnabled(true);
            botonNo.setEnabled(true);
        } else {
            actionButtons.setVisibility(View.GONE);
            botonSi.setEnabled(false);
            botonNo.setEnabled(false);
        }
    }

    private float parseTemperature(JSONObject response) {
        try {
            JSONObject feed = response.getJSONArray("feeds").getJSONObject(0);
            return Float.parseFloat(feed.getString("field1"));
        } catch (Exception e) {
            return -1; // Retorna un valor no válido en caso de error
        }
    }

    private void manejarBotonesSegunTemperatura(float temperatura) {
        boolean isHighTemperature = temperatura > 20.0;
        if (isHighTemperature) {
            botonSi.setEnabled(true);
            botonNo.setEnabled(true);
            mostrarNotificacion("¡RIESGO DE TEMPERATURA ALTA!");
        } else {
            botonSi.setEnabled(false);
            botonNo.setEnabled(false);
        }
    }

    private void activarAlerta(boolean activar) {
        botonSi.setEnabled(false);  // Deshabilitar botones
        botonNo.setEnabled(false);  // Deshabilitar botones

        if (activar) {
            mostrarMensajeConfirmacion("Alerta activada en la habitación", "#4CAF50");
            enviarComandoThingSpeak(1);
        } else {
            mostrarMensajeConfirmacion("Acción cancelada", "#F44336");
            enviarComandoThingSpeak(0);
        }
    }

    private void mostrarError() {
        temperaturaTextView.setText("Error al obtener la temperatura");
        botonSi.setEnabled(false);  // Deshabilitar botones
        botonNo.setEnabled(false);  // Deshabilitar botones
    }

    private void mostrarNotificacion(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void mostrarMensajeConfirmacion(String mensaje, String color) {
        confirmationTextView.setText(mensaje);
        confirmationTextView.setTextColor(android.graphics.Color.parseColor(color));
        confirmationTextView.setVisibility(View.VISIBLE);
    }

    private void enviarComandoThingSpeak(int comando) {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = "https://api.thingspeak.com/update.json?api_key=RKTMBAN559EUAS9B&field2=" + comando;

        // Enviar solicitud GET con el valor de 'comando' a ThingSpeak
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (statusCode == 200) {
                    Log.d("ThingSpeak", "Comando enviado correctamente: " + comando);
                    // Puedes mostrar una notificación o realizar otra acción si es necesario
                } else {
                    Log.e("ThingSpeak", "Error al enviar comando, código de estado: " + statusCode);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e("ThingSpeak", "Error al enviar comando: " + throwable.getMessage());
            }
        });
    }
}
