package com.example.wiltechfinance;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransaccionAdapter adapter;
    private List<Transaccion> listaTransacciones;
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_historial, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = vista.findViewById(R.id.recyclerViewHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaTransacciones = new ArrayList<>();

        adapter = new TransaccionAdapter(listaTransacciones, new TransaccionAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Transaccion transaccion) {
                // Abre la pantalla para editar enviando los datos del documento
                Intent intent = new Intent(getContext(), FormularioActivity.class);
                intent.putExtra("idDocumento", transaccion.getIdDocumento());
                intent.putExtra("concepto", transaccion.getConcepto());
                intent.putExtra("monto", transaccion.getMonto());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Transaccion transaccion) {
                // Diálogo para eliminar documento directamente en la nube
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
            }
        });

        recyclerView.setAdapter(adapter);
        escucharTransaccionesEnTiempoReal();

        return vista;
    }

    private void escucharTransaccionesEnTiempoReal() {
        // Escucha cambios automáticos ordenados por fecha descendente
        firestoreListener = db.collection("transacciones")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    listaTransacciones.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Transaccion t = doc.toObject(Transaccion.class);
                        if (t != null) {
                            t.setIdDocumento(doc.getId()); // Inyecta el ID NoSQL del documento
                            listaTransacciones.add(t);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancela la suscripción a Firestore al cerrar el fragmento para liberar memoria
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}