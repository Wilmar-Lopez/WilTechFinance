package com.example.wiltechfinance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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
        holder.tvMonto.setText(String.format("$%.2f", item.getMonto()));

        if ("Ingreso".equalsIgnoreCase(item.getTipo())) {
            holder.ivIcono.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcono.setColorFilter(0xFF00FF00);
        } else {
            holder.ivIcono.setImageResource(android.R.drawable.ic_delete);
            holder.ivIcono.setColorFilter(0xFFE50914);
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
        ImageView ivIcono;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConcepto = itemView.findViewById(R.id.tvConceptoItem);
            tvFecha = itemView.findViewById(R.id.tvFechaItem);
            tvMonto = itemView.findViewById(R.id.tvMontoItem);
            ivIcono = itemView.findViewById(R.id.ivTipoIcono);
        }
    }
}