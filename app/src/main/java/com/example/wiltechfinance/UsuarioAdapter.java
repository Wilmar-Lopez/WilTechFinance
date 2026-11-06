package com.example.wiltechfinance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Usuario usuario);
    }

    private final List<Usuario> lista;
    private final OnItemClickListener listener;

    public UsuarioAdapter(List<Usuario> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public UsuarioAdapter(List<Usuario> lista) {
        this(lista, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario u = lista.get(position);
        holder.tvNombre.setText(u.getNombre());
        holder.tvCorreo.setText(u.getCorreo());

        String rol = (u.getRol() != null) ? u.getRol().trim() : "Estándar";
        holder.tvRol.setText("Rol: " + rol);

        if ("Administrador".equalsIgnoreCase(rol) || "Admin".equalsIgnoreCase(rol)) {
            holder.tvRol.setTextColor(Color.parseColor("#E50914")); // Rojo si es Admin
        } else {
            holder.tvRol.setTextColor(Color.parseColor("#00E676")); // Verde si es Estándar
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(u);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCorreo, tvRol;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreUser);
            tvCorreo = itemView.findViewById(R.id.tvCorreoUser);
            tvRol = itemView.findViewById(R.id.tvRolUser);
        }
    }
}