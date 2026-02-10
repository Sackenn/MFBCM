package org.example.util;

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

    // Dostepne jezyki - dodaj nowe tutaj
    private static final Map<String, Locale> AVAILABLE_LANGUAGES = new LinkedHashMap<>();

    static {
        AVAILABLE_LANGUAGES.put("en", Locale.ENGLISH);
        AVAILABLE_LANGUAGES.put("pl", Locale.forLanguageTag("pl"));
        loadBundle();
    }

    /**
     * Interfejs dla obiektow nasluchujacych zmiany jezyka.
     */
    public interface LanguageChangeListener {
        void onLanguageChanged(Locale newLocale);
    }

    /**
     * Laduje pakiet zasobow dla aktualnego jezyka.
     */
    private static void loadBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Nie znaleziono pakietu zasobow dla: " + currentLocale + ", uzywam domyslnego");
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
        }
    }

    /**
     * Pobiera przetlumaczony tekst dla podanego klucza.
     */
    public static String get(String key) {
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Brak tlumaczenia dla klucza: " + key);
            return "!" + key + "!";
        }
    }

    /**
     * Pobiera przetlumaczony tekst z parametrami.
     * Uzywa MessageFormat do podstawiania wartosci.
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
            return pattern;
        }
    }

    /**
     * Zmienia aktualny jezyk aplikacji.
     */
    public static void setLanguage(String languageCode) {
        Locale newLocale = AVAILABLE_LANGUAGES.get(languageCode);
        if (newLocale != null && !newLocale.equals(currentLocale)) {
            currentLocale = newLocale;
            loadBundle();
            notifyListeners();
        }
    }

    /**
     * Zmienia aktualny jezyk aplikacji na podstawie Locale.
     */
    public static void setLocale(Locale locale) {
        if (locale != null && !locale.equals(currentLocale)) {
            currentLocale = locale;
            loadBundle();
            notifyListeners();
        }
    }

    /**
     * Zwraca aktualny jezyk.
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Zwraca kod aktualnego jezyka.
     */
    public static String getCurrentLanguageCode() {
        return currentLocale.getLanguage();
    }

    /**
     * Zwraca mape dostepnych jezykow (kod -> Locale).
     */
    public static Map<String, Locale> getAvailableLanguages() {
        return Collections.unmodifiableMap(AVAILABLE_LANGUAGES);
    }

    /**
     * Zwraca liste kodow dostepnych jezykow.
     */
    public static List<String> getAvailableLanguageCodes() {
        return new ArrayList<>(AVAILABLE_LANGUAGES.keySet());
    }

    /**
     * Zwraca nazwe jezyka w jego wlasnym jezyku.
     */
    public static String getLanguageDisplayName(String languageCode) {
        return get("language." + languageCode);
    }

    /**
     * Dodaje sluchacza zmiany jezyka.
     */
    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Usuwa sluchacza zmiany jezyka.
     */
    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Powiadamia wszystkich sluchaczy o zmianie jezyka.
     */
    private static void notifyListeners() {
        for (LanguageChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged(currentLocale);
            } catch (Exception e) {
                System.err.println("Blad podczas powiadamiania sluchacza o zmianie jezyka: " + e.getMessage());
            }
        }
    }

    /**
     * Sprawdza czy dany jezyk jest dostepny.
     */
    public static boolean isLanguageAvailable(String languageCode) {
        return AVAILABLE_LANGUAGES.containsKey(languageCode);
    }

    /**
     * Dodaje nowy jezyk do listy dostepnych.
     * Uzyj tej metody aby dynamicznie dodac jezyk w runtime.
     */
    public static void registerLanguage(String languageCode, Locale locale) {
        AVAILABLE_LANGUAGES.put(languageCode, locale);
    }
}
