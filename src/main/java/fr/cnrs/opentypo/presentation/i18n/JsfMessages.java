package fr.cnrs.opentypo.presentation.i18n;

import jakarta.faces.context.FacesContext;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Résout les clés du bundle JSF déclaré dans faces-config (&lt;var&gt;msg&lt;/var&gt;, base i18n.messages).
 */
public final class JsfMessages {

    private static final String BUNDLE_VAR = "msg";

    private JsfMessages() {
    }

    public static String get(String key) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return key;
        }
        try {
            ResourceBundle bundle = ctx.getApplication().getResourceBundle(ctx, BUNDLE_VAR);
            return bundle.getString(key);
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String format(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }
}
