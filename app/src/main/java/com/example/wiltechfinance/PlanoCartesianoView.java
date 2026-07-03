package com.example.wiltechfinance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PlanoCartesianoView extends View {

    private Paint paintLinea, paintPunto, paintTexto, paintCuadricula, paintTextoValores;
    private List<Double> valoresY = new ArrayList<>();
    private List<String> etiquetasX = new ArrayList<>();

    // Rango útil interno para mapear las coordenadas en píxeles
    private double valorMinimoY = 0;
    private double valorMaximoY = 100000;

    public PlanoCartesianoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inicializarPinceles();
    }

    private void inicializarPinceles() {
        paintLinea = new Paint();
        paintLinea.setColor(Color.parseColor("#E50914")); // Rojo Estructura WilTech
        paintLinea.setStrokeWidth(6f);
        paintLinea.setAntiAlias(true);
        paintLinea.setStyle(Paint.Style.STROKE);

        paintPunto = new Paint();
        paintPunto.setColor(Color.WHITE); // Nodos de control blancos en los picos
        paintPunto.setAntiAlias(true);
        paintPunto.setStyle(Paint.Style.FILL);

        paintTexto = new Paint();
        paintTexto.setColor(Color.parseColor("#8C8C8C"));
        paintTexto.setTextSize(24f);
        paintTexto.setAntiAlias(true);

        // Pincel exclusivo para los montos de la izquierda (Eje Y)
        paintTextoValores = new Paint();
        paintTextoValores.setColor(Color.parseColor("#A0A0A0"));
        paintTextoValores.setTextSize(22f);
        paintTextoValores.setAntiAlias(true);

        paintCuadricula = new Paint();
        paintCuadricula.setColor(Color.parseColor("#222222")); // Rejilla muy tenue de fondo
        paintCuadricula.setStrokeWidth(2f);
    }

    public void setDatos(List<Double> valores, List<String> etiquetas, double maximoY) {
        this.valoresY = valores;
        this.etiquetasX = etiquetas;

        if (valores != null && !valores.isEmpty()) {
            double min = valores.get(0);
            double max = valores.get(0);
            for (double v : valores) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
            // Holgura en la parte inferior y superior para que los picos no toquen los bordes físicos del control
            this.valorMinimoY = min >= 150000 ? min - 150000 : 0;
            this.valorMaximoY = max + 150000;
        } else {
            this.valorMinimoY = 0;
            this.valorMaximoY = maximoY;
        }
        invalidate(); // Redibuja la interfaz en tiempo real de forma inmediata
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (valoresY == null || valoresY.isEmpty()) return;

        int ancho = getWidth();
        int alto = getHeight();

        // 🔥 MARGEN IZQUIERDO REDISEÑADO: 180 píxeles para albergar el formato de moneda sin pisarse
        int margenIzquierdo = 180;
        int margenInferior = 60;
        int margenSuperior = 50;

        int areaDibujoAncho = ancho - margenIzquierdo - 40;
        int areaDibujoAlto = alto - margenInferior - margenSuperior;

        // 1. PRIMER PASO: Calcular posiciones físicas (X, Y) de cada transacción
        float pasoX = areaDibujoAncho / (float) (valoresY.size() == 1 ? 1 : valoresY.size() - 1);
        float[] coordenadasX = new float[valoresY.size()];
        float[] coordenadasY = new float[valoresY.size()];

        for (int i = 0; i < valoresY.size(); i++) {
            coordenadasX[i] = margenIzquierdo + (i * pasoX);

            double saldoActual = valoresY.get(i);
            double proporcionY = (saldoActual - valorMinimoY) / (valorMaximoY - valorMinimoY);
            if (proporcionY > 1.0) proporcionY = 1.0;
            if (proporcionY < 0.0) proporcionY = 0.0;

            coordenadasY[i] = (float) ((margenSuperior + areaDibujoAlto) - (areaDibujoAlto * proporcionY));
        }

        // 2. SEGUNDO PASO: Dibujar la cuadrícula horizontal y las etiquetas exactas de tus millones a la izquierda
        List<String> marcasDibujadasEjeY = new ArrayList<>();

        for (int i = 0; i < valoresY.size(); i++) {
            double saldoReal = valoresY.get(i);
            float posicionYFisica = coordenadasY[i];

            // Formateador limpio en millones para el eje Y (Ej: 1.50M, 2.40M, 2.05M)
            String textoMarca = String.format(java.util.Locale.US, "$%.2fM", saldoReal / 1000000.0);

            // Control de solapamiento: No redibuja la línea si dos transacciones alcanzaron exactamente el mismo saldo
            if (!marcasDibujadasEjeY.contains(textoMarca)) {
                marcasDibujadasEjeY.add(textoMarca);

                // Dibuja la línea de cuadrícula horizontal alineada exactamente al punto
                canvas.drawLine(margenIzquierdo, posicionYFisica, ancho - 40, posicionYFisica, paintCuadricula);

                // Pinta la cifra real de dinero a la izquierda del eje vertical
                canvas.drawText(textoMarca, 15, posicionYFisica + 8, paintTextoValores);
            }
        }

        // Línea vertical base del eje Y
        canvas.drawLine(margenIzquierdo, margenSuperior, margenIzquierdo, alto - margenInferior, paintTexto);

        // 3. TERCER PASO: Trazar la curva de balance (Líneas rojas) y colocar los nodos blancos
        for (int i = 0; i < valoresY.size(); i++) {
            float x = coordenadasX[i];
            float y = coordenadasY[i];

            // Une los puntos con la línea roja de WilTech
            if (i > 0) {
                canvas.drawLine(coordenadasX[i - 1], coordenadasY[i - 1], x, y, paintLinea);
            }

            // Dibuja el círculo en el pico exacto
            canvas.drawCircle(x, y, 10f, paintPunto);

            // Pinta las horas correspondientes en el eje X
            if (i < etiquetasX.size()) {
                float ajusteX = (i == valoresY.size() - 1) ? x - 75 : x - 35; // Corrección para que la última hora no se salga del borde
                canvas.drawText(etiquetasX.get(i), ajusteX, alto - 15, paintTexto);
            }
        }
    }
}