/*
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
 */

/**
 * Unit tests for objectMapper
 */
import { expect } from '@open-wc/testing';

import { formatDate } from '../../../src/utils/objectMapper.js';

describe('Given: An ObjectMapper utility', () => {
  describe('When: formatDate is called with an empty string', () => {
    it('Then: An empty string is returned', async () => {
      expect(formatDate('')).to.be.empty;
    });
  });
});
