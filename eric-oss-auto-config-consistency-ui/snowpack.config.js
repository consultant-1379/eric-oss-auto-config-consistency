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

const { nodeResolve } = require('@rollup/plugin-node-resolve');
const { devDependencies } = require('./package.json');
const externalModules = require('./public/config.package.json').modules;

/** @type {import("snowpack").SnowpackUserConfig } */
module.exports = {
  workspaceRoot: '/',
  mount: {
    public: '/',
    src: '/src',
    './node_modules/@eui/theme/dist/fonts': {
      url: '/assets/fonts',
      resolve: false,
      static: true,
    },
    './node_modules/@eui/theme': {
      url: '/libs/shared/@eui/theme',
      static: true,
    },
    './node_modules/@eui/container': {
      url: '/libs/shared/@eui/container',
      static: true,
    },
    './node_modules/@eui/navigation-menu': {
      url: '/libs/@eui/navigation-menu',
      resolve: true,
      static: false,
    },
  },
  plugins: ['@eui/import-css-plugin'],
  packageOptions: {
    rollup: {
      plugins: [nodeResolve()],
    },
    external: [
      ...Object.keys(devDependencies),
      ...externalModules.map(module => module.name),
    ],
    knownEntrypoints: [
      '@open-wc/testing-helpers',
      '@eui/base/actionable-icon',
      '@eui/base/button',
      '@eui/layout/card',
      '@eui/base/checkbox',
      '@eui/layout',
      '@eui/base/modal',
      '@eui/base/spinner',
      '@eui/base/text-field',
      '@eui/base/tooltip',
      '@eui/layout/tile',
      '@eui/base/pill',
    ],
  },
  devOptions: {},
  buildOptions: {
    metaUrlPath: 'libs',
  },
};
