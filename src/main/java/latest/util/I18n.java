package latest.util;

import java.util.Locale;

/**
 * @author dmitry dementiev
 */
public interface I18n {
    public abstract String getText(String s);

    public abstract String getHtmlEncodedText(String s);

    public abstract String getText(String s, Locale locale);
}