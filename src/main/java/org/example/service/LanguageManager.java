package org.example.service;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Menedzer jezykow aplikacji - obsluguje internacjonalizacje (i18n).
 * Pozwala na latwe dodawanie nowych jezykow poprzez pliki properties.
 */
public class LanguageManager {

    private static final String BUNDLE_BASE_NAME = "messages";
    private static final List<LanguageChangeListener> listeners = new CopyOnWriteArrayList<>();

    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle currentBundle;

    private static final Map<String, Locale> AVAILABLE_LANGUAGES = new LinkedHashMap<>();

    static {
        AVAILABLE_LANGUAGES.put("en", Locale.ENGLISH);
        AVAILABLE_LANGUAGES.put("pl", Locale.forLanguageTag("pl"));
        loadBundle();
    }

    public interface LanguageChangeListener {
        void onLanguageChanged(Locale newLocale);
    }

    private static void loadBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Nie znaleziono pakietu zasobow dla: " + currentLocale + ", uzywam domyslnego");
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
        }
    }

    public static String get(String key) {
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Brak tlumaczenia dla klucza: " + key);
            return "!" + key + "!";
        }
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
            return pattern;
        }
    }

    public static void setLanguage(String languageCode) {
        Locale newLocale = AVAILABLE_LANGUAGES.get(languageCode);
        if (newLocale != null && !newLocale.equals(currentLocale)) {
            currentLocale = newLocale;
            loadBundle();
            notifyListeners();
        }
    }

    public static String getCurrentLanguageCode() {
        return currentLocale.getLanguage();
    }

    public static List<String> getAvailableLanguageCodes() {
        return new ArrayList<>(AVAILABLE_LANGUAGES.keySet());
    }

    public static String getLanguageDisplayName(String languageCode) {
        return get("language." + languageCode);
    }

    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (LanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(currentLocale);
            } catch (Exception e) {
                System.err.println("Blad podczas powiadamiania sluchacza o zmianie jezyka: " + e.getMessage());
            }
        }
    }
}

