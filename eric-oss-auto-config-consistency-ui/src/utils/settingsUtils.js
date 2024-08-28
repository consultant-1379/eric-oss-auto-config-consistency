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

import { logWarning } from './logUtils.js';

/**
 * Downloads a file using the browser
 *
 * @param url The URL of where the file is stored
 * @param filename The name of the file once it is saved locally
 */
const download = async (url, filename) => {
  const data = await fetch(url)
    .then(response => {
      if (!response.ok) {
        logWarning(
          `Download failed, reason: status code: ${response.status}, status text: ${response.statusText}`,
        );
        return null;
      }
      return response.blob();
    })
    .catch(() => false);

  if (data) {
    const objectUrl = window.URL.createObjectURL(data);
    const link = document.createElement('a');
    link.setAttribute('href', objectUrl);
    link.setAttribute('download', filename);
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    return true;
  }

  return false;
};

export { download };
