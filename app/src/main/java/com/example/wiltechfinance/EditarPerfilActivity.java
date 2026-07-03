package com.example.wiltechfinance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Fuerza el enlace con los IDs del layout
import com.example.wiltechfinance.R;

public class EditarPerfilActivity extends AppCompatActivity {

    private ImageView imgFotoContacto;
    private EditText etPasswordVieja;

    private static final int REQ_COD_GALERIA = 101;
    private static final int REQ_COD_CAMARA = 102;
    private static final int PERMISO_GALERIA_CODE = 201;
    private static final int PERMISO_CAMARA_CODE = 202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        CardView cardFoto = findViewById(R.id.cardFotoEditar);
        imgFotoContacto = findViewById(R.id.imgFotoContacto);
        etPasswordVieja = findViewById(R.id.etPasswordVieja);

        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);
        Button btnCancelar = findViewById(R.id.btnCancelarPerfil);

        cardFoto.setOnClickListener(v -> mostrarOpcionesImagen());

        btnGuardar.setOnClickListener(v -> {
            String passVieja = etPasswordVieja.getText().toString().trim();

            if (passVieja.isEmpty()) {
                Toast.makeText(this, "Por seguridad, debes ingresar tu contraseña actual", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!passVieja.equals("1234")) {
                Toast.makeText(this, "Contraseña actual incorrecta. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnCancelar.setOnClickListener(v -> finish());
    }

    private void mostrarOpcionesImagen() {
        String[] opciones = {"Tomar foto con la cámara", "Agregar de la galería", "Eliminar foto actual"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona una opción");
        builder.setItems(opciones, (dialog, index) -> {
            if (index == 0) {
                verificarPermisosCamara();
            } else if (index == 1) {
                verificarPermisosGaleria();
            } else if (index == 2) {
                imgFotoContacto.setImageResource(android.R.drawable.ic_menu_camera);
                imgFotoContacto.setPadding(30, 30, 30, 30);
                imgFotoContacto.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8C8C8C")));
                Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void verificarPermisosCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamaraReal();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISO_CAMARA_CODE);
        }
    }

    private void verificarPermisosGaleria() {
        String permisoNecesario = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permisoNecesario) == PackageManager.PERMISSION_GRANTED) {
            abrirGaleriaReal();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permisoNecesario}, PERMISO_GALERIA_CODE);
        }
    }

    private void abrirCamaraReal() {
        Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intentCamara.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intentCamara, REQ_COD_CAMARA);
        }
    }

    private void abrirGaleriaReal() {
        Intent intentGaleria = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentGaleria, REQ_COD_GALERIA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISO_CAMARA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamaraReal();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISO_GALERIA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirGaleriaReal();
            } else {
                Toast.makeText(this, "Permiso de galería denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            imgFotoContacto.setPadding(0, 0, 0, 0);
            imgFotoContacto.setImageTintList(null);

            if (requestCode == REQ_COD_GALERIA && data.getData() != null) {
                Uri rutaImagen = data.getData();
                imgFotoContacto.setImageURI(rutaImagen);
                Toast.makeText(this, "Foto cargada desde galería", Toast.LENGTH_SHORT).show();

            } else if (requestCode == REQ_COD_CAMARA) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap fotoCamara = (Bitmap) extras.get("data");
                    imgFotoContacto.setImageBitmap(fotoCamara);
                    Toast.makeText(this, "Foto capturada con éxito", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}