/*
 * GPLv3 License
 *
 * Copyright (c) 2023-2024 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import com.formdev.flatlaf.FlatIntelliJLaf;

public class JarAnalyzerLaf extends FlatIntelliJLaf {
    public static boolean setup() {
        return setup(new JarAnalyzerLaf());
    }
}
