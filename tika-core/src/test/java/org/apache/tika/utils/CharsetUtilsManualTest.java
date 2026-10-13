/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Test;

/**
 * IFT3913 - Tache 2 : tests ecrits a la main pour {@link CharsetUtils}.
 * Chaque test est documente dans le README a la racine du depot.
 */
public class CharsetUtilsManualTest {

    // ------------------------------------------------------------------
    // forName : exceptions (le test original ne passe que par clean(), qui les avale)
    // ------------------------------------------------------------------

    @Test
    public void testForNameNullThrowsIllegalArgument() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CharsetUtils.forName(null));
        assertFalse(e instanceof IllegalCharsetNameException, "null est rejete avant toute analyse du nom");
    }

    @Test
    public void testForNameUnparseableNameThrowsIllegalCharsetName() {
        // la regex exige au moins un caractere qui ne soit ni espace, ni guillemet, ni > , ;
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName(""));
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("   "));
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("\"\""));
    }

    @Test
    public void testForNameNoneAndNoAreRejected() {
        IllegalCharsetNameException e1 = assertThrows(IllegalCharsetNameException.class,
                () -> CharsetUtils.forName("none"));
        assertEquals("none", e1.getCharsetName());
        assertThrows(IllegalCharsetNameException.class, () -> CharsetUtils.forName("NO"));
        // mais "non" ou "nonexistent" ne sont pas des mots reserves : ils vont jusqu'a Charset.forName
        assertThrows(UnsupportedCharsetException.class, () -> CharsetUtils.forName("nonexistent"));
    }

    @Test
    public void testForNameUnknownButWellFormedThrowsUnsupported() {
        assertThrows(UnsupportedCharsetException.class, () -> CharsetUtils.forName("bogus-charset-123"));
        assertNull(CharsetUtils.clean("bogus-charset-123"));
    }

    // ------------------------------------------------------------------
    // forName : chemin rapide (table des charsets communs) et normalisation
    // ------------------------------------------------------------------

    @Test
    public void testForNameCommonCharsetIsCaseInsensitiveAndCached() {
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("utf-8"));
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("UTF-8"));
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("Utf8")); // alias JDK
        assertSame(StandardCharsets.ISO_8859_1, CharsetUtils.forName("latin1")); // alias JDK
        assertSame(StandardCharsets.ISO_8859_1, CharsetUtils.forName("ISO-8851-1")); // faute de frappe connue
    }

    @Test
    public void testForNameExtractsFirstTokenOfHttpStyleValue() {
        // guillemets et espaces de tete ignores ; tout ce qui suit un espace, ',' ';' ou '>' est ignore
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("  \"utf-8\"; q=0.5"));
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("utf-8,latin1"));
        assertSame(StandardCharsets.UTF_8, CharsetUtils.forName("\"\" utf-8"));
        assertEquals("UTF-16BE", CharsetUtils.clean("utf-16be>"));
    }

    @Test
    public void testForNameIsoPatternRequiresKnownPart() {
        // ".*8859-(\d+)" : n'importe quel prefixe est accepte, le numero decide
        assertEquals("ISO-8859-7", CharsetUtils.clean("x8859-7"));
        assertEquals("ISO-8859-2", CharsetUtils.clean("8859-2"));
        // partie inconnue dans la table commune -> on retombe sur Charset.forName, qui echoue
        assertNull(CharsetUtils.clean("8859-99"));
        assertThrows(UnsupportedCharsetException.class, () -> CharsetUtils.forName("8859-99"));
    }

    @Test
    public void testForNameCpAndWinPatterns() {
        // cp-NNNN -> cpNNNN (alias JDK de windows-NNNN) ; win-NNNN / winNNNN -> windows-NNNN
        assertEquals("windows-1252", CharsetUtils.clean("CP-1252"));
        assertEquals("windows-1253", CharsetUtils.clean("win1253"));
        assertEquals("windows-1255", CharsetUtils.clean("WIN-1255"));
        // numero inconnu : la table ne repond pas, Charset.forName non plus
        assertNull(CharsetUtils.clean("cp-9999"));
        assertNull(CharsetUtils.clean("win-9999"));
        // "cp1252" sans tiret ne correspond pas a CP_NAME_PATTERN mais est un alias JDK direct
        assertEquals("windows-1252", CharsetUtils.clean("cp1252"));
    }

    @Test
    public void testForNameFallsBackToJdkForNonCommonCharset() {
        // UTF-32 n'est pas dans la table COMMON_CHARSETS : on doit passer par Charset.forName
        Charset cs = CharsetUtils.forName("UTF-32");
        assertEquals("UTF-32", cs.name());
        assertEquals("UTF-32", CharsetUtils.clean("utf-32"));
    }

    // ------------------------------------------------------------------
    // isSupported
    // ------------------------------------------------------------------

    @Test
    public void testIsSupportedDelegatesToJdkWithoutIcu() {
        assertTrue(CharsetUtils.isSupported("ISO-8859-1"));
        assertTrue(CharsetUtils.isSupported("utf-32"));
        assertFalse(CharsetUtils.isSupported("not-a-charset-xyz"));
        // nom syntaxiquement illegal pour le JDK : IllegalCharsetNameException -> false, pas d'exception
        assertFalse(CharsetUtils.isSupported("utf 8"));
        assertFalse(CharsetUtils.isSupported("!!"));
    }
}
