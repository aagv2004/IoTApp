package com.example.iotapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    private static final String THINGSPEAK_READ_URL = "https://api.thingspeak.com/channels/2788935/feeds.json?api_key=4GGFDD4U7K3IVV43&results=1";
    private static final String THINGSPEAK_WRITE_URL = "https://api.thingspeak.com/update?api_key=2XMA8DIRYBUCRBOM";

    private TextView tempAmbiente, tempPersona, pregunta, noData;
    private Button botonSi, botonNo, botonActualizar;
    private AsyncHttpClient client;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar AsyncHttpClient y Handler
        client = new AsyncHttpClient();
        handler = new Handler();

        // Inicializar vistas
        tempAmbiente = findViewById(R.id.tempAmbiente);
        tempPersona = findViewById(R.id.tempPersona);
        pregunta = findViewById(R.id.pregunta);
        noData = findViewById(R.id.noData);
        botonSi = findViewById(R.id.botonSi);
        botonNo = findViewById(R.id.botonNo);
        botonActualizar = findViewById(R.id.botonActualizar);

        // Configurar botón de actualización
        botonActualizar.setOnClickListener(view -> cargarDatos());

        // Configurar botones Sí y No
        botonSi.setOnClickListener(view -> handleResponse(1));
        botonNo.setOnClickListener(view -> handleResponse(0));

        // Inicializar estado de botones
        botonSi.setEnabled(false);
        botonNo.setEnabled(false);
        pregunta.setVisibility(View.GONE);
    }

    private void cargarDatos() {
        client.get(THINGSPEAK_READ_URL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String responseData = new String(responseBody);
                    JSONObject json = new JSONObject(responseData);
                    JSONArray feeds = json.getJSONArray("feeds");

                    if (feeds.length() > 0) {
                        JSONObject latestData = feeds.getJSONObject(feeds.length() - 1);
                        final String ambiente = latestData.optString("field1", "Sin datos");
                        final String persona = latestData.optString("field2", "Sin datos");
                        final String field3 = latestData.optString("field3", "");

                        Log.d("Datos API", "Ambiente: " + ambiente + ", Persona: " + persona + ", Field3: " + field3);

                        runOnUiThread(() -> {
                            tempAmbiente.setText("Temperatura ambiente: " + ambiente + " °C");
                            tempPersona.setText("Temperatura persona: " + persona + " °C");

                            if (field3.isEmpty()) {
                                // Activar pregunta y botones si hay datos en field2
                                if (!persona.equals("Sin datos")) {
                                    pregunta.setVisibility(View.VISIBLE);
                                    pregunta.setText("¿Considera que Persona tiene fiebre?");
                                    botonSi.setEnabled(true);
                                    botonNo.setEnabled(true);
                                } else {
                                    pregunta.setVisibility(View.GONE);
                                    botonSi.setEnabled(false);
                                    botonNo.setEnabled(false);
                                }
                            } else {
                                // Desactivar botones y ocultar pregunta si field3 ya tiene un valor
                                pregunta.setVisibility(View.GONE);
                                noData.setText("Último dato con comando asignado. \n    Field3 = " + field3 + "         | 0: No, 1: Sí. |\n(Sugerencia usuario: Actualizar información)\n(Sugerencia dev: Escribir nuevo feed)");
                                noData.setVisibility(View.VISIBLE);
                                botonSi.setEnabled(false);
                                botonNo.setEnabled(false);
                                Log.d("Validación", "El último dato ya tiene un valor asignado en Field3: " + field3);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
            }
        });
    }

    private void handleResponse(int response) {
        botonSi.setEnabled(false);
        botonNo.setEnabled(false);
        pregunta.setVisibility(View.GONE);

        handler.postDelayed(() -> sendField3Update(response), 3000);
    }

    private void sendField3Update(int value) {
        String url = THINGSPEAK_WRITE_URL + "&field3=" + value;

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("API Write", "Field3 actualizado con valor: " + value);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                error.printStackTrace();
            }
        });
    }
}
