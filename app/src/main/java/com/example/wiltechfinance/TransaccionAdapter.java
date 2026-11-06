package com.example.wiltechfinance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;
import java.util.Locale;

public class TransaccionAdapter extends RecyclerView.Adapter<TransaccionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Transaccion transaccion);
        void onItemLongClick(Transaccion transaccion);
    }

    private final List<Transaccion> lista;
    private final OnItemClickListener listener;

    public TransaccionAdapter(List<Transaccion> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaccion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaccion item = lista.get(position);
        holder.tvConcepto.setText(item.getConcepto());
        holder.tvFecha.setText(item.getFecha());
        holder.tvMonto.setText(String.format(Locale.US, "$%,.2f", item.getMonto()));

        if ("Ingreso".equalsIgnoreCase(item.getTipo())) {
            holder.ivIcono.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcono.setColorFilter(Color.parseColor("#00E676")); // Verde neón legible
        } else {
            holder.ivIcono.setImageResource(android.R.drawable.ic_delete);
            holder.ivIcono.setColorFilter(Color.parseColor("#E50914")); // Rojo WilTech
        }

        // Revisar URL ya sea guardada como urlFoto o urlFactura
        String urlImagen = (item.getUrlFoto() != null && !item.getUrlFoto().isEmpty())
                ? item.getUrlFoto()
                : item.getUrlFactura();

        if (urlImagen != null && !urlImagen.isEmpty()) {
            holder.ivEvidencia.setVisibility(View.VISIBLE);
            holder.ivEvidencia.clearColorFilter();
            Glide.with(holder.itemView.getContext())
                    .load(urlImagen)
                    .transform(new CenterCrop(), new RoundedCorners(10))
                    .into(holder.ivEvidencia);
        } else {
            // Mostrar botón/ícono para añadir evidencia en vez de ocultarlo
            holder.ivEvidencia.setVisibility(View.VISIBLE);
            holder.ivEvidencia.setImageResource(android.R.drawable.ic_menu_camera);
            holder.ivEvidencia.setColorFilter(Color.parseColor("#555555"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConcepto, tvFecha, tvMonto;
        ImageView ivIcono, ivEvidencia;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConcepto = itemView.findViewById(R.id.tvConceptoItem);
            tvFecha = itemView.findViewById(R.id.tvFechaItem);
            tvMonto = itemView.findViewById(R.id.tvMontoItem);
            ivIcono = itemView.findViewById(R.id.ivTipoIcono);
            ivEvidencia = itemView.findViewById(R.id.ivEvidenciaFoto);
        }
    }
}