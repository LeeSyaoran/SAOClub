import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import globals from "globals";

export default [
  { ignores: ["dist/**", "dist-ssr/**", "node_modules/**", "coverage/**"] },
  js.configs.recommended,
  ...vue.configs["flat/recommended"],
  {
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2021,
        __DEV_BOOT_ID__: "readonly", // hằng số vite.config.js define() lúc build
      },
    },
    rules: {
      "vue/multi-word-component-names": "off",
      "vue/max-attributes-per-line": "off",
      "vue/singleline-html-element-content-newline": "off",
      "vue/html-self-closing": "off",
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      "no-console": "warn",
      "no-empty": ["warn", { allowEmptyCatch: true }],
      "no-useless-escape": "warn",
      "no-dupe-keys": "warn",
      "no-undef": "off",
      "vue/require-default-prop": "off",
      "vue/no-template-shadow": "warn",
      "vue/no-unused-vars": "warn",
    },
  },
];
