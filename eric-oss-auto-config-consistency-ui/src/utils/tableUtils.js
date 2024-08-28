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

const NOT_FOUND = -1;

/**
 * Compares two objects based on the key provided
 *
 * @param objA The first object.
 * @param objB The second object.
 * @param key The key used to compare the two objects.
 * @returns true if they are equal, otherwise false.
 */
const sameObject = (objA, objB, key) => objA[key] === objB[key];

/**
 * Finds new entries in the updated data compared against the current data.
 *
 * @param currentData The current data.
 * @param updatedData The updated data.
 * @param filterColumn The filter to compare items.
 * @returns An array of new items.
 */
const findNewEntries = (currentData, updatedData, filterColumn) =>
  updatedData.filter(updatedObj => {
    const matchingIndex = currentData.findIndex(currentObj =>
      sameObject(currentObj, updatedObj, filterColumn),
    );
    return matchingIndex === NOT_FOUND ? updatedObj : undefined;
  });

/**
 * Finds deleted entries in the updated data compared against the current data.
 *
 * @param currentData The current data.
 * @param updatedData The updated data.
 * @param filterColumn The filter to compare items.
 * @returns An array of new items.
 */
const findDeletedEntries = (currentData, updatedData, filterColumn) =>
  currentData.filter(updatedObj => {
    const matchingIndex = updatedData.findIndex(currentObj =>
      sameObject(currentObj, updatedObj, filterColumn),
    );
    return matchingIndex === NOT_FOUND ? updatedObj : undefined;
  });

/**
 * Gets the current elements, sorts them using the attribute name and places the created name at the beginning of the array.
 *
 * @param elements The current elements.
 * @param createdName The newly created element.
 * @param attributeName The attribute used to compare against created name.
 * @returns An array of sorted elements with newly created coming first.
 */
const sortByNewest = (attributeName, createdName, elements) => {
  elements.sort((elementA, elementB) => {
    if (elementA[attributeName] === createdName) {
      return -1;
    }
    return elementB[attributeName] === createdName ? 1 : 0;
  });
};

/**
 * Removes the deleted entry from the table data based on a key
 *
 * @function removeDeletedEntry
 * @private
 * @param originalData The current data
 * @param entryToRemove The entry that was deleted from the server
 * @param filterColumn The key to used to remove data
 */
const removeDeletedEntry = (originalData, entryToRemove, filterColumn) => {
  const filteredData = originalData.filter(
    entry => !sameObject(entry, entryToRemove, filterColumn),
  );
  return filteredData;
};

export {
  findNewEntries,
  findDeletedEntries,
  sameObject,
  sortByNewest,
  removeDeletedEntry,
};
