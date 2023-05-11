## Build instructions

This project was started with Node v14+
You will also need [yarn](https://yarnpkg.com/getting-started/install)

To install packages and run dev server

```
yarn install
yarn dev
```

Build for prod

```
yarn build
```

## Vuex state management

This project uses [Vuex](https://next.vuex.vuejs.org/) with the modules pattern.
Each store module has separate files for state, actions, and mutations.
Current convention is to only call actions from components, (no mutations).

## Vue-router

Project routes make use of [vue-router](https://next.router.vuejs.org/guide/)

## Prettier

Formatting should use the .prettierrc file. For VSCode, install the Prettier extension, go to the IDE Settings and set this formatter to take precedence.

### Use `<script setup>`

[`<script setup>`](https://github.com/vuejs/rfcs/pull/227). To get proper IDE support for the syntax, use [Volar](https://marketplace.visualstudio.com/items?itemName=johnsoncodehk.volar).

## Templates (development purposes only)

Hidden route serving templates: /templates
