package com.example.wiltechfinance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class PinActivity extends AppCompatActivity {

    private EditText etPin;
    private Button btnConfirmarPin, btnOlvidePin;
    private FirebaseFirestore db;
    private String credencialUsuario;
    private int intentosFallidos = 0;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        db = FirebaseFirestore.getInstance();
        etPin = findViewById(R.id.etPin);
        btnConfirmarPin = findViewById(R.id.btnConfirmarPin);
        btnOlvidePin = findViewById(R.id.btnOlvidePin);
        prefs = getSharedPreferences("SeguridadPrefs", Context.MODE_PRIVATE);

        credencialUsuario = getIntent().getStringExtra("CREDENCIAL");

        // Control del bloqueo de 3 horas (10,800,000 milisegundos)
        long horaBloqueo = prefs.getLong("hora_bloqueo", 0);
        long horaActual = System.currentTimeMillis();

        if (horaBloqueo != 0 && (horaActual - horaBloqueo) < 10800000) {
            long tiempoRestanteMins = (10800000 - (horaActual - horaBloqueo)) / 60000;
            Toast.makeText(this, "App bloqueada. Intenta en " + tiempoRestanteMins + " minutos.", Toast.LENGTH_LONG).show();
            btnConfirmarPin.setEnabled(false);
        }

        btnConfirmarPin.setOnClickListener(v -> {
            String pinIngresado = etPin.getText().toString().trim();

            if (pinIngresado.length() < 4) {
                Toast.makeText(PinActivity.this, "El PIN debe ser de 4 dígitos", Toast.LENGTH_SHORT).show();
                return;
            }

            validarPinEnFirestore(credencialUsuario, pinIngresado);
        });

        // Navegación hacia la pantalla de recuperación
        btnOlvidePin.setOnClickListener(v -> {
            Intent intent = new Intent(PinActivity.this, RecuperarPinActivity.class);
            startActivity(intent);
        });
    }

    private void validarPinEnFirestore(String credencial, String pinIngresado) {
        btnConfirmarPin.setEnabled(false);

        // Si la credencial viene nula por seguridad, usa el usuario de prueba por defecto
        String documento = (credencial != null && !credencial.isEmpty()) ? credencial : "Prueba@tech.com";

        db.collection("usuarios").document(documento)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Valor por defecto "1234" si el documento aún no asigna un PIN
                    String pinReal = documentSnapshot.exists() && documentSnapshot.contains("pin")
                            ? documentSnapshot.getString("pin")
                            : "1234";

                    if (pinIngresado.equals(pinReal)) {
                        Toast.makeText(PinActivity.this, "¡Acceso Concedido!", Toast.LENGTH_SHORT).show();
                        intentosFallidos = 0;

                        Intent intent = new Intent(PinActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        btnConfirmarPin.setEnabled(true);
                        procesarIntentoFallido();
                    }
                })
                .addOnFailureListener(e -> {
                    btnConfirmarPin.setEnabled(true);
                    Toast.makeText(PinActivity.this, "Error de conexión con la nube", Toast.LENGTH_SHORT).show();
                });
    }

    private void procesarIntentoFallido() {
        intentosFallidos++;
        int restantes = 3 - intentosFallidos;

        if (intentosFallidos >= 3) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("hora_bloqueo", System.currentTimeMillis());
            editor.apply();

            enviarCorreoSeguridad(credencialUsuario);

            Toast.makeText(PinActivity.this, "Aplicación Bloqueada por 3 horas", Toast.LENGTH_LONG).show();
            btnConfirmarPin.setEnabled(false);
        } else {
            Toast.makeText(PinActivity.this, "PIN Incorrecto. Te quedan " + restantes + " intentos", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarCorreoSeguridad(String correoDestino) {
        new Thread(() -> {
            try {
                System.out.println("ALERTA DE SEGURIDAD SIMULADA ENVIADA A: " + correoDestino);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}