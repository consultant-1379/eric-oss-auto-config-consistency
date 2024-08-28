/*
 * COPYRIGHT Ericsson 2024
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
 * Unit tests for utilityCalls
 */
import { expect } from '@open-wc/testing';
import sinon from 'sinon';

import { ChangeStatus } from '../../../src/utils/attributes/changeAttributes.js';
import {
  changesInProgress,
  getNonRevertableChangesCount,
  getNonApplyableChangesCount,
} from '../../../src/utils/rest/utilityCalls.js';

import {
  MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY,
  MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
} from '../../resources/mockData.js';
import { MOCK_BAD_REQUEST_RESPONSE } from '../../resources/mockResponses.js';

const EXECUTION_ID = 1;
const EXECUTIONS_AUDITS_API = `/v1/executions/${EXECUTION_ID}/audit-results`;
const EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES = `${EXECUTIONS_AUDITS_API}?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.REVERSION_COMPLETE})&page=0&pageSize=1`;
const EXECUTIONS_AUDITS_API_FOR_NON_APPLYABLE_CHANGES = `${EXECUTIONS_AUDITS_API}?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS},${ChangeStatus.IMPLEMENTATION_COMPLETE})&page=0&pageSize=1`;
const EXECUTIONS_AUDITS_API_FOR_IN_PROGRESS_CHANGES = `${EXECUTIONS_AUDITS_API}?filter=changeStatus:(${ChangeStatus.IMPLEMENTATION_IN_PROGRESS},${ChangeStatus.REVERSION_IN_PROGRESS})&page=0&pageSize=1`;

const MOCK_POPULATED_AUDITS_RESPONSE = {
  json: MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_EMPTY_IN_PROGRESS_AUDITS_RESPONSE = {
  json: MOCK_AUDIT_RESULTS_ZERO_IN_PROGRESS_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

const MOCK_POPULATED_IN_PROGRESS_AUDITS_RESPONSE = {
  json: MOCK_AUDIT_RESULTS_FOR_CHANGES_DATA_ARRAY,
  ok: true,
  status: 200,
};

let fetchStub;

describe('Given: An utilityCalls object', () => {
  before(() => {
    fetchStub = sinon.stub(window, 'fetch');
  });

  after(() => {
    sinon.restore();
  });

  beforeEach(() => {
    fetchStub.callsFake(() => Promise.resolve(true));
  });

  afterEach(() => {
    fetchStub.reset();
  });

  describe('When: getNonRevertableChangesCount is called and JSON is returned', () => {
    it('Then: The number of non revertable changes is returned', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const totalCount = 5;
      const result = await getNonRevertableChangesCount(
        EXECUTION_ID,
        totalCount,
      );
      expect(result).to.equal(1);
    });
  });

  describe('When: getNonRevertableChangesCount is called and a backend an error occurs', () => {
    it('Then: The total count is returned by default', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_REVERTABLE_CHANGES))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      const totalCount = 5;
      const result = await getNonRevertableChangesCount(
        EXECUTION_ID,
        totalCount,
      );
      expect(result).to.equal(totalCount);
    });
  });

  describe('When: getNonApplyableChangesCount is called and JSON is returned', () => {
    it('Then: The number of non applyable changes is returned', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_APPLYABLE_CHANGES))
        .resolves(MOCK_POPULATED_AUDITS_RESPONSE);

      const totalCount = 5;
      const result = await getNonApplyableChangesCount(
        EXECUTION_ID,
        totalCount,
      );
      expect(result).to.equal(1);
    });
  });

  describe('When: getNonApplyableChangesCount is called is called and a backend an error occurs', () => {
    it('Then: Then: The total count is returned by default', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_NON_APPLYABLE_CHANGES))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      const totalCount = 5;
      const result = await getNonApplyableChangesCount(
        EXECUTION_ID,
        totalCount,
      );
      expect(result).to.equal(totalCount);
    });
  });

  describe('When: changesInProgress is called and JSON is returned with empty array', () => {
    it('Then: false is returned', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_IN_PROGRESS_CHANGES))
        .resolves(MOCK_POPULATED_EMPTY_IN_PROGRESS_AUDITS_RESPONSE);

      const result = await changesInProgress(EXECUTION_ID);
      expect(result).to.be.false;
    });
  });

  describe('When: changesInProgress is called and JSON is returned with a populated array', () => {
    it('Then: true is returned', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_IN_PROGRESS_CHANGES))
        .resolves(MOCK_POPULATED_IN_PROGRESS_AUDITS_RESPONSE);

      const result = await changesInProgress(EXECUTION_ID);
      expect(result).to.be.true;
    });
  });

  describe('When: changesInProgress is called and a backend an error occurs', () => {
    it('Then: true is returned', async () => {
      fetchStub
        .withArgs(sinon.match(EXECUTIONS_AUDITS_API_FOR_IN_PROGRESS_CHANGES))
        .resolves(MOCK_BAD_REQUEST_RESPONSE);

      const result = await changesInProgress(EXECUTION_ID);
      expect(result).to.be.true;
    });
  });
});
