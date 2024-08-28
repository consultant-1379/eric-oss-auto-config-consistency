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

import { expect, fixture, html } from '@open-wc/testing';

import SettingsErrorTable from '../../../../src/components/settings-error-table/settings-error-table.js';

import {
  RULESET_ERRORS_LINE_NUMBER,
  RULESET_ERRORS_ERROR_DETAILS,
} from '../../../../src/utils/attributes/rulesetAttributes.js';

import {
  getHeaderText,
  getColumnText,
} from '../../../resources/elementUtils.js';

const INVALID_CONDITION = 'Invalid Condition.';

const COLUMN_1_TITLE = 'Line';
const COLUMN_2_TITLE = 'Reason';

const COLUMNS = [
  { title: COLUMN_1_TITLE, attribute: RULESET_ERRORS_LINE_NUMBER },
  { title: COLUMN_2_TITLE, attribute: RULESET_ERRORS_ERROR_DETAILS },
];

const DATA = [
  {
    lineNumber: 2,
    errorType: `${INVALID_CONDITION}`,
    errorDetails:
      'Attribute(s) in Conditions not found in Managed Object model.',
    additionalInfo: '',
  },
  {
    lineNumber: 3,
    errorType: `${INVALID_CONDITION}`,
    errorDetails: 'Condition has invalid syntax.',
    additionalInfo: '',
  },
  {
    lineNumber: 4,
    errorType: `${INVALID_CONDITION}`,
    errorDetails:
      "Attributes in Conditions must be from the moType field (EUtranCellFDD)'s attributes",
    additionalInfo: '',
  },
];

describe('Given: An <e-settings-error-table>', () => {
  before(() => {
    SettingsErrorTable.register();
  });

  describe('When: The table is rendered', () => {
    it('Then: The errorDetails column contains a tooltip', async () => {
      const component = await fixture(
        html`<e-settings-error-table
          .columns="${COLUMNS}"
          .data="${DATA}"
        ></e-settings-error-table>`,
      );

      // Verify default table attributes
      expect(component.hasAttribute('dashed')).to.be.true;
      expect(component.hasAttribute('tiny')).to.be.true;
      expect(component.hasAttribute('sortable')).to.be.true;

      const tableHeaders = component.shadowRoot
        .querySelector('thead')
        .querySelector('tr:not(.filters)')
        .querySelectorAll('th');

      expect(getHeaderText(tableHeaders[0])).to.eq(COLUMN_1_TITLE);
      expect(getHeaderText(tableHeaders[1])).to.eq(COLUMN_2_TITLE);

      const tableData = component.shadowRoot
        .querySelector('tbody')
        .querySelectorAll('tr');
      expect(tableData.length).to.equal(DATA.length);

      DATA.forEach((entry, index) => {
        const cols = tableData[index].querySelectorAll('td');
        expect(getColumnText(cols[0])).to.contain(entry.lineNumber);

        const tooltip = cols[1].querySelector('eui-tooltip');
        expect(tooltip.getAttribute('message')).to.contain(entry.errorDetails);
        expect(tooltip.hasAttribute('smart')).to.be.true;
        expect(tooltip.getAttribute('position')).to.equal('top');
      });
    });
  });
});
