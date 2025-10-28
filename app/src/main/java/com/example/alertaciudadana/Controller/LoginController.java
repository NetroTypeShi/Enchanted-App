package com.example.alertaciudadana.Controller;

public class LoginController {
    public static String cifrarCesar(String texto) {
        String alfabeto = "abcdefghijklmnopqrstuvwxyz";
        String resultado = "";
        texto = texto.toLowerCase();

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            int pos = alfabeto.indexOf(c);
            if (pos != -1) {
                char nuevo = alfabeto.charAt((pos + 1) % 26);
                resultado = resultado + nuevo;
            } else {
                resultado = resultado + c;
            }
        }
        return resultado;
    }
}