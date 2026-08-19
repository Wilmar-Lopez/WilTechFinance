package com.example.wiltechfinance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraficaFragment extends Fragment {

    private TextView tvGastoMasAlto, tvMontoMasAltoSemanal;
    private PlanoCartesianoView planoDiario, planoSemanal;
    private NumberFormat formatoDinero;
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_grafica, container, false);

        db = FirebaseFirestore.getInstance();
        formatoDinero = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        tvGastoMasAlto = vista.findViewById(R.id.tvGastoMasAlto);
        tvMontoMasAltoSemanal = vista.findViewById(R.id.tvMontoMasAltoSemanal);
        planoDiario = vista.findViewById(R.id.planoCartesianoDiario);
        planoSemanal = vista.findViewById(R.id.planoCartesianoSemanal);

        escucharFlujoCartesianoEnTiempoReal();
        return vista;
    }

    public void escucharFlujoCartesianoEnTiempoReal() {
        if (planoDiario == null || planoSemanal == null) return;

        firestoreListener = db.collection("transacciones")
                .orderBy("fecha", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<Transaccion> transacciones = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Transaccion t = doc.toObject(Transaccion.class);
                        if (t != null) {
                            transacciones.add(t);
                        }
                    }

                    procesarGraficas(transacciones);
                });
    }

    private void procesarGraficas(List<Transaccion> transacciones) {
        // ------------------------------------------------------------------
        // LÓGICA 1: GRÁFICA DIARIA
        // ------------------------------------------------------------------
        List<Double> valoresDiariosY = new ArrayList<>();
        List<String> etiquetasDiariasX = new ArrayList<>();
        double gastoMasAltoDeHoy = 0;
        double saldoCorrienteDiario = 0.0;
        int contadorHoras = 1;
        boolean esPrimeraTransaccion = true;

        for (Transaccion t : transacciones) {
            String concepto = t.getConcepto() != null ? t.getConcepto() : "";
            double monto = t.getMonto();
            String tipo = t.getTipo() != null ? t.getTipo() : "Ingreso";

            if ("Gasto".equalsIgnoreCase(tipo) && monto > gastoMasAltoDeHoy) {
                gastoMasAltoDeHoy = monto;
            }

            saldoCorrienteDiario += "Ingreso".equalsIgnoreCase(tipo) ? monto : -monto;
            valoresDiariosY.add(saldoCorrienteDiario);

            if (concepto.length() >= 5 && concepto.substring(0, 2).matches("\\d+") && concepto.contains("-")) {
                String horaExtraida = concepto.split("-")[0].trim();
                etiquetasDiariasX.add(horaExtraida);
            } else {
                if (esPrimeraTransaccion) {
                    etiquetasDiariasX.add("08:00 AM");
                } else {
                    etiquetasDiariasX.add(String.format(Locale.US, "%02d:00 PM", 12 + contadorHoras));
                    contadorHoras++;
                }
            }
            esPrimeraTransaccion = false;
        }

        tvGastoMasAlto.setText("Gasto más alto de hoy: " + formatoDinero.format(gastoMasAltoDeHoy));

        double minDiario = valoresDiariosY.isEmpty() ? 0 : valoresDiariosY.get(0);
        double maxDiario = valoresDiariosY.isEmpty() ? 100000 : valoresDiariosY.get(0);
        for (double v : valoresDiariosY) {
            if (v < minDiario) minDiario = v;
            if (v > maxDiario) maxDiario = v;
        }

        double rangoDiario = maxDiario - minDiario;
        double margenDiario = rangoDiario > 0 ? rangoDiario * 0.05 : 100000.00;

        double techoEjeYDiario = maxDiario + margenDiario;
        double pisoEjeYDiario = minDiario - margenDiario;

        if (pisoEjeYDiario < 0 || minDiario >= 500000) {
            if (pisoEjeYDiario < 0) pisoEjeYDiario = 0;
            else pisoEjeYDiario = minDiario - 200000;
        }

        if (!valoresDiariosY.isEmpty()) {
            planoDiario.setDatos(valoresDiariosY, etiquetasDiariasX, techoEjeYDiario);
        }

        // ------------------------------------------------------------------
        // LÓGICA 2: GRÁFICA SEMANAL
        // ------------------------------------------------------------------
        List<Double> valoresSemanalesY = new ArrayList<>();
        List<String> etiquetasSemanalesX = new ArrayList<>();
        String[] diasSemana = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        double saldoHistoricoAcumulado = 0.0;

        for (int i = 0; i < diasSemana.length; i++) {
            etiquetasSemanalesX.add(diasSemana[i]);

            if (i < transacciones.size()) {
                Transaccion t = transacciones.get(i);
                double monto = t.getMonto();
                String tipo = t.getTipo() != null ? t.getTipo() : "Ingreso";
                saldoHistoricoAcumulado += "Ingreso".equalsIgnoreCase(tipo) ? monto : -monto;
            }

            valoresSemanalesY.add(saldoHistoricoAcumulado);
        }

        double minSemanal = valoresSemanalesY.isEmpty() ? 0 : valoresSemanalesY.get(0);
        double maxSemanal = valoresSemanalesY.isEmpty() ? 100000 : valoresSemanalesY.get(0);
        for (double v : valoresSemanalesY) {
            if (v < minSemanal) minSemanal = v;
            if (v > maxSemanal) maxSemanal = v;
        }

        tvMontoMasAltoSemanal.setText("Monto más alto de la semana: " + formatoDinero.format(maxSemanal));

        double rangoSemanal = maxSemanal - minSemanal;
        double margenSemanal = rangoSemanal > 0 ? rangoSemanal * 0.05 : 100000.00;

        double techoEjeYSemanal = maxSemanal + margenSemanal;

        if (!valoresSemanalesY.isEmpty()) {
            planoSemanal.setDatos(valoresSemanalesY, etiquetasSemanalesX, techoEjeYSemanal);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}