package com.example.wiltechfinance; // OJO: Deja tu package original aquí arriba

import android.content.Intent; // <- ESTA LÍNEA ES LA CONEXIÓN CLAVE
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etCredencial;
    private Button btnEntrar;
    private ConexionSQLite dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCredencial = findViewById(R.id.etCredencial);
        btnEntrar = findViewById(R.id.btnEntrar);
        dbHelper = new ConexionSQLite(this);

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String credencialIngresada = etCredencial.getText().toString().trim();

                if (credencialIngresada.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Por favor ingresa tu correo o teléfono", Toast.LENGTH_SHORT).show();
                } else {
                    // 1. Buscamos si el correo existe en la Base de Datos con escudo de protección
                    if (buscarCredencial(credencialIngresada)) {

                        // 2. CONEXIÓN: Creamos el Intent para saltar a PinActivity
                        Intent intent = new Intent(MainActivity.this, PinActivity.class);

                        // 3. Guardamos el correo en la maleta para que PinActivity sepa de quién es el PIN
                        intent.putExtra("CREDENCIAL", credencialIngresada);

                        // 4. Arrancamos la nueva pantalla
                        startActivity(intent);

                    } else {
                        Toast.makeText(MainActivity.this, "Credencial no registrada", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private boolean buscarCredencial(String credencial) {
        // 🔥 CORREGIDO Y BLINDADO: Agregamos un try-catch para que si la base de datos falla, la app NO se cierre.
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // La consulta busca correctamente 'credencial' tal cual como tu tabla principal
            Cursor cursor = db.rawQuery("SELECT * FROM usuarios WHERE credencial = ?", new String[]{credencial});

            boolean existe = false;
            if (cursor != null) {
                existe = cursor.moveToFirst();
                cursor.close();
            }
            return existe;
        } catch (Exception e) {
            // Si la base de datos se confunde de columnas, muestra el error en texto en lugar de cerrarse
            Toast.makeText(this, "Error de sincronización de datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}