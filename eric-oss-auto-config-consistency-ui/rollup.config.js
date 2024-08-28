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

import { terser } from 'rollup-plugin-terser';
import minifyHTML from 'rollup-plugin-minify-html-literals';
import summary from 'rollup-plugin-summary';
import glob from 'glob';
import { fileURLToPath } from 'node:url';

const minifiedFiles = [];

// list of all modules that are being extended here
const externalModules = [];

// creates an array of all JS files to be minified
const jsFilesToMinify = glob.sync('build/**/*.js');

jsFilesToMinify.forEach(file => {
  if (file.includes('index.js')) {
    return;
  }
  externalModules.push(fileURLToPath(new URL(file, import.meta.url)));
  minifiedFiles.push({
    input: file,
    output: [
      {
        file,
        format: 'esm',
        name: `${file}`,
      },
    ],
    plugins: [
      // Minify HTML template literals
      minifyHTML(),
      // Minify JS
      terser({
        ecma: 2020,
        module: true,
        warnings: true,
      }),
      // Print bundle summary
      // Not necessary for our CLI but good for showcase to show file sizes
      summary(),
    ],
    external: externalModules,
    onwarn: warning => {
      // Skip This set to undefined warning
      // see warning message https://rollupjs.org/guide/en/#error-this-is-undefined
      // for more information on this
      if (warning.code === 'THIS_IS_UNDEFINED') {
        return;
      }

      // console.warn everything else
      // eslint-disable-next-line no-console
      console.warn(warning.message);
    },
  });
});

export default minifiedFiles;
