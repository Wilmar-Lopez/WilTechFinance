package com.example.wiltechfinance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etCorreoLogin, etPasswordLogin;
    private TextInputLayout tilCorreoLogin, tilPasswordLogin;
    private Button btnEntrar, btnRegistrarse;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE);

        // Auto-Login: Si ya hay un usuario logueado en FirebaseAuth
        if (mAuth.getCurrentUser() != null) {
            irAlHome();
            return;
        }

        etCorreoLogin = findViewById(R.id.etCorreoLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        tilCorreoLogin = findViewById(R.id.tilCorreoLogin);
        tilPasswordLogin = findViewById(R.id.tilPasswordLogin);

        btnEntrar = findViewById(R.id.btnEntrar);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);

        // Control dinámico de bordes en Java
        configurarBordeDinamico(tilCorreoLogin, etCorreoLogin);
        configurarBordeDinamico(tilPasswordLogin, etPasswordLogin);

        btnEntrar.setOnClickListener(v -> iniciarSesion());

        btnRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void configurarBordeDinamico(TextInputLayout til, TextInputEditText et) {
        int colorInactivo = Color.parseColor("#B3FFFFFF"); // Blanco 70% nítido
        int colorActivo = Color.parseColor("#E50914");   // Rojo activo

        til.setBoxStrokeColor(colorInactivo);

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                til.setBoxStrokeColor(colorActivo);
            } else {
                til.setBoxStrokeColor(colorInactivo);
            }
        });
    }

    private void iniciarSesion() {
        String correo = etCorreoLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString().trim();
        boolean valido = true;

        if (correo.isEmpty()) {
            tilCorreoLogin.setError("Ingresa tu correo");
            valido = false;
        } else {
            tilCorreoLogin.setErrorEnabled(false);
        }

        if (password.isEmpty()) {
            tilPasswordLogin.setError("Ingresa tu contraseña");
            valido = false;
        } else {
            tilPasswordLogin.setErrorEnabled(false);
        }

        if (!valido) return;

        btnEntrar.setEnabled(false);
        btnEntrar.setText("VERIFICANDO...");

        mAuth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        // Pasamos el correo y el UID para hacer una búsqueda estricta y sin cruces
                        String userEmail = mAuth.getCurrentUser().getEmail();
                        String uid = mAuth.getCurrentUser().getUid();
                        obtenerRolYContinuar(userEmail, uid);
                    } else {
                        btnEntrar.setEnabled(true);
                        btnEntrar.setText("INICIAR SESIÓN");
                        Toast.makeText(MainActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void obtenerRolYContinuar(String correo, String uid) {
        String correoNormalizado = correo != null ? correo.toLowerCase() : "";

        // 1. Intento principal: Buscar por el correo como ID de documento (Nuevo estándar)
        db.collection("usuarios").document(correoNormalizado).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        guardarSesionYContinuar(documentSnapshot);
                    } else {
                        // 2. Si es una cuenta antigua, buscar obligatoriamente donde el campo "correo" coincida
                        buscarPorCampos(correoNormalizado, uid);
                    }
                })
                .addOnFailureListener(e -> buscarPorCampos(correoNormalizado, uid));
    }

    private void buscarPorCampos(String correo, String uid) {
        db.collection("usuarios").whereEqualTo("correo", correo).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        DocumentSnapshot doc = querySnapshots.getDocuments().get(0);
                        guardarSesionYContinuar(doc);
                    } else {
                        // 3. Último recurso: Buscar por UID por si el campo correo estaba vacío en la base de datos
                        db.collection("usuarios").whereEqualTo("uid", uid).limit(1).get()
                                .addOnSuccessListener(queryUid -> {
                                    if (!queryUid.isEmpty()) {
                                        guardarSesionYContinuar(queryUid.getDocuments().get(0));
                                    } else {
                                        mostrarErrorPerfil();
                                    }
                                })
                                .addOnFailureListener(e -> mostrarErrorPerfil());
                    }
                })
                .addOnFailureListener(e -> mostrarErrorPerfil());
    }

    private void mostrarErrorPerfil() {
        btnEntrar.setEnabled(true);
        btnEntrar.setText("INICIAR SESIÓN");
        Toast.makeText(MainActivity.this, "No se encontró perfil para este usuario", Toast.LENGTH_LONG).show();
        mAuth.signOut(); // Limpiamos sesión fantasma
    }

    private void guardarSesionYContinuar(DocumentSnapshot doc) {
        String nombre = doc.getString("nombre");
        String rol = doc.getString("rol");
        String telefono = doc.getString("telefono");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nombre", nombre);
        editor.putString("rol", rol);
        editor.putString("telefono", telefono);
        editor.apply();

        irAlHome();
    }

    private void irAlHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}