// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 7 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import org.apache.tika.utils.CharsetUtils;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static java.util.Locale.ENGLISH;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharsetUtils_clean_2_4_Test {

    private CharsetUtils charsetUtils;

    private Method forNameMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        charsetUtils = new CharsetUtils();
        forNameMethod = CharsetUtils.class.getDeclaredMethod("forName", String.class);
        forNameMethod.setAccessible(true);
    }

    @Test
    public void testCleanWithValidCharset() throws Exception {
        String charsetName = "UTF-8";
        String result = charsetUtils.clean(charsetName);
        assertEquals(charsetName, result);
    }

    @Test
    public void testCleanWithInvalidCharset() throws Exception {
        String charsetName = "INVALID";
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithWhitespace() throws Exception {
        String charsetName = "  UTF-8  ";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Test
    public void testCleanWithTrailingComma() throws Exception {
        String charsetName = "UTF-8,";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Disabled("IFT3913 : oracle faux : une virgule en tete n'est pas acceptee par CHARSET_NAME_PATTERN -> clean renvoie null")

    @Test
    public void testCleanWithLeadingComma() throws Exception {
        String charsetName = ",UTF-8";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Disabled("IFT3913 : oracle faux : 'iso 8859-1' contient un espace, la regex ne garde que 'iso' -> clean renvoie null")

    @Test
    public void testCleanWithISO8859Pattern() throws Exception {
        String charsetName = "iso 8859-1";
        String result = charsetUtils.clean(charsetName);
        assertEquals("ISO-8859-1", result);
    }

    @Disabled("IFT3913 : oracle faux : attend un nom non canonique (CP1252 / WINDOWS-1252) alors que forName renvoie le nom canonique JDK windows-1252")

    @Test
    public void testCleanWithCPNamePattern() throws Exception {
        String charsetName = "cp-1252";
        String result = charsetUtils.clean(charsetName);
        assertEquals("CP1252", result);
    }

    @Disabled("IFT3913 : oracle faux : attend un nom non canonique (CP1252 / WINDOWS-1252) alors que forName renvoie le nom canonique JDK windows-1252")

    @Test
    public void testCleanWithWinNamePattern() throws Exception {
        String charsetName = "win-1252";
        String result = charsetUtils.clean(charsetName);
        assertEquals("WINDOWS-1252", result);
    }

    @Test
    public void testCleanWithIgnoreCase() throws Exception {
        String charsetName = "utf-8";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Test
    public void testCleanWithNone() throws Exception {
        String charsetName = "none";
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithNo() throws Exception {
        String charsetName = "no";
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithNull() throws Exception {
        String charsetName = null;
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithEmptyString() throws Exception {
        String charsetName = "";
        String result = charsetUtils.clean(charsetName);
        assertNull(result);
    }

    @Test
    public void testCleanWithWhitespaceAndComma() throws Exception {
        String charsetName = "  UTF-8,  ";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Test
    public void testCleanWithTrailingCommaAndWhitespace() throws Exception {
        String charsetName = "UTF-8,  ";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Disabled("IFT3913 : oracle faux : une virgule en tete n'est pas acceptee par CHARSET_NAME_PATTERN -> clean renvoie null")

    @Test
    public void testCleanWithLeadingCommaAndWhitespace() throws Exception {
        String charsetName = ",  UTF-8";
        String result = charsetUtils.clean(charsetName);
        assertEquals("UTF-8", result);
    }

    @Disabled("IFT3913 : oracle faux : 'iso 8859-1' contient un espace, la regex ne garde que 'iso' -> clean renvoie null")

    @Test
    public void testCleanWithISO8859PatternAndWhitespace() throws Exception {
        String charsetName = "  iso 8859-1  ";
        String result = charsetUtils.clean(charsetName);
        assertEquals("ISO-8859-1", result);
    }

    @Disabled("IFT3913 : oracle faux : attend un nom non canonique (CP1252 / WINDOWS-1252) alors que forName renvoie le nom canonique JDK windows-1252")

    @Test
    public void testCleanWithCPNamePatternAndWhitespace() throws Exception {
        String charsetName = "  cp-1252  ";
        String result = charsetUtils.clean(charsetName);
        assertEquals("CP1252", result);
    }

    @Test
    public void testCleanWithWinNamePatternAndWhitespace() throws Exception {
    }
}
