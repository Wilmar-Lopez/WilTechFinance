package com.example.wiltechfinance;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etTelefono, etCorreo, etPassword;
    private TextInputLayout tilNombre, tilTelefono, tilCorreo, tilPassword;
    private RadioGroup rgMetodoVerificacion;
    private RadioButton rbVerificarCorreo, rbVerificarSMS;
    private Button btnRegistrar, btnVolver;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String idVerificacionSMS;
    private PhoneAuthProvider.ForceResendingToken tokenReenvioSMS;
    private AlertDialog dialogoVerificacion; // Nueva ventana de espera

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombreRegistro);
        etTelefono = findViewById(R.id.etTelefonoRegistro);
        etCorreo = findViewById(R.id.etCorreoRegistro);
        etPassword = findViewById(R.id.etPasswordRegistro);

        tilNombre = findViewById(R.id.tilNombreRegistro);
        tilTelefono = findViewById(R.id.tilTelefonoRegistro);
        tilCorreo = findViewById(R.id.tilCorreoRegistro);
        tilPassword = findViewById(R.id.tilPasswordRegistro);

        rgMetodoVerificacion = findViewById(R.id.rgMetodoVerificacion);
        rbVerificarCorreo = findViewById(R.id.rbVerificarCorreo);
        rbVerificarSMS = findViewById(R.id.rbVerificarSMS);

        btnRegistrar = findViewById(R.id.btnRegistrarUsuario);
        btnVolver = findViewById(R.id.btnVolverLogin);

        configurarBordeDinamico(tilNombre, etNombre);
        configurarBordeDinamico(tilTelefono, etTelefono);
        configurarBordeDinamico(tilCorreo, etCorreo);
        configurarBordeDinamico(tilPassword, etPassword);

        // Ocultamos la vista del SMS del XML ya que usaremos el Diálogo
        View tilCodigoSMS = findViewById(R.id.tilCodigoSMS);
        View btnVerificarAcceso = findViewById(R.id.btnVerificarAcceso);
        if (tilCodigoSMS != null) tilCodigoSMS.setVisibility(View.GONE);
        if (btnVerificarAcceso != null) btnVerificarAcceso.setVisibility(View.GONE);

        btnRegistrar.setOnClickListener(v -> procesarRegistro());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void configurarBordeDinamico(TextInputLayout til, TextInputEditText et) {
        int colorInactivo = Color.parseColor("#B3FFFFFF");
        int colorActivo = Color.parseColor("#E50914");
        til.setBoxStrokeColor(colorInactivo);
        et.setOnFocusChangeListener((v, hasFocus) -> {
            til.setBoxStrokeColor(hasFocus ? colorActivo : colorInactivo);
        });
    }

    private void procesarRegistro() {
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean valido = true;

        if (nombre.isEmpty()) { tilNombre.setError("Requerido"); valido = false; } else { tilNombre.setErrorEnabled(false); }
        if (telefono.isEmpty()) { tilTelefono.setError("Requerido"); valido = false; } else { tilTelefono.setErrorEnabled(false); }
        if (correo.isEmpty()) { tilCorreo.setError("Requerido"); valido = false; } else { tilCorreo.setErrorEnabled(false); }
        if (password.length() < 6) { tilPassword.setError("Mínimo 6 caracteres"); valido = false; } else { tilPassword.setErrorEnabled(false); }

        if (!valido) return;

        btnRegistrar.setEnabled(false);

        if (rbVerificarCorreo.isChecked()) {
            iniciarRegistroPorCorreo(nombre, telefono, correo, password);
        } else {
            iniciarRegistroPorSMS(telefono);
        }
    }

    private void iniciarRegistroPorCorreo(String nombre, String telefono, String correo, String password) {
        btnRegistrar.setText("ENVIANDO CORREO...");

        mAuth.createUserWithEmailAndPassword(correo, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    mostrarPantallaEsperaCorreo();
                                } else {
                                    restaurarBoton();
                                    Toast.makeText(this, "Error enviando correo", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        restaurarBoton();
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void iniciarRegistroPorSMS(String telefono) {
        btnRegistrar.setText("SOLICITANDO SMS...");

        String telefonoFormateado = telefono.startsWith("+") ? telefono : "+57" + telefono;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(telefonoFormateado)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        if (dialogoVerificacion != null && dialogoVerificacion.isShowing()) {
                            dialogoVerificacion.dismiss();
                        }
                        completarRegistroFinal(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        restaurarBoton();
                        Toast.makeText(RegistroActivity.this, "Fallo SMS: Asegúrate de habilitar Phone Auth y agregar el SHA-1 en Firebase.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(s, token);
                        idVerificacionSMS = s;
                        tokenReenvioSMS = token;
                        mostrarPantallaEsperaSMS();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // --- PANTALLAS DE ESPERA (NUEVA EXPERIENCIA DE UI) ---

    private void mostrarPantallaEsperaCorreo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verificación Requerida");
        builder.setMessage("Hemos enviado un enlace a tu correo electrónico. Por favor, revísalo, presiona el enlace y luego vuelve aquí y presiona CONTINUAR.");
        builder.setCancelable(false);

        builder.setPositiveButton("CONTINUAR", (dialog, which) -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.reload().addOnCompleteListener(reloadTask -> {
                    if (user.isEmailVerified()) {
                        guardarDatosEnFirestore(user.getUid());
                    } else {
                        Toast.makeText(RegistroActivity.this, "Aún no está verificado. Revisa tu bandeja de entrada o SPAM.", Toast.LENGTH_LONG).show();
                        mostrarPantallaEsperaCorreo(); // Vuelve a mostrar si no ha verificado
                    }
                });
            }
        });

        builder.setNegativeButton("Cancelar Registro", (dialog, which) -> {
            if (mAuth.getCurrentUser() != null) mAuth.getCurrentUser().delete();
            restaurarBoton();
        });

        dialogoVerificacion = builder.create();
        dialogoVerificacion.show();
    }

    private void mostrarPantallaEsperaSMS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingresa tu Código SMS");
        builder.setMessage("Te hemos enviado un código de 6 dígitos por mensaje de texto.");
        builder.setCancelable(false);

        final EditText inputCodigo = new EditText(this);
        inputCodigo.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCodigo.setHint("Código de 6 dígitos");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        inputCodigo.setLayoutParams(lp);
        builder.setView(inputCodigo);

        builder.setPositiveButton("VERIFICAR Y CONTINUAR", (dialog, which) -> {
            String codigo = inputCodigo.getText().toString().trim();
            if (codigo.length() == 6 && idVerificacionSMS != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(idVerificacionSMS, codigo);
                completarRegistroFinal(credential);
            } else {
                Toast.makeText(RegistroActivity.this, "Código inválido. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                mostrarPantallaEsperaSMS();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> restaurarBoton());

        dialogoVerificacion = builder.create();
        dialogoVerificacion.show();
    }

    private void completarRegistroFinal(PhoneAuthCredential credential) {
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (mAuth.getCurrentUser() != null) {
            guardarDatosEnFirestore(mAuth.getCurrentUser().getUid());
        } else {
            mAuth.createUserWithEmailAndPassword(correo, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().linkWithCredential(credential)
                                    .addOnCompleteListener(linkTask -> guardarDatosEnFirestore(mAuth.getCurrentUser().getUid()));
                        } else {
                            Toast.makeText(RegistroActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void guardarDatosEnFirestore(String uid) {
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim().toLowerCase();

        Map<String, Object> perfilUsuario = new HashMap<>();
        perfilUsuario.put("uid", uid);
        perfilUsuario.put("nombre", nombre);
        perfilUsuario.put("telefono", telefono);
        perfilUsuario.put("correo", correo);
        perfilUsuario.put("rol", "Estándar");

        db.collection("usuarios").document(correo).set(perfilUsuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegistroActivity.this, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(RegistroActivity.this, "Error guardando perfil.", Toast.LENGTH_LONG).show());
    }

    private void restaurarBoton() {
        btnRegistrar.setEnabled(true);
        btnRegistrar.setText("REGISTRARSE");
    }
}