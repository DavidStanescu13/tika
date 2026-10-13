// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 3 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.apache.tika.utils.ProcessUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtils_escapeCommandLine_2_2_Test {

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = ProcessUtils.escapeCommandLine(null);
        assertEquals(null, result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces() {
        String result = ProcessUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() {

        assumeTrue(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_Windows() {

        assumeTrue(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_Windows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_NoWindows() {

        assumeFalse(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("test arg", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_NoWindows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }
}
