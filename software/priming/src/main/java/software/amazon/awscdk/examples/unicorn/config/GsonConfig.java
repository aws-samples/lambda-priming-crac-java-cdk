/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package software.amazon.awscdk.examples.unicorn.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    @Bean
    public GsonHttpMessageConverter gsonHttpMessageConverter() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes field) {
                        try {
                            Field javaField = field.getDeclaringClass().getDeclaredField(field.getName());
                            javaField.setAccessible(true);
                            Object value = javaField.get(null);

                            // Exclude fields based on the desired JsonInclude behavior
                            if (value == null) {
                                return true; // Exclude null values
                            } else if (value instanceof String && ((String) value).isEmpty()) {
                                return true; // Exclude empty strings
                            } else // Include the field
                            if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                                return true; // Exclude empty collections
                            } else
                                return value instanceof Map && ((Map<?, ?>) value).isEmpty(); // Exclude empty maps
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            return false;
                        }
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .serializeNulls()
                // Customize Gson configurations here if needed
                .create();

        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(gson);
        return converter;
    }

}