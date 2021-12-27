# 🔥 [Unreleased](https://github.com/tami5/clj-dev)

## <!-- 0 -->✨ Features



<dl><dd><details><summary>core <b><a href="https://github.com/tami5/clj-dev/commit/d0c258c087938d82543272ce1d3bede8268983bc">add integrant style alias. halt, go, reset</a></b></summary><br /><sup>This should be easier for people coming using integrant and integrant-repl.</sup></details></dd></dl>




- watch <b><a href="https://github.com/tami5/clj-dev/commit/fa8935dbe1f7fbba4fb0ed4e289045d2ca0af2cf">point to watch-paths instead of paths</a></b>

## <!-- 2 -->♻️ Refactor



- state <b><a href="https://github.com/tami5/clj-dev/commit/16e8da7fc6c0efb3ca566c3aa88339a4d6ed1435">process watch-paths once</a></b>

## <!-- 6 -->👷 Miscellaneous



- dev <b><a href="https://github.com/tami5/clj-dev/commit/9cd6839a0cf589704ffc396cb8e90f492a513f90">add script to build and install locally</a></b>
<dl><dd><details><summary>dev <b><a href="https://github.com/tami5/clj-dev/commit/c7455feeed86986296ac8beb479c5f010217ee36">auto-changelog generator</a></b></summary><br /><sup>Now, what's missing is a github action to automatically do that 🙈</sup></details></dd></dl>



# 🎉 [0.1.0](https://github.com/tami5/clj-dev/tree/0.1.0) - 2021-12-27

## <!-- 4 -->📚 Documentation



- readme <b><a href="https://github.com/tami5/clj-dev/commit/601d035b3411fd2e22cd6e4de698b3c937e8eaf6">describe purpose and usage</a></b>
- readme <b><a href="https://github.com/tami5/clj-dev/commit/5aba38d19e282ca0192857b19eba55717f737560">update configuration spec</a></b>



- spec <b><a href="https://github.com/tami5/clj-dev/commit/7869cd70e0158475715999e47d1587f6667e7a3e">update and add auto-start and load-runtime</a></b>
<dl><dd><details><summary>spec <b><a href="https://github.com/tami5/clj-dev/commit/24fe286ea5b4c0b7491de23c100c14d60660cf3b">remove :integrant/prep</a></b></summary><br /><sup>The point of ig/set-prep function is to read configuration and not to
produce side effects.

Though, It might be important for custom integrant setup, which is out
of scope right now.</sup></details></dd></dl>



