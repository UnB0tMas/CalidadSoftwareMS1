package com.upsjb.ms1.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PasswordPolicyUtil {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 120;

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicyUtil() {
    }

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("La contraseña es obligatoria.");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("La contraseña debe tener al menos 8 caracteres.");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("La contraseña no puede superar 120 caracteres.");
        }

        if (!UPPERCASE.matcher(password).find()) {
            errors.add("La contraseña debe contener al menos una letra mayúscula.");
        }

        if (!LOWERCASE.matcher(password).find()) {
            errors.add("La contraseña debe contener al menos una letra minúscula.");
        }

        if (!DIGIT.matcher(password).find()) {
            errors.add("La contraseña debe contener al menos un número.");
        }

        if (!SPECIAL.matcher(password).find()) {
            errors.add("La contraseña debe contener al menos un carácter especial.");
        }

        if (hasRepeatedSequence(password)) {
            errors.add("La contraseña no debe contener secuencias repetitivas simples.");
        }

        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    private static boolean hasRepeatedSequence(String password) {
        return password.matches(".*(.)\\1{3,}.*");
    }
}