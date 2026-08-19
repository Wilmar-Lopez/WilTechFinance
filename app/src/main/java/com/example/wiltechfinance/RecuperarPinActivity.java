package com.example.wiltechfinance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class RecuperarPinActivity extends AppCompatActivity {

    private EditText etCorreoRecuperar, etNuevoPin;
    private Button btnCambiarPin, btnVolverLogin;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_pin);

        db = FirebaseFirestore.getInstance();

        etCorreoRecuperar = findViewById(R.id.etCorreoRecuperar);
        etNuevoPin = findViewById(R.id.etNuevoPin);
        btnCambiarPin = findViewById(R.id.btnCambiarPin);
        btnVolverLogin = findViewById(R.id.btnVolverLoginRecuperar);

        btnCambiarPin.setOnClickListener(v -> actualizarPin());
        btnVolverLogin.setOnClickListener(v -> finish());
    }

    private void actualizarPin() {
        String correo = etCorreoRecuperar.getText().toString().trim();
        String nuevoPin = etNuevoPin.getText().toString().trim();

        if (correo.isEmpty() || nuevoPin.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nuevoPin.length() < 4) {
            Toast.makeText(this, "El nuevo PIN debe tener mínimo 4 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCambiarPin.setEnabled(false);

        db.collection("usuarios").document(correo)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        db.collection("usuarios").document(correo)
                                .update("pin", nuevoPin)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "¡PIN actualizado con éxito!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnCambiarPin.setEnabled(true);
                                    Toast.makeText(this, "Error al actualizar el PIN", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        btnCambiarPin.setEnabled(true);
                        Toast.makeText(this, "El correo no está registrado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnCambiarPin.setEnabled(true);
                    Toast.makeText(this, "Error de conexión con la nube", Toast.LENGTH_SHORT).show();
                });
    }
}