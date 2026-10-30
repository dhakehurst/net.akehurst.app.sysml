# SysML v2.0 agl grammar

`SysMLv2_0.agl` is an [agl](https://github.com/dhakehurst/net.akehurst.language) grammar of the SysML v2.0 textual notation.
It should eventually move to the omg repo, next to the generated sysml metamodel.

It is converted from the KerML and SysML v2.0 textual BNF published by the OMG
(`bnf/*.kebnf` in https://github.com/Systems-Modeling/SysML-v2-Release) by `tools/convert.py`:

* assignments (`x =`, `x +=`, `x ?=`), semantic actions `{...}` and `~` markers are removed, `[QualifiedName]` becomes `QualifiedName`;
* only rules reachable from `RootNamespace` are kept, SysML rules override KerML rules of the same name;
* rules that match no text (`EmptyFeature`, ...) are removed together with their uses;
* corrections to the BNF are in `tools/fixes.py`; the main one replaces the single ambiguous
  operator expression rule by the precedence layers of KerML 8.2.5.8.1.

With these corrections all 96 example files of the SysML v2 release (`sysml/src/examples`) parse without errors.

To regenerate the grammar and `SysmlGrammar.kt` (the grammar as a Kotlin string, so it is available on every platform):

    tools/regenerate.sh SysML-textual-bnf.kebnf KerML-textual-bnf.kebnf
