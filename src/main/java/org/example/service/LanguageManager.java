package org.example.service;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Menedżer języków aplikacji - obsługuje internacjonalizację (i18n).
 * Pozwala na łatwe dodawanie nowych języków poprzez pliki properties.
 * Polskie znaki są kodowane jako sekwencje Unicode (np. \\u0105 dla ą).
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
        updateSwingButtonTexts();
    }

    public interface LanguageChangeListener {
        void onLanguageChanged(Locale newLocale);
    }

    /**
     * Ładuje plik tłumaczeń.
     */
    private static void loadBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Nie znaleziono pakietu zasobów dla: " + currentLocale + ", używam domyślnego");
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
            Locale.setDefault(newLocale);
            javax.swing.JComponent.setDefaultLocale(newLocale);
            javax.swing.UIManager.getDefaults().setDefaultLocale(newLocale);
            loadBundle();
            updateSwingButtonTexts();
            notifyListeners();
        }
    }

    /**
     * Aktualizuje teksty przycisków Swing (Yes, No, OK, Cancel, FileChooser) na przetłumaczone.
     */
    private static void updateSwingButtonTexts() {
        // OptionPane
        javax.swing.UIManager.put("OptionPane.yesButtonText", get("button.yes"));
        javax.swing.UIManager.put("OptionPane.noButtonText", get("button.no"));
        javax.swing.UIManager.put("OptionPane.okButtonText", get("button.ok"));
        javax.swing.UIManager.put("OptionPane.cancelButtonText", get("button.cancel"));

        // FileChooser
        javax.swing.UIManager.put("FileChooser.openButtonText", get("filechooser.open"));
        javax.swing.UIManager.put("FileChooser.cancelButtonText", get("button.cancel"));
        javax.swing.UIManager.put("FileChooser.saveButtonText", get("filechooser.save"));
        javax.swing.UIManager.put("FileChooser.openDialogTitleText", get("filechooser.openTitle"));
        javax.swing.UIManager.put("FileChooser.saveDialogTitleText", get("filechooser.saveTitle"));
        javax.swing.UIManager.put("FileChooser.lookInLabelText", get("filechooser.lookIn"));
        javax.swing.UIManager.put("FileChooser.saveInLabelText", get("filechooser.saveIn"));
        javax.swing.UIManager.put("FileChooser.fileNameLabelText", get("filechooser.fileName"));
        javax.swing.UIManager.put("FileChooser.filesOfTypeLabelText", get("filechooser.filesOfType"));
        javax.swing.UIManager.put("FileChooser.folderNameLabelText", get("filechooser.folderName"));
        javax.swing.UIManager.put("FileChooser.upFolderToolTipText", get("filechooser.upFolder"));
        javax.swing.UIManager.put("FileChooser.homeFolderToolTipText", get("filechooser.homeFolder"));
        javax.swing.UIManager.put("FileChooser.newFolderToolTipText", get("filechooser.newFolder"));
        javax.swing.UIManager.put("FileChooser.listViewButtonToolTipText", get("filechooser.listView"));
        javax.swing.UIManager.put("FileChooser.detailsViewButtonToolTipText", get("filechooser.detailsView"));
        javax.swing.UIManager.put("FileChooser.acceptAllFileFilterText", get("filechooser.allFiles"));
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

