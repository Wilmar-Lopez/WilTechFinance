package com.example.wiltechfinance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etPin;
    private Button btnRegistrar, btnVolver;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        db = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombreRegistro);
        etCorreo = findViewById(R.id.etCorreoRegistro);
        etPin = findViewById(R.id.etPinRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrarUsuario);
        btnVolver = findViewById(R.id.btnVolverLogin);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String pin = etPin.getText().toString().trim();

        if (nombre.isEmpty() || correo.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pin.length() < 4) {
            Toast.makeText(this, "El PIN debe ser de mínimo 4 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegistrar.setEnabled(false);

        db.collection("usuarios").document(correo)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        btnRegistrar.setEnabled(true);
                        Toast.makeText(this, "Esta credencial ya se encuentra registrada", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("nombre", nombre);
                        usuario.put("correo", correo);
                        usuario.put("pin", pin);

                        db.collection("usuarios").document(correo)
                                .set(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnRegistrar.setEnabled(true);
                                    Toast.makeText(this, "Error al guardar en la nube", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(this, "Error de conexión con Firestore", Toast.LENGTH_SHORT).show();
                });
    }
}