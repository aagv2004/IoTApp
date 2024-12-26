package com.example.iotapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {
    private TextView temperaturaTextView;
    private Button botonSi, botonNo;
    private ProgressBar progressBar;

    private static final String THINGSPEAK_API_URL = "https://api.thingspeak.com/channels/2794908/feeds.json?api_key=RKTMBAN559EUAS9B&results=2";
    private static final String API_KEY = "CVZSD9VIVJZNS2XG";
    private static final String CHANNEL_URL = "https://api.thingspeak.com/update";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperaturaTextView = findViewById(R.id.temperaturaTextView);
        progressBar = findViewById(R.id.progressBar);
        TextView confirmationTextView = findViewById(R.id.confirmationTextView);
        botonSi = findViewById(R.id.botonSi);
        botonNo = findViewById(R.id.botonNo);

        // Animación inicial
        animateViews();

        // Inicializar notificaciones
        crearCanalDeNotificaciones();

        // Lógica inicial
        botonSi.setEnabled(false);
        botonNo.setEnabled(false);
        obtenerTemperaturaDeThingSpeak();

        // Botón Sí
        botonSi.setOnClickListener(v -> {
            mostrarMensajeConfirmacion(confirmationTextView, "Alerta activada en la habitación.", "#4CAF50");
            enviarComandoThingSpeak(1);
        });

        // Botón No
        botonNo.setOnClickListener(v -> {
            mostrarMensajeConfirmacion(confirmationTextView, "Acción cancelada.", "#F44336");
            enviarComandoThingSpeak(0);
        });
    }

    private void animateViews() {
        findViewById(R.id.preguntaTextView).animate().alpha(1).setDuration(1000).setStartDelay(500).start();
        findViewById(R.id.actionButtons).animate().alpha(1).setDuration(1000).setStartDelay(1000).start();
    }

    private void obtenerTemperaturaDeThingSpeak() {
        progressBar.setVisibility(View.VISIBLE);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(THINGSPEAK_API_URL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Float temperatura = parseTemperature(response);
                if (temperatura != null) {
                    temperaturaTextView.setText(String.format("Temperatura: %.1f°C", temperatura));
                    boolean isHighTemperature = temperatura > 30.0;
                    if (isHighTemperature) {
                        mostrarNotificacion("¡RIESGO DE TEMPERATURA ALTA!");
                    }
                    botonSi.setEnabled(isHighTemperature);
                    botonNo.setEnabled(isHighTemperature);
                } else {
                    temperaturaTextView.setText("Error al obtener la temperatura");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progressBar.setVisibility(View.GONE);
                temperaturaTextView.setText("Error al obtener la temperatura");
                Log.e("FetchTemperatureTask", "Error en la respuesta: " + statusCode);
            }
        });
    }

    private Float parseTemperature(JSONObject jsonData) {
        try {
            JSONArray feeds = jsonData.getJSONArray("feeds");
            if (feeds.length() > 0) {
                JSONObject latestFeed = feeds.getJSONObject(0);
                return (float) latestFeed.getDouble("field1");
            }
        } catch (Exception e) {
            Log.e("FetchTemperatureTask", "Error al parsear JSON: " + e.getMessage());
        }
        return null;
    }

    private void mostrarNotificacion(String mensaje) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_id")
                .setSmallIcon(R.drawable.baseline_crisis_alert_24)
                .setContentTitle("Alerta de Temperatura")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void crearCanalDeNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel("canal_id", "Canal de alertas", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Canal para notificaciones de temperatura");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);
        }
    }

    private void mostrarMensajeConfirmacion(TextView confirmationTextView, String mensaje, String color) {
        confirmationTextView.setVisibility(View.VISIBLE);
        confirmationTextView.setText(mensaje);
        confirmationTextView.setTextColor(Color.parseColor(color));
        confirmationTextView.animate().alpha(1).setDuration(500).withEndAction(() -> {
            confirmationTextView.animate().alpha(0).setDuration(1000).setStartDelay(2000).start();
        }).start();
    }

    private void enviarComandoThingSpeak(int comando) {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = CHANNEL_URL + "?api_key=" + API_KEY + "&field2=" + comando;

        client.post(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("ThingSpeak", "Comando enviado exitosamente: " + comando);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("ThingSpeak", "Error al enviar comando: " + error.getMessage());
            }
        });
    }
}
