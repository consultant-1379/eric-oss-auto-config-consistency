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

import {
  HAVE_ATTRIBUTE,
  CONTAIN,
  UI_RENDERING_PERIOD,
  PAGE_CHANGE_PERIOD,
} from '../utils/testUtils.js';
import { APP_ID, APP_DESCRIPTION, BASE_EACC_URL } from '../utils/testData.js';

/**
 * UC_00: It shall be possible to log into the GAS Portal and sucessfully authenticate.
 */
describe('EACC-UC_00: GAS portal integration', () => {
  before(() => {
    cy.visit('/#launcher?productName=rapps:product');
    cy.wait(UI_RENDERING_PERIOD);
  });
  it(`The GAS portal should contain a '${APP_ID}' card that when clicked leads to the '${BASE_EACC_URL}' landing page.`, () => {
    cy.getEaccAppCard()
      .should(HAVE_ATTRIBUTE, 'display-name', APP_ID)
      .and(HAVE_ATTRIBUTE, 'description-long', APP_DESCRIPTION);

    cy.getEaccAppCard().click();
    cy.wait(PAGE_CHANGE_PERIOD);

    cy.url().should(CONTAIN, BASE_EACC_URL);

    cy.get('eui-breadcrumb', { includeShadowDom: true })
      .shadow()
      .find('#breadcrumb')
      .should(CONTAIN, APP_ID);
  });
});
