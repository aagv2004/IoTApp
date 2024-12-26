package com.example.iotapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_READ_URL = "https://api.thingspeak.com/channels/2795546/feeds.json"; // Reemplaza con tu ID de canal
    private static final String CHANNEL_WRITE_URL = "https://api.thingspeak.com/update";

    private static final String READ_API_KEY = "1SADELONT7CQVKN4"; // Reemplaza con tu API Key de lectura
    private static final String WRITE_API_KEY = "IK149MYOHTUDUW7D"; // Reemplaza con tu API Key de escritura

    private TextView temperaturaTextView;
    private TextView humedadTextView; // Añadir un TextView para la humedad
    private ProgressBar loadingProgressBar;
    private Button siButton, noButton, actualizarButton;
    private AsyncHttpClient client;
    private long lastCommandTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        humedadTextView = findViewById(R.id.humedadTextView); // Inicializar el TextView de humedad
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        siButton = findViewById(R.id.siButton);
        noButton = findViewById(R.id.noButton);
        actualizarButton = findViewById(R.id.actualizarButton);

        // Inicializar AsyncHttpClient una sola vez para ser reutilizado
        client = new AsyncHttpClient();

        // Configurar botones
        siButton.setOnClickListener(v -> encenderLed()); // Enviar comando 1 para encender luz
        noButton.setOnClickListener(v -> apagarLed()); // Enviar comando 0 para detener el parpadeo
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
                        double humedad = feed.optDouble("field2", Double.NaN);
                        double aviso = feed.optDouble("field3", Double.NaN);

                        if (!Double.isNaN(temperatura)) {
                            temperaturaTextView.setText(String.format("Temperatura: %.1f °C", temperatura));
                            // Mostrar el dialogo si la temperatura es mayor a 28.5
                            if (temperatura > 28.5) {
                                mostrarDialogoAdvertencia(temperatura);
                            }
                        } else {
                            temperaturaTextView.setText("Temperatura : No disponible");
                        }

                        if (!Double.isNaN(humedad)) {
                            humedadTextView.setText(String.format("Humedad: %.1f %%", humedad));
                        } else {
                            humedadTextView.setText("Humedad: No disponible");
                        }

                        // Manejo del aviso
                        if ( aviso == 1) {
                            mostrarSnackbar("El LED está parpadeando", "green");
                        } else {
                            mostrarSnackbar("El LED está apagado", "yellow");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarSnackbar("Error al obtener datos", "red");
                } finally {
                    mostrarProgreso(false); // Asegúrate de que esto se llame siempre
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                mostrarSnackbar("Error de conexión", "red");
                mostrarProgreso(false); // Asegúrate de que esto se llame siempre
            }
        });
    }

    // Función para mostrar el diálogo de advertencia
    private void mostrarDialogoAdvertencia(double temperatura) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¡Precaución!");
        builder.setMessage(String.format("Se recomienda activar la luz de advertencia.\nTemperatura detectada: %.1f °C", temperatura));
        builder.setPositiveButton("Activar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                encenderLed(); // Enviar comando para activar la luz
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Enviar comando para detener el parpadeo
                dialog.dismiss(); // Cerrar el diálogo
            }
        });
        builder.show();
    }

    // Función para enviar comando a ThingSpeak
    private void encenderLed() {
        String url = "https://api.thingspeak.com/update?api_key=IK149MYOHTUDUW7D&field3=1";
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(url, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    if (statusCode == 200){
                        String response = new String(responseBody);
                        if (response.equals("0")){
                            mostrarSnackbar("Respuesta: " + response + "        | Espere 5 a 15 segundos.", "yellow");
                        } else {
                            mostrarSnackbar("Respuesta: " + response + "        | Luz de alerta encendida.", "green");
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    mostrarSnackbar("Status Code: " + statusCode + "\n Header: " + headers + "\n responseBody: " + responseBody + "\nError: " + error, "red");
                }
            });
    }

    private void apagarLed() {
        String url = "https://api.thingspeak.com/update?api_key=IK149MYOHTUDUW7D&field3=0";
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200){
                    String response = new String(responseBody);
                    if (response.equals("0")){
                        mostrarSnackbar("Respuesta: " + response + "            | Espere 5 a 15 segundos.", "yellow");
                    } else {
                        mostrarSnackbar("Respuesta: " + response + "            | Luz de alerta Apagada.", "green");
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarSnackbar("Status Code: " + statusCode + "\n Header: " + headers + "\n responseBody: " + responseBody + "\nError: " + error, "red");
            }
        });
    }

    // Función para mostrar/ocultar el progreso
    private void mostrarProgreso(boolean mostrar) {
        loadingProgressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        if (mostrar) {
            mostrarSnackbar("Cargando...", "yellow");
        }
    }

    // Función para mostrar Snackbar
    private void mostrarSnackbar(String mensaje, String color) {
        View view = findViewById(R.id.mainLayout); // Asegúrate de que este ID corresponde a tu layout principal
        Snackbar snackbar = Snackbar.make(view, mensaje, Snackbar.LENGTH_SHORT);
        switch (color) {
            case "red":
                snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                break;
            case "yellow":
                snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                break;
            case "green":
                snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                break;
        }
        snackbar.show();
    }
}