/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.db.testutil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.IOUtils;

import static freemarker.template.Configuration.VERSION_2_3_0;

public class TestTemplateUtil {
    private static final TestTemplateUtil INSTANCE = new TestTemplateUtil();

    private final Configuration templateConfig;

    public static TestTemplateUtil getInstance() {
        return INSTANCE;
    }

    private TestTemplateUtil() {
        this.templateConfig = new Configuration(VERSION_2_3_0);

        // Where load the templates from:
        templateConfig.setClassForTemplateLoading(TestTemplateUtil.class, "/");

        // Some other recommended settings:
        templateConfig.setDefaultEncoding("UTF-8");
        templateConfig.setLocale(Locale.US);
        templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public void writeTemplate(String templatePath, Map<String, Object> params, File outputFile) {
        try (Writer writer = new FileWriter(outputFile)){
            writeTemplate(templatePath, params, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTemplate(String templatePath, Map<String, Object> params, Writer writer) {
        try {
            Template template = templateConfig.getTemplate(templatePath);

            template.process(params, writer);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
