# re-find.fx

A JavaFX11 wrapper for re-find.
## Requirements
* JDK11
* tools.deps

## Try it out

```sh
clj -Sdeps "{:deps
                   {MageMasher/re-find.fx
                      {:git/url \"https://github.com/MageMasher/re-find.fx\"
                       :sha \"b29bde3519f7632b63eb71415943bf8a7cfa1462\"}}}" \
    -m re-find.fx

```

## Installation

Add the alias below to your project `deps.edn` or global at `~/.clojure/deps.edn`

```clojure
:re-find.fx
{:extra-deps {MageMasher/re-find.fx
              {:git/url "https://github.com/MageMasher/re-find.fx"
               :sha "b29bde3519f7632b63eb71415943bf8a7cfa1462"}}}

```

## Usage

Run the project directly:

    $ clj -A:re-find.fx -m re-find.fx

Add re-find.fx to an existing project by enabling the `re-find.fx` alias.

    $ clj -A:re-find.fx

Run the project's tests (they'll fail until you edit them):

    $ clj -A:test:runner

## Examples
Same examples as on [re-find.it](https://re-find.it/)
### Bugs

Right now if you provide multiple arguments, only the first argument shows up in
the arguments column of the result table. Problem is identified, need to think 
through the solution.

Example:
#### Args
```
1 2 3
```
#### Ret
```
1
```
#### Results
```
|function         | arguments | return value |
|-----------------+-----------+--------------|
|clojure.core/min | 1         | 1            |
```

### Acknowledgements
Thanks [Michiel Borkent](https://github.com/borkdude/) for creating [re-find](https://github.com/borkdude/re-find) and [re-find.web](https://github.com/borkdude/re-find.web) which is hosted at [re-find.it](https://re-find.it/)
