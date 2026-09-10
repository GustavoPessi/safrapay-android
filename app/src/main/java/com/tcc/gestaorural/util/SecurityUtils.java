package com.tcc.gestaorural.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash de senha para o TCC (SHA-256 + salt fixo simples).
 * OBS: para produção, trocar por bcrypt/scrypt/argon2 com salt por usuário.
 */
public final class SecurityUtils {
    private SecurityUtils() {}

    private static final String SALT = "gestao_rural_tcc_v1::";

    public static String hashSenha(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((SALT + senha).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Falha ao gerar hash", e);
        }
    }

    public static boolean conferir(String senha, String hashArmazenado) {
        return hashSenha(senha).equals(hashArmazenado);
    }
}
