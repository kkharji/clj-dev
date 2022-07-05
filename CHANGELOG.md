# 🎉 [0.1.1](https://github.com/kkharji/clj-dev/tree/0.1.1) - 2021-12-29

## <!-- 0 -->✨ Features



<dl><dd><details><summary>core <b><a href="https://github.com/kkharji/clj-dev/commit/d0c258c087938d82543272ce1d3bede8268983bc">add integrant style alias. halt, go, reset</a></b></summary><br /><sup>This should be easier for people coming using integrant and integrant-repl.</sup></details></dd></dl>




- watch <b><a href="https://github.com/kkharji/clj-dev/commit/fa8935dbe1f7fbba4fb0ed4e289045d2ca0af2cf">point to watch-paths instead of paths</a></b>

## <!-- 1 -->🌱 Enhancements



- core <b><a href="https://github.com/kkharji/clj-dev/commit/e3e8c4f08b54ad3973452c760cb932b27db61de3">run init function only once</a></b>



- util <b><a href="https://github.com/kkharji/clj-dev/commit/a8f1cc35a4f5084bdb6eb62bbf79e2defcb18d35">optional log without timestamp</a></b>

## <!-- 2 -->♻️ Refactor



- global <b><a href="https://github.com/kkharji/clj-dev/commit/46d02594bbbedd7eb5b130018e5c7e6341487e34">isolate and test duct/integrant integration</a></b>
- global <b><a href="https://github.com/kkharji/clj-dev/commit/2a5fcfa8fe58dff17c98f33001731dfa410cba42">rework user notification</a></b>



- state <b><a href="https://github.com/kkharji/clj-dev/commit/da7ddd283de471cc6dc1099ad90a79469a2057f7">process watch-paths once</a></b>

## <!-- 3 -->🐛 Bug Fixes



- global <b><a href="https://github.com/kkharji/clj-dev/commit/5ff11fe0f58c4c417b315cf2cd1a31d1c130983f">initializing with duct and loading local.clj</a></b>

## <!-- 6 -->👷 Miscellaneous



- changelog <b><a href="https://github.com/kkharji/clj-dev/commit/f02b47c6afd8e3f1841aa42223b0054ca5875bc8">update</a></b>
- changelog <b><a href="https://github.com/kkharji/clj-dev/commit/581761d3e46515d93e26b6c0922fc5eb253814ff">update</a></b>



- dev <b><a href="https://github.com/kkharji/clj-dev/commit/9cd6839a0cf589704ffc396cb8e90f492a513f90">add script to build and install locally</a></b>
<dl><dd><details><summary>dev <b><a href="https://github.com/kkharji/clj-dev/commit/c7455feeed86986296ac8beb479c5f010217ee36">auto-changelog generator</a></b></summary><br /><sup>Now, what's missing is a github action to automatically do that 🙈</sup></details></dd></dl>




- release <b><a href="https://github.com/kkharji/clj-dev/commit/113f911557e92c5d560f28eef96ca5d552d5a785">0.1.1</a></b>


# 🎉 [0.1.0](https://github.com/kkharji/clj-dev/tree/0.1.0) - 2021-12-27

## <!-- 4 -->📚 Documentation



- readme <b><a href="https://github.com/kkharji/clj-dev/commit/601d035b3411fd2e22cd6e4de698b3c937e8eaf6">describe purpose and usage</a></b>
- readme <b><a href="https://github.com/kkharji/clj-dev/commit/5aba38d19e282ca0192857b19eba55717f737560">update configuration spec</a></b>



- spec <b><a href="https://github.com/kkharji/clj-dev/commit/7869cd70e0158475715999e47d1587f6667e7a3e">update and add auto-start and load-runtime</a></b>
<dl><dd><details><summary>spec <b><a href="https://github.com/kkharji/clj-dev/commit/24fe286ea5b4c0b7491de23c100c14d60660cf3b">remove :integrant/prep</a></b></summary><br /><sup>The point of ig/set-prep function is to read configuration and not to
produce side effects.

Though, It might be important for custom integrant setup, which is out
of scope right now.</sup></details></dd></dl>



