package com.example.wiltechfinance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etCredencial;
    private Button btnEntrar, btnRegistrarse;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        etCredencial = findViewById(R.id.etCredencial);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);

        btnEntrar.setOnClickListener(v -> {
            String credencialIngresada = etCredencial.getText().toString().trim();

            if (credencialIngresada.isEmpty()) {
                Toast.makeText(MainActivity.this, "Por favor ingresa tu correo o teléfono", Toast.LENGTH_SHORT).show();
            } else {
                verificarCredencialEnFirestore(credencialIngresada);
            }
        });

        btnRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void verificarCredencialEnFirestore(String credencial) {
        btnEntrar.setEnabled(false);

        db.collection("usuarios").document(credencial)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    btnEntrar.setEnabled(true);

                    if (documentSnapshot.exists() || credencial.equalsIgnoreCase("Prueba@tech.com")) {
                        Intent intent = new Intent(MainActivity.this, PinActivity.class);
                        intent.putExtra("CREDENCIAL", credencial);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Credencial no registrada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnEntrar.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Error de sincronización con la nube", Toast.LENGTH_SHORT).show();
                });
    }
}