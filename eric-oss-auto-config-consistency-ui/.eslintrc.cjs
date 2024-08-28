module.exports = {
  extends: ['@open-wc/eslint-config', 'eslint-config-prettier'],
  parser: '@babel/eslint-parser',
  parserOptions: {
    requireConfigFile: false,
  },
  globals: {
    cy: true,
    Cypress: true,
    expect: true,
  },
  rules: {
    // Tries to force static, static methods can't be overridden when extending class
    'class-methods-use-this': 0,
    'import/no-unresolved': 0,
    // reassigning CustomEvent detail is common pattern
    'no-param-reassign': 0,
    'lit-a11y/click-events-have-key-events': 0,
    // as project is small, many files that will export multiple only export one thing
    'import/prefer-default-export': 0,
    'max-classes-per-file': 0,
    'no-restricted-syntax': 0,
    'comma-dangle': [
      'error',
      {
        arrays: 'always-multiline',
        objects: 'always-multiline',
        imports: 'always-multiline',
        exports: 'always-multiline',
        functions: 'always-multiline',
      },
    ],
    // Using custom non-error values as Promise rejection reasons, so rule can be turned off(as per documentation)
    'prefer-promise-reject-errors': 'off',
  },
};
