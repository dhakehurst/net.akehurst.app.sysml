#!/bin/bash
# Regenerates SysMLv2_0.agl from the OMG SysML/KerML v2.0 textual BNF, and the Kotlin source holding it.
# usage: regenerate.sh <SysML-textual-bnf.kebnf> <KerML-textual-bnf.kebnf>
# The .kebnf files are in https://github.com/Systems-Modeling/SysML-v2-Release (bnf folder).
set -e
T=$(cd "$(dirname "$0")" && pwd)
G=$(dirname "$T")
python3 -I "$T/convert.py" "$1" "$2" RootNamespace NAME,STRING_VALUE,DECIMAL_VALUE,EXPONENTIAL_VALUE,REGULAR_COMMENT > "$T/rules.tmp" 2> /dev/null
{ cat "$T/header.agl"; echo; cat "$T/rules.tmp"; echo "}"; } > "$G/SysMLv2_0.agl"
rm "$T/rules.tmp"
python3 -I "$T/to_kotlin.py" "$G/SysMLv2_0.agl" "$G/../src/commonMain/kotlin/net/akehurst/app/sysml/core/language/SysmlGrammar.kt"
