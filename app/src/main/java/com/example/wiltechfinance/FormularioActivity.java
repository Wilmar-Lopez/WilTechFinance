package com.example.wiltechfinance;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FormularioActivity extends AppCompatActivity {

    private TextInputEditText etConcepto, etMonto;
    private TextInputLayout tilConcepto, tilMonto;
    private RadioGroup rgTipo;
    private RadioButton rbIngreso;
    private Button btnRegistrar, btnTomarFoto, btnCancelar;
    private ImageView ivVistaPreviaEvidencia;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri imagenSeleccionadaUri = null;
    private byte[] imagenCapturadaBytes = null;

    private String idDocumentoParaEditar = null;
    private String correoObjetivoTransaccion = null;

    private static final String CANAL_ID = "notificaciones_gastos";

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Permisos básicos solicitados
            });

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imagenSeleccionadaUri = result.getData().getData();
                    imagenCapturadaBytes = null;
                    ivVistaPreviaEvidencia.setImageURI(imagenSeleccionadaUri);
                    ivVistaPreviaEvidencia.setVisibility(View.VISIBLE);
                    btnTomarFoto.setText("CAMBIAR FOTO / FACTURA");
                }
            });

    private final ActivityResultLauncher<Intent> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivVistaPreviaEvidencia.setImageBitmap(imageBitmap);
                    ivVistaPreviaEvidencia.setVisibility(View.VISIBLE);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    imagenCapturadaBytes = baos.toByteArray();
                    imagenSeleccionadaUri = null;
                    btnTomarFoto.setText("CAMBIAR FOTO / FACTURA");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("evidencias_transacciones");

        if (getIntent() != null && getIntent().hasExtra("correoAuditado")) {
            correoObjetivoTransaccion = getIntent().getStringExtra("correoAuditado").toLowerCase().trim();
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                correoObjetivoTransaccion = user.getEmail().toLowerCase().trim();
            }
        }

        crearCanalNotificaciones();
        solicitarPermisos();

        etConcepto = findViewById(R.id.etConcepto);
        etMonto = findViewById(R.id.etMonto);
        tilConcepto = findViewById(R.id.tilConcepto);
        tilMonto = findViewById(R.id.tilMonto);
        rgTipo = findViewById(R.id.rgTipo);
        rbIngreso = findViewById(R.id.rbIngreso);
        ivVistaPreviaEvidencia = findViewById(R.id.ivVistaPreviaEvidencia);

        btnRegistrar = findViewById(R.id.btnRegistrarActividad);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnCancelar = findViewById(R.id.btnCancelarActividad);

        configurarBordeDinamico(tilConcepto, etConcepto);
        configurarBordeDinamico(tilMonto, etMonto);

        if (getIntent() != null && getIntent().hasExtra("idDocumento")) {
            idDocumentoParaEditar = getIntent().getStringExtra("idDocumento");
            String conceptoEditar = getIntent().getStringExtra("concepto");
            double montoEditar = getIntent().getDoubleExtra("monto", 0.0);

            etConcepto.setText(conceptoEditar);
            etMonto.setText(String.valueOf((long) montoEditar));
            btnRegistrar.setText("ACTUALIZAR MOVIMIENTO");
        }

        btnTomarFoto.setOnClickListener(v -> mostrarDialogoFoto());
        btnCancelar.setOnClickListener(v -> finish());
        btnRegistrar.setOnClickListener(v -> validarYProcesar());

        etMonto.addTextChangedListener(new TextWatcher() {
            private String actual = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(actual)) {
                    etMonto.removeTextChangedListener(this);
                    String limpio = s.toString().replaceAll("[.,]", "");
                    if (!limpio.isEmpty()) {
                        try {
                            double numero = Double.parseDouble(limpio);
                            DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
                            simbolos.setGroupingSeparator('.');
                            DecimalFormat formateador = new DecimalFormat("#,###", simbolos);
                            actual = formateador.format(numero);
                            etMonto.setText(actual);
                            etMonto.setSelection(actual.length());
                        } catch (NumberFormatException e) { }
                    } else {
                        actual = "";
                    }
                    etMonto.addTextChangedListener(this);
                }
            }
        });
    }

    private void configurarBordeDinamico(TextInputLayout til, TextInputEditText et) {
        int colorInactivo = Color.parseColor("#B3FFFFFF");
        int colorActivo = Color.parseColor("#E50914");
        til.setBoxStrokeColor(colorInactivo);
        et.setOnFocusChangeListener((v, hasFocus) -> til.setBoxStrokeColor(hasFocus ? colorActivo : colorInactivo));
    }

    private void solicitarPermisos() {
        String[] permisos = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_IMAGES} :
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        requestPermissionsLauncher.launch(permisos);
    }

    private void mostrarDialogoFoto() {
        String[] opciones = {"Tomar foto", "Elegir de galería"};
        new AlertDialog.Builder(this)
                .setTitle("Adjuntar Factura")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        camaraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    }
                }).show();
    }

    private void validarYProcesar() {
        String concepto = etConcepto.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();
        boolean valido = true;

        if (concepto.isEmpty()) { tilConcepto.setError("Ingresa un concepto"); valido = false; } else { tilConcepto.setErrorEnabled(false); }
        if (montoStr.isEmpty()) { tilMonto.setError("Ingresa el monto"); valido = false; } else { tilMonto.setErrorEnabled(false); }
        if (!valido) return;

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText(idDocumentoParaEditar != null ? "ACTUALIZANDO..." : "SUBIENDO DATOS...");

        String montoLimpio = montoStr.replaceAll("\\.", "");
        double montoIngresado = Double.parseDouble(montoLimpio);
        String tipo = rbIngreso.isChecked() ? "Ingreso" : "Gasto";
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (imagenSeleccionadaUri != null || imagenCapturadaBytes != null) {
            subirFotoYGuardar(concepto, montoIngresado, tipo, fechaActual);
        } else {
            guardarEnFirestore(concepto, montoIngresado, tipo, fechaActual, null);
        }
    }

    private void subirFotoYGuardar(String concepto, double monto, String tipo, String fecha) {
        String nombreArchivo = UUID.randomUUID().toString() + ".jpg";
        StorageReference fotoRef = storageRef.child(nombreArchivo);

        if (imagenCapturadaBytes != null) {
            fotoRef.putBytes(imagenCapturadaBytes).addOnSuccessListener(taskSnapshot -> obtenerUrl(fotoRef, concepto, monto, tipo, fecha))
                    .addOnFailureListener(e -> falloLectura());
        } else if (imagenSeleccionadaUri != null) {
            fotoRef.putFile(imagenSeleccionadaUri).addOnSuccessListener(taskSnapshot -> obtenerUrl(fotoRef, concepto, monto, tipo, fecha))
                    .addOnFailureListener(e -> falloLectura());
        }
    }

    private void obtenerUrl(StorageReference fotoRef, String concepto, double monto, String tipo, String fecha) {
        fotoRef.getDownloadUrl().addOnSuccessListener(uri -> guardarEnFirestore(concepto, monto, tipo, fecha, uri.toString()));
    }

    private void guardarEnFirestore(String concepto, double monto, String tipo, String fecha, String urlFotoDescarga) {
        if (idDocumentoParaEditar != null) {
            Map<String, Object> actualizaciones = new HashMap<>();
            actualizaciones.put("concepto", concepto);
            actualizaciones.put("monto", monto);
            actualizaciones.put("tipo", tipo);
            if (urlFotoDescarga != null) {
                actualizaciones.put("urlFoto", urlFotoDescarga);
                actualizaciones.put("urlFactura", urlFotoDescarga);
            }

            db.collection("transacciones").document(idDocumentoParaEditar).update(actualizaciones)
                    .addOnSuccessListener(aVoid -> finalizarYNotificar(concepto, monto, "¡Movimiento actualizado!"))
                    .addOnFailureListener(e -> falloLectura());
        } else {
            String urlSegura = (urlFotoDescarga == null) ? "" : urlFotoDescarga;

            Map<String, Object> nuevaTransaccion = new HashMap<>();
            nuevaTransaccion.put("concepto", concepto);
            nuevaTransaccion.put("monto", monto);
            nuevaTransaccion.put("tipo", tipo);
            nuevaTransaccion.put("fecha", fecha);
            nuevaTransaccion.put("urlFoto", urlSegura);
            nuevaTransaccion.put("urlFactura", urlSegura);
            nuevaTransaccion.put("correoUsuario", (correoObjetivoTransaccion != null ? correoObjetivoTransaccion : ""));

            db.collection("transacciones").add(nuevaTransaccion)
                    .addOnSuccessListener(documentReference -> finalizarYNotificar(concepto, monto, "¡Movimiento guardado!"))
                    .addOnFailureListener(e -> falloLectura());
        }
    }

    private void finalizarYNotificar(String concepto, double monto, String mensajeExito) {
        lanzarNotificacion(concepto, monto);
        Toast.makeText(this, mensajeExito, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void falloLectura() {
        btnRegistrar.setEnabled(true);
        btnRegistrar.setText(idDocumentoParaEditar != null ? "ACTUALIZAR MOVIMIENTO" : "GUARDAR MOVIMIENTO");
        Toast.makeText(this, "Error al guardar en la nube", Toast.LENGTH_SHORT).show();
    }

    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CANAL_ID, "Notificaciones de Gastos", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void lanzarNotificacion(String concepto, double monto) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Movimiento Registrado")
                .setContentText("Has registrado: " + concepto + " por $" + monto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, builder.build());
    }
}