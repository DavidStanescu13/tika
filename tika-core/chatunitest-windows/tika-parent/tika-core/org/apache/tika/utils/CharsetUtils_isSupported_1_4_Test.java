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

public class CharsetUtils_isSupported_1_4_Test {

    private static Method isSupportedMethod;

    @BeforeAll
    public static void setUp() throws NoSuchMethodException {
        isSupportedMethod = CharsetUtils.class.getDeclaredMethod("isSupported", String.class);
        isSupportedMethod.setAccessible(true);
    }

    @Test
    public void testIsSupportedWithSupportedCharset() throws Exception {
        assertTrue((Boolean) isSupportedMethod.invoke(null, "UTF-8"));
    }

    @Test
    public void testIsSupportedWithUnsupportedCharset() throws Exception {
        assertFalse((Boolean) isSupportedMethod.invoke(null, "INVALID_CHARSET"));
    }
}
