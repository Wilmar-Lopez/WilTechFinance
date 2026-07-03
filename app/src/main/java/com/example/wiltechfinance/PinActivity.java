package com.example.wiltechfinance; // OJO: Deja tu package original aquí arriba si cambia

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PinActivity extends AppCompatActivity {

    private EditText etPin;
    private Button btnConfirmarPin;
    private ConexionSQLite dbHelper;
    private String credencialUsuario;
    private int intentosFallidos = 0;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.etPin);
        btnConfirmarPin = findViewById(R.id.btnConfirmarPin);
        dbHelper = new ConexionSQLite(this);
        prefs = getSharedPreferences("SeguridadPrefs", Context.MODE_PRIVATE);

        credencialUsuario = getIntent().getStringExtra("CREDENCIAL");

        // Control del bloqueo de 3 horas
        long horaBloqueo = prefs.getLong("hora_bloqueo", 0);
        long horaActual = System.currentTimeMillis();

        if (horaBloqueo != 0 && (horaActual - horaBloqueo) < 10800000) {
            long tiempoRestanteMins = (10800000 - (horaActual - horaBloqueo)) / 60000;
            Toast.makeText(this, "App bloqueada. Intenta en " + tiempoRestanteMins + " minutos.", Toast.LENGTH_LONG).show();
            btnConfirmarPin.setEnabled(false);
        }

        btnConfirmarPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pinIngresado = etPin.getText().toString().trim();

                if (pinIngresado.length() < 4) {
                    Toast.makeText(PinActivity.this, "El PIN debe ser de 4 dígitos", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (validarPin(credencialUsuario, pinIngresado)) {
                    Toast.makeText(PinActivity.this, "¡Acceso Concedido!", Toast.LENGTH_SHORT).show();
                    intentosFallidos = 0;

                    // ==========================================
                    // 🔥 ¡REVISA ESTAS TRES LÍNEAS ABAJO! 🔥
                    // Esto es lo que hace que pase a la siguiente página
                    // ==========================================
                    Intent intent = new Intent(PinActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish(); // Cierra el PIN para que no regrese al darle atrás al celular

                } else {
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
            }
        });
    }

    private boolean validarPin(String credencial, String pin) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE credencial = ? AND pin = ?", new String[]{credencial, pin});
        boolean correcto = cursor.moveToFirst();
        cursor.close();
        return correcto;
    }

    private void enviarCorreoSeguridad(String correoDestino) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("ALERTA DE SEGURIDAD SIMULADA ENVIADA A: " + correoDestino);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}