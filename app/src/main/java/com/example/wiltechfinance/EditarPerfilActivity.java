package com.example.wiltechfinance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditarPerfilActivity extends AppCompatActivity {

    private ImageView imgFotoContacto;
    private EditText etEditarNombre, etEditarTelefono, etPasswordVieja, etPasswordNueva;
    private EditText etEditarCorreo; // Para mostrar el correo real bloqueado si existe en el layout
    private Button btnGuardar, btnCancelar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri imagenSeleccionadaUri = null;
    private byte[] imagenCapturadaBytes = null;
    private String urlFotoActual = "";
    private String documentIdUsuario = null;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
                    mostrarOpcionesImagen();
                }
            });

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imagenSeleccionadaUri = result.getData().getData();
                    imagenCapturadaBytes = null;
                    Glide.with(this).load(imagenSeleccionadaUri).transform(new CircleCrop()).into(imgFotoContacto);
                }
            });

    private final ActivityResultLauncher<Intent> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    Glide.with(this).load(imageBitmap).transform(new CircleCrop()).into(imgFotoContacto);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    imagenCapturadaBytes = baos.toByteArray();
                    imagenSeleccionadaUri = null;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("fotos_perfil");

        CardView cardFoto = findViewById(R.id.cardFotoEditar);
        imgFotoContacto = findViewById(R.id.imgFotoContacto);
        etEditarNombre = findViewById(R.id.etEditarNombre);
        etEditarTelefono = findViewById(R.id.etEditarTelefono);
        etPasswordVieja = findViewById(R.id.etPasswordVieja);
        etPasswordNueva = findViewById(R.id.etPasswordNueva);

        // Si tu layout tiene un campo para el correo, lo enlazamos y deshabilitamos
        int idCorreo = getResources().getIdentifier("etEditarCorreo", "id", getPackageName());
        if (idCorreo != 0) {
            etEditarCorreo = findViewById(idCorreo);
            if (etEditarCorreo != null) {
                etEditarCorreo.setEnabled(false);
                etEditarCorreo.setFocusable(false);
            }
        }

        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        btnCancelar = findViewById(R.id.btnCancelarPerfil);

        cargarDatosActuales();

        cardFoto.setOnClickListener(v -> verificarPermisosYMostrarOpciones());
        btnGuardar.setOnClickListener(v -> validarYReautenticar());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void cargarDatosActuales() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        String correoDoc = user.getEmail().toLowerCase().trim();
        documentIdUsuario = correoDoc;

        // Cargar directamente con el ID estándar (correo electrónico en minúsculas)
        db.collection("usuarios").document(correoDoc).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        llenarCamposDesdeDoc(doc);
                    } else {
                        // Respaldo por si quedó con UID antiguo
                        buscarPorUidRespaldo(user.getUid());
                    }
                })
                .addOnFailureListener(e -> buscarPorUidRespaldo(user.getUid()));
    }

    private void buscarPorUidRespaldo(String uid) {
        db.collection("usuarios").whereEqualTo("uid", uid).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        DocumentSnapshot doc = querySnapshots.getDocuments().get(0);
                        documentIdUsuario = doc.getId();
                        llenarCamposDesdeDoc(doc);
                    }
                });
    }

    private void llenarCamposDesdeDoc(DocumentSnapshot doc) {
        String nombre = doc.getString("nombre");
        String telefono = doc.getString("telefono");
        String correo = doc.getString("correo");
        urlFotoActual = doc.getString("urlFoto");

        if (nombre != null) etEditarNombre.setText(nombre);
        if (telefono != null) etEditarTelefono.setText(telefono);
        if (correo != null && etEditarCorreo != null) etEditarCorreo.setText(correo);

        if (urlFotoActual != null && !urlFotoActual.isEmpty()) {
            Glide.with(this).load(urlFotoActual).transform(new CircleCrop()).into(imgFotoContacto);
        }
    }

    private void verificarPermisosYMostrarOpciones() {
        String[] permisos = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES} :
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        requestPermissionsLauncher.launch(permisos);
    }

    private void mostrarOpcionesImagen() {
        String[] opciones = {"Tomar foto", "Elegir de galería"};
        new AlertDialog.Builder(this)
                .setTitle("Cambiar Foto de Perfil")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        camaraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    }
                }).show();
    }

    private void validarYReautenticar() {
        String passVieja = etPasswordVieja.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (passVieja.isEmpty() || user == null || user.getEmail() == null) {
            Toast.makeText(this, "Ingresa tu contraseña actual por seguridad", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("VERIFICANDO...");

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), passVieja);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                procesarActualizacion(user);
            } else {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("GUARDAR CAMBIOS");
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarActualizacion(FirebaseUser user) {
        String nuevoPass = etPasswordNueva.getText().toString().trim();
        if (!nuevoPass.isEmpty() && nuevoPass.length() >= 6) {
            user.updatePassword(nuevoPass);
        }

        if (imagenCapturadaBytes != null || imagenSeleccionadaUri != null) {
            subirNuevaFotoYGuardar();
        } else {
            guardarDatosEnFirestore(urlFotoActual);
        }
    }

    private void subirNuevaFotoYGuardar() {
        String nombreArchivo = UUID.randomUUID().toString() + ".jpg";
        StorageReference fotoRef = storageRef.child(nombreArchivo);

        if (imagenCapturadaBytes != null) {
            fotoRef.putBytes(imagenCapturadaBytes).addOnSuccessListener(task -> obtenerUrlYGuardar(fotoRef));
        } else if (imagenSeleccionadaUri != null) {
            fotoRef.putFile(imagenSeleccionadaUri).addOnSuccessListener(task -> obtenerUrlYGuardar(fotoRef));
        }
    }

    private void obtenerUrlYGuardar(StorageReference fotoRef) {
        fotoRef.getDownloadUrl().addOnSuccessListener(uri -> guardarDatosEnFirestore(uri.toString()));
    }

    private void guardarDatosEnFirestore(String urlFoto) {
        String nombre = etEditarNombre.getText().toString().trim();
        String telefono = etEditarTelefono.getText().toString().trim();

        Map<String, Object> actualizaciones = new HashMap<>();
        actualizaciones.put("nombre", nombre);
        actualizaciones.put("telefono", telefono);
        if (urlFoto != null && !urlFoto.isEmpty()) {
            actualizaciones.put("urlFoto", urlFoto);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        String targetDoc = (documentIdUsuario != null) ? documentIdUsuario :
                (user != null && user.getEmail() != null ? user.getEmail().toLowerCase().trim() : "desconocido");

        db.collection("usuarios").document(targetDoc).set(actualizaciones, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(err -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR CAMBIOS");
                    Toast.makeText(this, "Error al guardar: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}