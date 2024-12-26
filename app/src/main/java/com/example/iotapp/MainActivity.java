package com.example.iotapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_READ_URL = "https://api.thingspeak.com/channels/2794908/feeds.json";
    private static final String CHANNEL_WRITE_URL = "https://api.thingspeak.com/update";

    private static final String READ_API_KEY = "RKTMBAN559EUAS9B";
    private static final String WRITE_API_KEY = "CVZSD9VIVJZNS2XG";

    private TextView temperaturaTextView;
    private ProgressBar loadingProgressBar;
    private Button siButton, noButton, actualizarButton;
    private AsyncHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        siButton = findViewById(R.id.siButton);
        noButton = findViewById(R.id.noButton);
        actualizarButton = findViewById(R.id.actualizarButton);

        // Inicializar AsyncHttpClient una sola vez para ser reutilizado
        client = new AsyncHttpClient();

        // Configurar botones
        siButton.setOnClickListener(v -> enviarComando(1));
        noButton.setOnClickListener(v -> enviarComando(0));
        actualizarButton.setOnClickListener(v -> obtenerDatosThingSpeak());

        // Obtener datos iniciales
        obtenerDatosThingSpeak();
    }

    // Función para obtener datos de ThingSpeak
    private void obtenerDatosThingSpeak() {
        mostrarProgreso(true);

        RequestParams params = new RequestParams();
        params.put("api_key", READ_API_KEY);
        params.put("results", 1); // Obtener solo el último feed

        client.get(CHANNEL_READ_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray feeds = response.optJSONArray("feeds");
                    if (feeds != null && feeds.length() > 0) {
                        JSONObject feed = feeds.getJSONObject(0);
                        double temperatura = feed.optDouble("field1", Double.NaN);

                        if (!Double.isNaN(temperatura)) {
                            temperaturaTextView.setText(String.format("Temperatura: %.1f °C", temperatura));
                        } else {
                            temperaturaTextView.setText("Temperatura no disponible");
                        }
                    } else {
                        temperaturaTextView.setText("No hay datos disponibles");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarError("Error al obtener datos. Intenta nuevamente.");
                } finally {
                    mostrarProgreso(false);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                mostrarProgreso(false);
                if (statusCode == 0) {
                    mostrarError("Error de conexión. Verifica tu conexión y vuelve a intentarlo.");
                } else {
                    mostrarError("Error al obtener datos. Código de error: " + statusCode);
                }
            }
        });
    }

    // Función para enviar el comando al canal de ThingSpeak
    private void enviarComando(int comando) {
        mostrarProgreso(true);

        RequestParams params = new RequestParams();
        params.put("api_key", WRITE_API_KEY);
        params.put("field2", comando);  // Enviar el comando (1 o 0)

        client.post(CHANNEL_WRITE_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Toast.makeText(MainActivity.this, "Comando enviado correctamente", Toast.LENGTH_SHORT).show();
                mostrarProgreso(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                mostrarProgreso(false);
                if (statusCode == 0) {
                    mostrarError("Error de conexión al enviar comando. Intenta nuevamente.");
                } else {
                    mostrarError("Error al enviar comando. Código de error: " + statusCode);
                }
            }
        });
    }

    // Función para mostrar el progreso (loading)
    private void mostrarProgreso(boolean enProgreso) {
        loadingProgressBar.setVisibility(enProgreso ? View.VISIBLE : View.GONE);
    }

    // Función para mostrar mensajes de error con un botón de reintento usando AlertDialog
    private void mostrarError(String mensaje) {
        // Mostrar mensaje de error usando Snackbar
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG)
                .setAction("Reintentar", v -> obtenerDatosThingSpeak())  // Reintentar acción
                .show();
    }
}

