package com.example.wiltechfinance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUsuariosFragment extends Fragment {

    private RecyclerView recyclerView;
    private UsuarioAdapter adapter;
    private List<Usuario> listaUsuarios;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_admin_usuarios, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = vista.findViewById(R.id.recyclerViewUsuarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaUsuarios = new ArrayList<>();

        // Configuramos el adapter con el evento de clic sobre cada usuario
        adapter = new UsuarioAdapter(listaUsuarios, usuarioSeleccionado -> {
            if (getActivity() instanceof HomeActivity) {
                String correo = usuarioSeleccionado.getCorreo();
                String nombre = usuarioSeleccionado.getNombre() != null ? usuarioSeleccionado.getNombre() : correo;

                if (correo != null && !correo.isEmpty()) {
                    ((HomeActivity) getActivity()).iniciarAuditoria(correo, nombre);
                } else {
                    Toast.makeText(getContext(), "El usuario no tiene un correo válido registrado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setAdapter(adapter);

        cargarUsuarios();

        return vista;
    }

    private void cargarUsuarios() {
        db.collection("usuarios").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaUsuarios.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Usuario u = doc.toObject(Usuario.class);
                        if (u != null) {
                            // Garantizar que el correo esté asignado aunque venga como ID de documento
                            if (u.getCorreo() == null || u.getCorreo().isEmpty()) {
                                u.setCorreo(doc.getId());
                            }
                            listaUsuarios.add(u);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar usuarios: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}