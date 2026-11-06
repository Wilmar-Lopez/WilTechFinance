package com.example.wiltechfinance;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransaccionAdapter adapter;
    private List<Transaccion> listaTransacciones;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ListenerRegistration firestoreListener;

    private String idTransaccionSeleccionada = null;
    private String correoAuditado = null;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) {
                    mostrarDialogoOrigenImagen();
                } else {
                    Toast.makeText(getContext(), "Permiso de cámara necesario para tomar foto", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && idTransaccionSeleccionada != null) {
                        subirFacturaPorUri(uri, idTransaccionSeleccionada);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        if (bitmap != null && idTransaccionSeleccionada != null) {
                            subirFacturaPorBitmap(bitmap, idTransaccionSeleccionada);
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_historial, container, false);

        if (getArguments() != null) {
            correoAuditado = getArguments().getString("correoAuditado");
        }

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("facturas_transacciones");

        recyclerView = vista.findViewById(R.id.recyclerViewHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaTransacciones = new ArrayList<>();

        adapter = new TransaccionAdapter(listaTransacciones, new TransaccionAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Transaccion transaccion) {
                mostrarOpcionesTransaccion(transaccion);
            }

            @Override
            public void onItemLongClick(Transaccion transaccion) {
                boolean esAdmin = false;
                if (getActivity() instanceof HomeActivity) {
                    esAdmin = ((HomeActivity) getActivity()).esAdmin();
                }

                if (esAdmin) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar Transacción")
                            .setMessage("¿Deseas eliminar este registro de la nube?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                if (transaccion.getIdDocumento() != null) {
                                    db.collection("transacciones")
                                            .document(transaccion.getIdDocumento())
                                            .delete()
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(getContext(), "Registro eliminado", Toast.LENGTH_SHORT).show()
                                            );
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "Acción denegada: Solo los administradores pueden eliminar transacciones.", Toast.LENGTH_LONG).show();
                }
            }
        });

        recyclerView.setAdapter(adapter);
        escucharTransaccionesEnTiempoReal();

        return vista;
    }

    private String obtenerCorreoObjetivo() {
        if (correoAuditado != null && !correoAuditado.isEmpty()) {
            return correoAuditado.toLowerCase().trim();
        }
        if (getActivity() instanceof HomeActivity) {
            String correoHome = ((HomeActivity) getActivity()).getCorreoActivo();
            if (correoHome != null && !correoHome.isEmpty()) {
                return correoHome;
            }
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null && user.getEmail() != null) ? user.getEmail().toLowerCase().trim() : "";
    }

    private void mostrarOpcionesTransaccion(Transaccion t) {
        idTransaccionSeleccionada = t.getIdDocumento();
        boolean tieneFoto = t.getUrlFoto() != null && !t.getUrlFoto().isEmpty();

        boolean esAdmin = false;
        if (getActivity() instanceof HomeActivity) {
            esAdmin = ((HomeActivity) getActivity()).esAdmin();
        }

        List<String> opciones = new ArrayList<>();
        if (tieneFoto) {
            opciones.add("Ver Factura / Comprobante");
            opciones.add("Cambiar Foto de Factura");
        } else {
            opciones.add("Adjuntar Foto de Factura");
        }

        // Solo el Admin puede editar transacciones existentes
        if (esAdmin) {
            opciones.add("Editar Transacción");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(t.getConcepto() + " ($" + t.getMonto() + ")")
                .setItems(opciones.toArray(new String[0]), (dialog, which) -> {
                    String seleccion = opciones.get(which);
                    if (seleccion.equals("Ver Factura / Comprobante")) {
                        verFacturaGrande(t.getUrlFoto());
                    } else if (seleccion.contains("Factura")) {
                        verificarPermisosYSubir();
                    } else if (seleccion.equals("Editar Transacción")) {
                        Intent intent = new Intent(getContext(), FormularioActivity.class);
                        intent.putExtra("idDocumento", t.getIdDocumento());
                        intent.putExtra("concepto", t.getConcepto());
                        intent.putExtra("monto", t.getMonto());
                        if (correoAuditado != null) {
                            intent.putExtra("correoAuditado", correoAuditado);
                        }
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void verFacturaGrande(String urlFoto) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        ImageView imgVista = new ImageView(requireContext());
        imgVista.setAdjustViewBounds(true);
        imgVista.setPadding(20, 20, 20, 20);

        Glide.with(this).load(urlFoto).into(imgVista);

        builder.setView(imgVista)
                .setTitle("Comprobante de Transacción")
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void verificarPermisosYSubir() {
        String[] permisos = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES} :
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean camaraOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (camaraOk) {
            mostrarDialogoOrigenImagen();
        } else {
            requestPermissionsLauncher.launch(permisos);
        }
    }

    private void mostrarDialogoOrigenImagen() {
        String[] origenes = {"Tomar foto con Cámara", "Elegir de Galería"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar Factura")
                .setItems(origenes, (dialog, which) -> {
                    if (which == 0) {
                        camaraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    }
                })
                .show();
    }

    private void subirFacturaPorBitmap(Bitmap bitmap, String idDoc) {
        Toast.makeText(getContext(), "Subiendo factura...", Toast.LENGTH_SHORT).show();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] bytes = baos.toByteArray();

        String nombreArchivo = idDoc + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storageRef.child(nombreArchivo);

        ref.putBytes(bytes).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri ->
                        actualizarUrlEnFirestore(idDoc, uri.toString())
                )
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void subirFacturaPorUri(Uri uri, String idDoc) {
        Toast.makeText(getContext(), "Subiendo factura...", Toast.LENGTH_SHORT).show();
        String nombreArchivo = idDoc + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storageRef.child(nombreArchivo);

        ref.putFile(uri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                        actualizarUrlEnFirestore(idDoc, downloadUri.toString())
                )
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void actualizarUrlEnFirestore(String idDoc, String urlDescarga) {
        Map<String, Object> update = new HashMap<>();
        update.put("urlFoto", urlDescarga);
        update.put("urlFactura", urlDescarga);

        db.collection("transacciones").document(idDoc).update(update)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "¡Factura adjuntada con éxito!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al guardar factura", Toast.LENGTH_SHORT).show()
                );
    }

    private void escucharTransaccionesEnTiempoReal() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        String correoObjetivo = obtenerCorreoObjetivo();
        if (correoObjetivo.isEmpty()) return;

        firestoreListener = db.collection("transacciones")
                .whereEqualTo("correoUsuario", correoObjetivo)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    listaTransacciones.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Transaccion t = doc.toObject(Transaccion.class);
                        if (t != null) {
                            t.setIdDocumento(doc.getId());
                            listaTransacciones.add(t);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}