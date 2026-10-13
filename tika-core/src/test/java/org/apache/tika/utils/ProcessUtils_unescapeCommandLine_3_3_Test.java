// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 1 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

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
public class ProcessUtils_unescapeCommandLine_3_3_Test {

    @Disabled("IFT3913 : oracle faux : suppose que unescapeCommandLine retire les guillemets sur toute plateforme et meme sans espace (le code n'agit que sous Windows ET en presence d'un espace)")

    @Test
    public void testUnescapeCommandLine() {
        // Test case 1: Argument does not contain spaces and is not a Windows quoted string
        String arg1 = "example";
        String expected1 = "example";
        String result1 = ProcessUtils.unescapeCommandLine(arg1);
        assertEquals(expected1, result1);
        // Test case 2: Argument contains spaces and is a Windows quoted string
        String arg2 = "\"example arg\"";
        String expected2 = "example arg";
        String result2 = ProcessUtils.unescapeCommandLine(arg2);
        assertEquals(expected2, result2);
        // Test case 3: Argument does not contain spaces and is a Windows quoted string
        String arg3 = "\"example\"";
        String expected3 = "example";
        String result3 = ProcessUtils.unescapeCommandLine(arg3);
        assertEquals(expected3, result3);
    }
}
