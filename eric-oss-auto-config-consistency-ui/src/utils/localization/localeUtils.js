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
 * Updates a locale string that is defined using the following syntax e.g.
 * "Node set '%node_set_name%' was created." Once the substitutions map contains a "%node_set_name%"
 * key it will replace it with it's corresponding value i.e.
 *
 * const substitutions = new Map();
 * substitutions.set('%node_set_name%', 'athlone_01-123');
 *
 * In this case the result will be: "Node set 'athlone_01-123' was created."
 *
 * @param localeString The locale string to use.
 * @param substitutions The substitutions to use.
 * @returns An updated locale string.
 */
const updateLocaleString = (localeString, substitutions) => {
  let updatedString = localeString;
  substitutions.forEach((value, key) => {
    updatedString = updatedString.replaceAll(key, value);
  });
  return updatedString;
};

export { updateLocaleString };
