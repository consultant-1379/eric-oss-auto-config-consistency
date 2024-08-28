/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_FILE_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.TEST_SETUP_ERROR;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Utilities {

    public static String readFileFromResources(final String fileName) throws IOException {
        String result = "";
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (final InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found in resources: " + fileName);
            }
            result = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));
        }
        return result;
    }

    public static ResponseEntity<String> executeHttpGet(final RestTemplate restTemplate, final URI url, final HttpHeaders headers) {
        final HttpEntity<String> request = new HttpEntity<>(null, headers);
        return restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    }

    public static ResponseEntity<String> executeHttpPost(final RestTemplate restTemplate, final URI url, final HttpHeaders headers,
            final String body) {
        final HttpEntity<String> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public static ResponseEntity<String> executeHttpPatch(final RestTemplate restTemplate, final URI url, final HttpHeaders headers,
            final String body) {
        final HttpEntity<String> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
    }

    public static byte[] toJson(final Object object) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    public static MockMultipartFile createMultipartFile(final String filepath) {
        final AtomicReference<MockMultipartFile> multipartFile = new AtomicReference<>();
        assertThatCode(() -> {
            try (final InputStream inputStream = Files.newInputStream(Path.of(filepath), StandardOpenOption.READ)) {
                multipartFile.set(new MockMultipartFile(DEFAULT_FILE_NAME, inputStream));
            }
        }).as(TEST_SETUP_ERROR + ", unable to read test data file").doesNotThrowAnyException();

        return multipartFile.get();
    }
}
