// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 3 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_escapeCommandLine_2_1_Test {

    @InjectMocks
    private ProcessUtils processUtils;

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = processUtils.escapeCommandLine(null);
        assertEquals(null, result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_Windows() {
        String result = processUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() {

        assumeTrue(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        String result = processUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_Windows() {
        String result = processUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotesAndSpaces_Windows() {

        assumeTrue(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        String result = processUtils.escapeCommandLine("\"test arg\" with spaces");
        assertEquals("\"\"test arg\" with spaces\"", result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_NotWindows() {
        System.setProperty("os.name", "Linux");
        String result = processUtils.escapeCommandLine("test");
        assertEquals("test", result);
        System.clearProperty("os.name");
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NotWindows() {

        assumeFalse(SystemUtils.IS_OS_WINDOWS); // IFT3913 : test conditionne a la plateforme apres echec en CI
        System.setProperty("os.name", "Linux");
        String result = processUtils.escapeCommandLine("test arg");
        assertEquals("test arg", result);
        System.clearProperty("os.name");
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_NotWindows() {
        System.setProperty("os.name", "Linux");
        String result = processUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
        System.clearProperty("os.name");
    }

    @Test
    public void testEscapeCommandLine_WithQuotesAndSpaces_NotWindows() {
        System.setProperty("os.name", "Linux");
        String result = processUtils.escapeCommandLine("\"test arg\" with spaces");
        assertEquals("\"test arg\" with spaces", result);
        System.clearProperty("os.name");
    }
}
