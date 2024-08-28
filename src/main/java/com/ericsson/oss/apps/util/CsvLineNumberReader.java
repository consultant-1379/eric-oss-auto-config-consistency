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

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang3.ArrayUtils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Utility class to read the line number of a rules csv file.
 */
public class CsvLineNumberReader extends CSVReader {

    public CsvLineNumberReader(final Reader reader) {
        super(reader);
    }

    @Override
    public String[] readNext() throws IOException, CsvValidationException {
        final String[] nextLine = super.readNext();
        return nextLine == null ? null : ArrayUtils.add(nextLine, String.valueOf(linesRead));
    }

    @Override
    public String[] readNextSilently() throws IOException {
        final String[] nextLine = super.readNextSilently();
        return nextLine == null ? null : ArrayUtils.add(nextLine, "LINE_NUMBER");
    }
}
