// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 3 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_unescapeCommandLine_3_2_Test {

    @Mock
    private SystemUtils systemUtils;

    @Disabled("IFT3913 : Mockito : tentative de stubber le champ static final SystemUtils.IS_OS_WINDOWS via un mock -> MissingMethodInvocationException / WrongTypeOfReturnValue")

    @Test
    public void testUnescapeCommandLine() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(true);
        String input = "\"example arg\"";
        String expectedOutput = "example arg";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }

    @Disabled("IFT3913 : Mockito : tentative de stubber le champ static final SystemUtils.IS_OS_WINDOWS via un mock -> MissingMethodInvocationException / WrongTypeOfReturnValue")

    @Test
    public void testUnescapeCommandLineNoQuotes() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(true);
        String input = "example arg";
        String expectedOutput = "example arg";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }

    @Disabled("IFT3913 : Mockito : tentative de stubber le champ static final SystemUtils.IS_OS_WINDOWS via un mock -> MissingMethodInvocationException / WrongTypeOfReturnValue")

    @Test
    public void testUnescapeCommandLineNonWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(false);
        String input = "\"example arg\"";
        String expectedOutput = "\"example arg\"";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }
}
